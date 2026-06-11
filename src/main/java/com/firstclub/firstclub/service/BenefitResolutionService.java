package com.firstclub.firstclub.service;

import com.firstclub.firstclub.configuration.catalog.BenefitDefinition;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProvider;
import com.firstclub.firstclub.configuration.catalog.TierDefinition;
import com.firstclub.firstclub.constants.BenefitCode;
import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.dto.response.ResolvedBenefitsResponse;
import com.firstclub.firstclub.entity.UserMembership;
import com.firstclub.firstclub.repository.UserMembershipRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BenefitResolutionService {

    private final UserMembershipRepository membershipRepository;
    private final MembershipCatalogProvider catalogProvider;

    public BenefitResolutionService(
            UserMembershipRepository membershipRepository,
            MembershipCatalogProvider catalogProvider) {
        this.membershipRepository = membershipRepository;
        this.catalogProvider = catalogProvider;
    }

    public ResponseEntity<ResolvedBenefitsResponse> resolveBenefits(String externalUserId) {
        UserMembership membership = membershipRepository
                .findActiveByExternalUserId(externalUserId, MembershipStatus.ACTIVE)
                .orElse(null);

        if (membership == null) {
            ResolvedBenefitsResponse body = ResolvedBenefitsResponse.builder()
                    .status("SUCCESS")
                    .message("No active membership found")
                    .userId(externalUserId)
                    .activeMembership(false)
                    .freeDelivery(false)
                    .extraDiscountPercent(0)
                    .exclusiveDealsAccess(false)
                    .prioritySupport(false)
                    .build();
            return ResponseEntity.ok(body);
        }

        TierDefinition tier = catalogProvider.getActiveTier(membership.getTierCode());
        boolean freeDelivery = hasBenefit(tier, BenefitCode.FREE_DELIVERY);
        boolean exclusiveDeals = hasBenefit(tier, BenefitCode.EXCLUSIVE_DEALS);
        boolean prioritySupport = hasBenefit(tier, BenefitCode.PRIORITY_SUPPORT);
        int discount = hasBenefit(tier, BenefitCode.EXTRA_DISCOUNT) ? tier.getDiscountPercent() : 0;

        ResolvedBenefitsResponse body = ResolvedBenefitsResponse.builder()
                .status("SUCCESS")
                .message("Benefits resolved successfully")
                .userId(externalUserId)
                .tierCode(membership.getTierCode())
                .activeMembership(true)
                .freeDelivery(freeDelivery)
                .extraDiscountPercent(discount)
                .exclusiveDealsAccess(exclusiveDeals)
                .prioritySupport(prioritySupport)
                .membershipExpiresAt(membership.getEndDate())
                .build();
        return ResponseEntity.ok(body);
    }

    private boolean hasBenefit(TierDefinition tier, BenefitCode code) {
        for (BenefitDefinition benefit : tier.getBenefits()) {
            if (benefit.getCode() == code) {
                return true;
            }
        }
        return false;
    }
}
