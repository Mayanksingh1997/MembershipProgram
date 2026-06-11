package com.firstclub.firstclub.dto.response;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.constants.TierCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MembershipResponse extends BaseResponse {

    private Long membershipId;
    private String userId;
    private PlanCode planCode;
    private TierCode tierCode;
    private MembershipStatus membershipStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long daysRemaining;
    private Boolean autoRenew;
    private String paymentStatus;
}
