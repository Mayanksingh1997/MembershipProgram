package com.firstclub.firstclub.dto.request;

import jakarta.validation.constraints.Min;
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
public class UpdateOrderStatsRequest {

    @NotNull
    @Min(0)
    private Integer totalOrders;

    @NotNull
    @Min(0)
    private BigDecimal monthlyOrderValue;
}
