package com.firstclub.firstclub.domain.payment;

import com.firstclub.firstclub.configuration.catalog.PlanDefinition;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentContext {

    private String userId;
    private BigDecimal amount;
    private PlanDefinition plan;
}
