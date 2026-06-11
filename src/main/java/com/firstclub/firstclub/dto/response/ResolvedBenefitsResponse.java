package com.firstclub.firstclub.dto.response;

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
public class ResolvedBenefitsResponse extends BaseResponse {

    private String userId;
    private TierCode tierCode;
    private boolean activeMembership;
    private boolean freeDelivery;
    private Integer extraDiscountPercent;
    private boolean exclusiveDealsAccess;
    private boolean prioritySupport;
    private LocalDate membershipExpiresAt;
}
