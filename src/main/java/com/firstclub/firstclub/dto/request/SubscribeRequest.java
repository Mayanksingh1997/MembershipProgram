package com.firstclub.firstclub.dto.request;

import com.firstclub.firstclub.constants.PaymentStrategyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest {

    @NotBlank
    private String planCode;

    @NotBlank
    private String tierCode;

    @Builder.Default
    private Boolean autoRenew = true;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal paymentAmount;

    @NotNull
    private PaymentStrategyType paymentStrategy;
}
