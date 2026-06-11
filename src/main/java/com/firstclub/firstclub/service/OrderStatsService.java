package com.firstclub.firstclub.service;

import com.firstclub.firstclub.dto.request.UpdateOrderStatsRequest;
import com.firstclub.firstclub.dto.response.OrderStatsResponse;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserOrderAggregate;
import com.firstclub.firstclub.exception.MembershipException;
import com.firstclub.firstclub.repository.UserAccountRepository;
import com.firstclub.firstclub.repository.UserOrderAggregateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Transactional
public class OrderStatsService {

    private final UserAccountRepository userAccountRepository;
    private final UserOrderAggregateRepository orderAggregateRepository;

    public OrderStatsService(
            UserAccountRepository userAccountRepository,
            UserOrderAggregateRepository orderAggregateRepository) {
        this.userAccountRepository = userAccountRepository;
        this.orderAggregateRepository = orderAggregateRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<OrderStatsResponse> getOrderStats(String externalUserId) {
        UserAccount user = userAccountRepository.findByExternalUserId(externalUserId)
                .orElseThrow(() -> new MembershipException(
                        "User not found: " + externalUserId,
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"));

        UserOrderAggregate aggregate = orderAggregateRepository.findByUserId(user.getId()).orElse(null);
        int totalOrders = 0;
        BigDecimal monthlyOrderValue = BigDecimal.ZERO;
        Instant lastOrderAt = null;
        if (aggregate != null) {
            totalOrders = aggregate.getTotalOrders();
            monthlyOrderValue = aggregate.getMonthlyOrderValue();
            lastOrderAt = aggregate.getLastOrderAt();
        }

        OrderStatsResponse body = OrderStatsResponse.builder()
                .status("SUCCESS")
                .message("Order stats fetched successfully")
                .userId(externalUserId)
                .totalOrders(totalOrders)
                .monthlyOrderValue(monthlyOrderValue)
                .lastOrderAt(lastOrderAt)
                .build();
        return ResponseEntity.ok(body);
    }

    public ResponseEntity<Void> updateOrderStats(String externalUserId, UpdateOrderStatsRequest request) {
        UserAccount user = userAccountRepository.findByExternalUserId(externalUserId)
                .orElseThrow(() -> new MembershipException(
                        "User not found: " + externalUserId,
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"));

        UserOrderAggregate aggregate = orderAggregateRepository.findByUserId(user.getId()).orElse(null);
        if (aggregate == null) {
            aggregate = UserOrderAggregate.builder()
                    .user(user)
                    .totalOrders(0)
                    .monthlyOrderValue(java.math.BigDecimal.ZERO)
                    .build();
        }

        aggregate.setTotalOrders(request.getTotalOrders());
        aggregate.setMonthlyOrderValue(request.getMonthlyOrderValue());
        aggregate.setLastOrderAt(Instant.now());
        orderAggregateRepository.save(aggregate);
        return ResponseEntity.noContent().build();
    }
}
