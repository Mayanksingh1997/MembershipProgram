package com.firstclub.firstclub.service;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserMembership;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MembershipMapperTest {

    private final MembershipMapper mapper = new MembershipMapper();

    @Test
    void toMembershipResponse_mapsAllFields() {
        UserAccount user = UserAccount.builder()
                .externalUserId("user-001")
                .build();
        UserMembership membership = UserMembership.builder()
                .id(6L)
                .user(user)
                .planCode(PlanCode.MONTHLY)
                .tierCode(TierCode.SILVER)
                .status(MembershipStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 6, 12))
                .endDate(LocalDate.of(2026, 7, 12))
                .autoRenew(true)
                .build();

        var response = mapper.toMembershipResponse(membership, "Membership fetched successfully");

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).isEqualTo("Membership fetched successfully");
        assertThat(response.getMembershipId()).isEqualTo(6L);
        assertThat(response.getUserId()).isEqualTo("user-001");
        assertThat(response.getPlanCode()).isEqualTo(PlanCode.MONTHLY);
        assertThat(response.getTierCode()).isEqualTo(TierCode.SILVER);
        assertThat(response.getMembershipStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(response.getAutoRenew()).isTrue();
        assertThat(response.getPaymentStatus()).isNull();
    }

    @Test
    void toMembershipResponse_includesPaymentStatusWhenProvided() {
        UserAccount user = UserAccount.builder().externalUserId("user-001").build();
        UserMembership membership = UserMembership.builder()
                .id(1L)
                .user(user)
                .planCode(PlanCode.MONTHLY)
                .tierCode(TierCode.SILVER)
                .status(MembershipStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .autoRenew(true)
                .build();

        var response = mapper.toMembershipResponse(
                membership,
                "Subscription created successfully",
                "Payment received successfully");

        assertThat(response.getPaymentStatus()).isEqualTo("Payment received successfully");
    }
}
