package com.firstclub.firstclub.service;

import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.dto.response.ResolvedBenefitsResponse;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserMembership;
import com.firstclub.firstclub.service.support.ServiceTestFixtures;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProvider;
import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.repository.UserMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitResolutionServiceTest {

    @Mock
    private UserMembershipRepository membershipRepository;

    private MembershipCatalogProvider catalogProvider;
    private BenefitResolutionService benefitResolutionService;

    @BeforeEach
    void setUp() {
        catalogProvider = ServiceTestFixtures.catalogProvider();
        benefitResolutionService = new BenefitResolutionService(membershipRepository, catalogProvider);
    }

    @Test
    void resolveBenefits_returnsDefaultsWhenNoActiveMembership() {
        when(membershipRepository.findActiveByExternalUserId("user-001", MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ResponseEntity<ResolvedBenefitsResponse> response = benefitResolutionService.resolveBenefits("user-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResolvedBenefitsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isActiveMembership()).isFalse();
        assertThat(body.isFreeDelivery()).isFalse();
        assertThat(body.getExtraDiscountPercent()).isZero();
        assertThat(body.isExclusiveDealsAccess()).isFalse();
        assertThat(body.isPrioritySupport()).isFalse();
    }

    @Test
    void resolveBenefits_returnsGoldBenefitsForActiveMembership() {
        UserAccount user = ServiceTestFixtures.user("user-002");
        UserMembership membership = ServiceTestFixtures.membership(user, TierCode.GOLD, PlanCode.MONTHLY);
        when(membershipRepository.findActiveByExternalUserId("user-002", MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));

        ResponseEntity<ResolvedBenefitsResponse> response = benefitResolutionService.resolveBenefits("user-002");

        ResolvedBenefitsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isActiveMembership()).isTrue();
        assertThat(body.getTierCode()).isEqualTo(TierCode.GOLD);
        assertThat(body.isFreeDelivery()).isTrue();
        assertThat(body.getExtraDiscountPercent()).isEqualTo(10);
        assertThat(body.isExclusiveDealsAccess()).isTrue();
        assertThat(body.isPrioritySupport()).isFalse();
    }
}
