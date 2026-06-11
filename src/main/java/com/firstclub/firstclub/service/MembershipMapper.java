package com.firstclub.firstclub.service;

import com.firstclub.firstclub.dto.response.MembershipResponse;
import com.firstclub.firstclub.entity.UserMembership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class MembershipMapper {

    public MembershipResponse toMembershipResponse(UserMembership membership, String message) {
        return toMembershipResponse(membership, message, null);
    }

    public MembershipResponse toMembershipResponse(
            UserMembership membership,
            String message,
            String paymentStatus) {
        long daysRemaining = Math.max(
                0,
                ChronoUnit.DAYS.between(LocalDate.now(), membership.getEndDate()));
        return MembershipResponse.builder()
                .status("SUCCESS")
                .message(message)
                .membershipId(membership.getId())
                .userId(membership.getUser().getExternalUserId())
                .planCode(membership.getPlanCode())
                .tierCode(membership.getTierCode())
                .membershipStatus(membership.getStatus())
                .startDate(membership.getStartDate())
                .endDate(membership.getEndDate())
                .daysRemaining(daysRemaining)
                .autoRenew(membership.getAutoRenew())
                .paymentStatus(paymentStatus)
                .build();
    }
}
