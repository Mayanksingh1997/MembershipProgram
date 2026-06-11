package com.firstclub.firstclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderStatsResponse extends BaseResponse {

    private String userId;
    private Integer totalOrders;
    private BigDecimal monthlyOrderValue;
    private Instant lastOrderAt;
}
