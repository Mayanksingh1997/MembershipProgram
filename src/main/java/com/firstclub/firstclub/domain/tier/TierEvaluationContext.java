package com.firstclub.firstclub.domain.tier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierEvaluationContext {

    private Integer totalOrders;
    private BigDecimal monthlyOrderValue;

    @Builder.Default
    private List<String> cohortCodes = new ArrayList<>();
}
