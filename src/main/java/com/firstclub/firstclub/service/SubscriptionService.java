package com.firstclub.firstclub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProvider;
import com.firstclub.firstclub.configuration.catalog.PlanDefinition;
import com.firstclub.firstclub.configuration.catalog.TierDefinition;
import com.firstclub.firstclub.constants.MembershipEventType;
import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.domain.factory.MembershipDomainFactory;
import com.firstclub.firstclub.domain.observer.MembershipEvent;
import com.firstclub.firstclub.domain.observer.MembershipEventPublisher;
import com.firstclub.firstclub.domain.subscription.state.MembershipState;
import com.firstclub.firstclub.dto.request.CancelMembershipRequest;
import com.firstclub.firstclub.dto.request.ChangeTierRequest;
import com.firstclub.firstclub.dto.request.SubscribeRequest;
import com.firstclub.firstclub.dto.response.MembershipResponse;
import com.firstclub.firstclub.entity.SubscriptionIdempotency;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserMembership;
import com.firstclub.firstclub.exception.MembershipException;
import com.firstclub.firstclub.repository.SubscriptionIdempotencyRepository;
import com.firstclub.firstclub.repository.UserAccountRepository;
import com.firstclub.firstclub.repository.UserMembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class SubscriptionService {

    private final UserAccountRepository userAccountRepository;
    private final MembershipCatalogProvider catalogProvider;
    private final UserMembershipRepository membershipRepository;
    private final SubscriptionIdempotencyRepository idempotencyRepository;
    private final TierEvaluationService tierEvaluationService;
    private final MembershipDomainFactory domainFactory;
    private final MembershipEventPublisher eventPublisher;
    private final MembershipMapper mapper;
    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public SubscriptionService(
            UserAccountRepository userAccountRepository,
            MembershipCatalogProvider catalogProvider,
            UserMembershipRepository membershipRepository,
            SubscriptionIdempotencyRepository idempotencyRepository,
            TierEvaluationService tierEvaluationService,
            MembershipDomainFactory domainFactory,
            MembershipEventPublisher eventPublisher,
            MembershipMapper mapper,
            ObjectMapper objectMapper,
            PaymentService paymentService) {
        this.userAccountRepository = userAccountRepository;
        this.catalogProvider = catalogProvider;
        this.membershipRepository = membershipRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.tierEvaluationService = tierEvaluationService;
        this.domainFactory = domainFactory;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    public ResponseEntity<MembershipResponse> subscribe(String externalUserId, SubscribeRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<SubscriptionIdempotency> existing = idempotencyRepository
                    .findByIdempotencyKeyWithMembership(idempotencyKey);
            if (existing.isPresent()) {
                MembershipResponse body = mapper.toMembershipResponse(
                        existing.get().getMembership(),
                        "Subscription already processed (idempotent)");
                return ResponseEntity.ok(body);
            }
        }

        UserAccount user = getUser(externalUserId);
        Optional<UserMembership> activeMembership = membershipRepository.findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE);
        if (activeMembership.isPresent()) {
            throw new MembershipException(
                    "User already has an active membership",
                    HttpStatus.CONFLICT,
                    "ACTIVE_MEMBERSHIP_EXISTS");
        }

        PlanCode planCode = catalogProvider.resolvePlanCode(request.getPlanCode());
        TierCode tierCode = catalogProvider.resolveTierCode(request.getTierCode());
        PlanDefinition plan = catalogProvider.getActivePlan(planCode);
        TierDefinition tier = catalogProvider.getActiveTier(tierCode);
        tierEvaluationService.validateTierEligibility(user, tierCode);

        String paymentStatus = paymentService.processPayment(
                request.getPaymentStrategy(),
                request.getPaymentAmount(),
                plan,
                externalUserId);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(plan.getDurationDays());

        UserMembership membership = UserMembership.builder()
                .user(user)
                .planCode(plan.getCode())
                .tierCode(tier.getCode())
                .status(MembershipStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(request.getAutoRenew())
                .build();

        membership = membershipRepository.save(membership);
        publishEvent(membership, MembershipEventType.SUBSCRIBED, null, snapshot(membership), externalUserId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyRepository.save(SubscriptionIdempotency.builder()
                    .idempotencyKey(idempotencyKey)
                    .user(user)
                    .membership(membership)
                    .build());
        }

        MembershipResponse body = mapper.toMembershipResponse(
                membership,
                "Subscription created successfully",
                paymentStatus);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<MembershipResponse> getMembership(String externalUserId) {
        UserMembership membership = membershipRepository
                .findActiveByExternalUserId(externalUserId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new MembershipException(
                        "No active membership found for user",
                        HttpStatus.NOT_FOUND,
                        "MEMBERSHIP_NOT_FOUND"));
        MembershipResponse body = mapper.toMembershipResponse(membership, "Membership fetched successfully");
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<MembershipResponse> changeTier(String externalUserId, ChangeTierRequest request) {
        UserMembership membership = getActiveMembership(externalUserId);
        TierDefinition currentTier = catalogProvider.getActiveTier(membership.getTierCode());
        TierCode targetTierCode = catalogProvider.resolveTierCode(request.getTargetTierCode());
        TierDefinition targetTier = catalogProvider.getActiveTier(targetTierCode);

        validateTierChangeDirection(request, currentTier, targetTier);
        tierEvaluationService.validateTierEligibility(membership.getUser(), targetTierCode);

        String oldSnapshot = snapshot(membership);
        MembershipState state = domainFactory.createMembershipState(membership.getStatus());
        state.changeTier(membership);
        membership.setTierCode(targetTier.getCode());
        membership = membershipRepository.save(membership);

        MembershipEventType eventType = request.getAction() == ChangeTierRequest.TierAction.UPGRADE
                ? MembershipEventType.TIER_UPGRADED
                : MembershipEventType.TIER_DOWNGRADED;
        publishEvent(membership, eventType, oldSnapshot, snapshot(membership), externalUserId);

        MembershipResponse body = mapper.toMembershipResponse(membership, "Membership tier updated successfully");
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<MembershipResponse> cancel(String externalUserId, CancelMembershipRequest request) {
        UserMembership membership = getActiveMembership(externalUserId);
        String oldSnapshot = snapshot(membership);

        MembershipState state = domainFactory.createMembershipState(membership.getStatus());
        state.cancel(membership, Boolean.TRUE.equals(request.getImmediate()));
        membership.setAutoRenew(false);
        membership = membershipRepository.save(membership);

        publishEvent(membership, MembershipEventType.CANCELLED, oldSnapshot, snapshot(membership), externalUserId);
        MembershipResponse body = mapper.toMembershipResponse(membership, "Membership cancelled successfully");
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<MembershipResponse> evaluateTier(String externalUserId) {
        UserMembership membership = getActiveMembership(externalUserId);
        TierDefinition eligibleTier = tierEvaluationService.determineEligibleTier(
                tierEvaluationService.buildContext(membership.getUser()));

        if (eligibleTier.getCode() == membership.getTierCode()) {
            MembershipResponse body = mapper.toMembershipResponse(membership, "Tier unchanged after evaluation");
            return ResponseEntity.ok(body);
        }

        int previousRank = catalogProvider.getActiveTier(membership.getTierCode()).getRank();
        String oldSnapshot = snapshot(membership);
        MembershipState state = domainFactory.createMembershipState(membership.getStatus());
        state.changeTier(membership);
        membership.setTierCode(eligibleTier.getCode());
        membership = membershipRepository.save(membership);

        MembershipEventType eventType = eligibleTier.getRank() > previousRank
                ? MembershipEventType.TIER_UPGRADED
                : MembershipEventType.TIER_DOWNGRADED;
        publishEvent(membership, MembershipEventType.TIER_EVALUATED, oldSnapshot, snapshot(membership), externalUserId);
        publishEvent(membership, eventType, oldSnapshot, snapshot(membership), externalUserId);

        MembershipResponse body = mapper.toMembershipResponse(membership, "Tier evaluated and updated successfully");
        return ResponseEntity.ok(body);
    }

    public int expireMemberships() {
        List<UserMembership> expiredMemberships = membershipRepository.findByStatusAndEndDateBefore(
                MembershipStatus.ACTIVE, LocalDate.now());

        for (UserMembership membership : expiredMemberships) {
            String oldSnapshot = snapshot(membership);
            MembershipState state = domainFactory.createMembershipState(membership.getStatus());
            state.expire(membership);
            membershipRepository.save(membership);
            publishEvent(
                    membership,
                    MembershipEventType.EXPIRED,
                    oldSnapshot,
                    snapshot(membership),
                    "SYSTEM");
        }
        return expiredMemberships.size();
    }

    private void validateTierChangeDirection(
            ChangeTierRequest request,
            TierDefinition currentTier,
            TierDefinition targetTier) {
        if (request.getAction() == ChangeTierRequest.TierAction.UPGRADE
                && targetTier.getRank() <= currentTier.getRank()) {
            throw new MembershipException(
                    "Target tier must be higher than current tier for upgrade",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_TIER_UPGRADE");
        }
        if (request.getAction() == ChangeTierRequest.TierAction.DOWNGRADE
                && targetTier.getRank() >= currentTier.getRank()) {
            throw new MembershipException(
                    "Target tier must be lower than current tier for downgrade",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_TIER_DOWNGRADE");
        }
    }

    private UserMembership getActiveMembership(String externalUserId) {
        return membershipRepository
                .findActiveByExternalUserId(externalUserId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new MembershipException(
                        "No active membership found for user",
                        HttpStatus.NOT_FOUND,
                        "MEMBERSHIP_NOT_FOUND"));
    }

    private UserAccount getUser(String externalUserId) {
        return userAccountRepository.findByExternalUserId(externalUserId)
                .orElseThrow(() -> new MembershipException(
                        "User not found: " + externalUserId,
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"));
    }

    private void publishEvent(
            UserMembership membership,
            MembershipEventType eventType,
            String oldValue,
            String newValue,
            String triggeredBy) {
        eventPublisher.publish(MembershipEvent.builder()
                .membership(membership)
                .eventType(eventType)
                .oldValueJson(oldValue)
                .newValueJson(newValue)
                .triggeredBy(triggeredBy)
                .build());
    }

    private String snapshot(UserMembership membership) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("planCode", membership.getPlanCode());
        snapshot.put("tierCode", membership.getTierCode());
        snapshot.put("status", membership.getStatus());
        snapshot.put("startDate", membership.getStartDate());
        snapshot.put("endDate", membership.getEndDate());
        snapshot.put("autoRenew", membership.getAutoRenew());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            return snapshot.toString();
        }
    }
}
