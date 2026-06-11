package com.firstclub.firstclub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProvider;
import com.firstclub.firstclub.configuration.catalog.PlanDefinition;
import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.constants.PaymentStrategyType;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.domain.factory.MembershipDomainFactory;
import com.firstclub.firstclub.domain.factory.MembershipStateFactory;
import com.firstclub.firstclub.domain.factory.TierEvaluatorFactory;
import com.firstclub.firstclub.domain.observer.MembershipEventPublisher;
import com.firstclub.firstclub.domain.subscription.state.ActiveMembershipState;
import com.firstclub.firstclub.domain.subscription.state.CancelledMembershipState;
import com.firstclub.firstclub.domain.subscription.state.ExpiredMembershipState;
import com.firstclub.firstclub.domain.subscription.state.PendingMembershipState;
import com.firstclub.firstclub.domain.tier.CohortEvaluator;
import com.firstclub.firstclub.domain.tier.MonthlyOrderValueEvaluator;
import com.firstclub.firstclub.domain.tier.OrderCountEvaluator;
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
import com.firstclub.firstclub.service.support.ServiceTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserMembershipRepository membershipRepository;
    @Mock
    private SubscriptionIdempotencyRepository idempotencyRepository;
    @Mock
    private TierEvaluationService tierEvaluationService;
    @Mock
    private MembershipEventPublisher eventPublisher;
    @Mock
    private PaymentService paymentService;

    private MembershipCatalogProvider catalogProvider;
    private MembershipDomainFactory domainFactory;
    private MembershipMapper mapper;
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        catalogProvider = ServiceTestFixtures.catalogProvider();
        TierEvaluatorFactory tierEvaluatorFactory = new TierEvaluatorFactory(List.of(
                new OrderCountEvaluator(),
                new MonthlyOrderValueEvaluator(),
                new CohortEvaluator()));
        MembershipStateFactory stateFactory = new MembershipStateFactory(
                new PendingMembershipState(),
                new ActiveMembershipState(),
                new CancelledMembershipState(),
                new ExpiredMembershipState());
        domainFactory = new MembershipDomainFactory();
        ReflectionTestUtils.setField(domainFactory, "tierEvaluatorFactory", tierEvaluatorFactory);
        ReflectionTestUtils.setField(domainFactory, "membershipStateFactory", stateFactory);
        mapper = new MembershipMapper();
        subscriptionService = new SubscriptionService(
                userAccountRepository,
                catalogProvider,
                membershipRepository,
                idempotencyRepository,
                tierEvaluationService,
                domainFactory,
                eventPublisher,
                mapper,
                new ObjectMapper(),
                paymentService);
    }

    @Test
    void subscribe_createsMembershipSuccessfully() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        PlanDefinition plan = ServiceTestFixtures.monthlyPlan();
        SubscribeRequest request = SubscribeRequest.builder()
                .planCode("MONTHLY")
                .tierCode("SILVER")
                .autoRenew(true)
                .paymentAmount(BigDecimal.valueOf(99))
                .paymentStrategy(PaymentStrategyType.UPI)
                .build();

        when(userAccountRepository.findByExternalUserId("user-001")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE)).thenReturn(Optional.empty());
        doNothing().when(tierEvaluationService).validateTierEligibility(user, TierCode.SILVER);
        when(paymentService.processPayment(
                PaymentStrategyType.UPI,
                BigDecimal.valueOf(99),
                plan,
                "user-001")).thenReturn("Payment received successfully");
        when(membershipRepository.save(any(UserMembership.class))).thenAnswer(invocation -> {
            UserMembership saved = invocation.getArgument(0);
            saved.setId(6L);
            return saved;
        });

        ResponseEntity<MembershipResponse> response = subscriptionService.subscribe("user-001", request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        MembershipResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMembershipId()).isEqualTo(6L);
        assertThat(body.getPlanCode()).isEqualTo(PlanCode.MONTHLY);
        assertThat(body.getTierCode()).isEqualTo(TierCode.SILVER);
        assertThat(body.getPaymentStatus()).isEqualTo("Payment received successfully");
        verify(eventPublisher).publish(any());
    }

    @Test
    void subscribe_returnsIdempotentResponseWhenKeyExists() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership existingMembership = ServiceTestFixtures.membership(user, TierCode.SILVER, PlanCode.MONTHLY);
        SubscriptionIdempotency idempotency = SubscriptionIdempotency.builder()
                .idempotencyKey("key-123")
                .user(user)
                .membership(existingMembership)
                .build();

        when(idempotencyRepository.findByIdempotencyKeyWithMembership("key-123"))
                .thenReturn(Optional.of(idempotency));

        SubscribeRequest request = SubscribeRequest.builder()
                .planCode("MONTHLY")
                .tierCode("SILVER")
                .paymentAmount(BigDecimal.valueOf(99))
                .paymentStrategy(PaymentStrategyType.UPI)
                .build();

        ResponseEntity<MembershipResponse> response = subscriptionService.subscribe("user-001", request, "key-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("idempotent");
        verify(userAccountRepository, org.mockito.Mockito.never()).findByExternalUserId(any());
    }

    @Test
    void subscribe_throwsWhenActiveMembershipExists() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership active = ServiceTestFixtures.membership(user, TierCode.SILVER, PlanCode.MONTHLY);
        SubscribeRequest request = SubscribeRequest.builder()
                .planCode("MONTHLY")
                .tierCode("SILVER")
                .paymentAmount(BigDecimal.valueOf(99))
                .paymentStrategy(PaymentStrategyType.UPI)
                .build();

        when(userAccountRepository.findByExternalUserId("user-001")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserIdAndStatus(1L, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> subscriptionService.subscribe("user-001", request, null))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("already has an active membership");
    }

    @Test
    void getMembership_returnsActiveMembership() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership membership = ServiceTestFixtures.membership(user, TierCode.GOLD, PlanCode.QUARTERLY);
        when(membershipRepository.findActiveByExternalUserId("user-001", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));

        ResponseEntity<MembershipResponse> response = subscriptionService.getMembership("user-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTierCode()).isEqualTo(TierCode.GOLD);
    }

    @Test
    void getMembership_throwsWhenNotFound() {
        when(membershipRepository.findActiveByExternalUserId("user-001", MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getMembership("user-001"))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("No active membership found");
    }

    @Test
    void changeTier_upgradesSuccessfully() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership membership = ServiceTestFixtures.membership(user, TierCode.SILVER, PlanCode.MONTHLY);
        ChangeTierRequest request = ChangeTierRequest.builder()
                .action(ChangeTierRequest.TierAction.UPGRADE)
                .targetTierCode("GOLD")
                .build();

        when(membershipRepository.findActiveByExternalUserId("user-001", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        doNothing().when(tierEvaluationService).validateTierEligibility(user, TierCode.GOLD);
        when(membershipRepository.save(membership)).thenReturn(membership);

        ResponseEntity<MembershipResponse> response = subscriptionService.changeTier("user-001", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTierCode()).isEqualTo(TierCode.GOLD);
        verify(eventPublisher).publish(any());
    }

    @Test
    void changeTier_throwsOnInvalidUpgradeDirection() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership membership = ServiceTestFixtures.membership(user, TierCode.GOLD, PlanCode.MONTHLY);
        ChangeTierRequest request = ChangeTierRequest.builder()
                .action(ChangeTierRequest.TierAction.UPGRADE)
                .targetTierCode("SILVER")
                .build();

        when(membershipRepository.findActiveByExternalUserId("user-001", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> subscriptionService.changeTier("user-001", request))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("Target tier must be higher");
    }

    @Test
    void cancel_cancelsActiveMembership() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership membership = ServiceTestFixtures.membership(user, TierCode.SILVER, PlanCode.MONTHLY);
        when(membershipRepository.findActiveByExternalUserId("user-001", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.save(membership)).thenReturn(membership);

        ResponseEntity<MembershipResponse> response = subscriptionService.cancel(
                "user-001",
                CancelMembershipRequest.builder().immediate(false).build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMembershipStatus()).isEqualTo(MembershipStatus.CANCELLED);
        assertThat(response.getBody().getAutoRenew()).isFalse();
    }

    @Test
    void evaluateTier_returnsUnchangedWhenEligibleTierMatchesCurrent() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership membership = ServiceTestFixtures.membership(user, TierCode.GOLD, PlanCode.MONTHLY);
        when(membershipRepository.findActiveByExternalUserId("user-001", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(tierEvaluationService.buildContext(user)).thenReturn(
                com.firstclub.firstclub.domain.tier.TierEvaluationContext.builder()
                        .totalOrders(15)
                        .monthlyOrderValue(BigDecimal.valueOf(3500))
                        .cohortCodes(List.of())
                        .build());
        when(tierEvaluationService.determineEligibleTier(any()))
                .thenReturn(ServiceTestFixtures.goldTier());

        ResponseEntity<MembershipResponse> response = subscriptionService.evaluateTier("user-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("unchanged");
    }

    @Test
    void expireMemberships_expiresPastDueMemberships() {
        UserAccount user = ServiceTestFixtures.user("user-001");
        UserMembership membership = ServiceTestFixtures.membership(user, TierCode.SILVER, PlanCode.MONTHLY);
        membership.setEndDate(LocalDate.now().minusDays(1));
        when(membershipRepository.findByStatusAndEndDateBefore(eq(MembershipStatus.ACTIVE), eq(LocalDate.now())))
                .thenReturn(List.of(membership));

        int expiredCount = subscriptionService.expireMemberships();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
        assertThat(membership.getAutoRenew()).isFalse();
        verify(membershipRepository).save(membership);
        verify(eventPublisher).publish(any());
    }
}
