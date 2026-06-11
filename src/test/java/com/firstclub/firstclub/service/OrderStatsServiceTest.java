package com.firstclub.firstclub.service;

import com.firstclub.firstclub.dto.request.UpdateOrderStatsRequest;
import com.firstclub.firstclub.dto.response.OrderStatsResponse;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserOrderAggregate;
import com.firstclub.firstclub.exception.MembershipException;
import com.firstclub.firstclub.repository.UserAccountRepository;
import com.firstclub.firstclub.repository.UserOrderAggregateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserOrderAggregateRepository orderAggregateRepository;

    private OrderStatsService orderStatsService;

    @BeforeEach
    void setUp() {
        orderStatsService = new OrderStatsService(userAccountRepository, orderAggregateRepository);
    }

    @Test
    void getOrderStats_returnsStoredAggregate() {
        UserAccount user = UserAccount.builder()
                .id(1L)
                .externalUserId("user-002")
                .build();
        Instant lastOrderAt = Instant.parse("2026-06-10T14:30:00Z");
        UserOrderAggregate aggregate = UserOrderAggregate.builder()
                .user(user)
                .totalOrders(15)
                .monthlyOrderValue(BigDecimal.valueOf(3500))
                .lastOrderAt(lastOrderAt)
                .build();

        when(userAccountRepository.findByExternalUserId("user-002")).thenReturn(Optional.of(user));
        when(orderAggregateRepository.findByUserId(1L)).thenReturn(Optional.of(aggregate));

        ResponseEntity<OrderStatsResponse> response = orderStatsService.getOrderStats("user-002");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OrderStatsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getUserId()).isEqualTo("user-002");
        assertThat(body.getTotalOrders()).isEqualTo(15);
        assertThat(body.getMonthlyOrderValue()).isEqualByComparingTo("3500.00");
        assertThat(body.getLastOrderAt()).isEqualTo(lastOrderAt);
    }

    @Test
    void getOrderStats_returnsDefaultsWhenAggregateMissing() {
        UserAccount user = UserAccount.builder()
                .id(2L)
                .externalUserId("user-001")
                .build();

        when(userAccountRepository.findByExternalUserId("user-001")).thenReturn(Optional.of(user));
        when(orderAggregateRepository.findByUserId(2L)).thenReturn(Optional.empty());

        ResponseEntity<OrderStatsResponse> response = orderStatsService.getOrderStats("user-001");

        OrderStatsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTotalOrders()).isZero();
        assertThat(body.getMonthlyOrderValue()).isEqualByComparingTo("0.00");
        assertThat(body.getLastOrderAt()).isNull();
    }

    @Test
    void getOrderStats_throwsWhenUserNotFound() {
        when(userAccountRepository.findByExternalUserId("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderStatsService.getOrderStats("missing-user"))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateOrderStats_createsAggregateWhenMissing() {
        UserAccount user = UserAccount.builder()
                .id(3L)
                .externalUserId("user-003")
                .build();
        UpdateOrderStatsRequest request = UpdateOrderStatsRequest.builder()
                .totalOrders(20)
                .monthlyOrderValue(BigDecimal.valueOf(6000))
                .build();

        when(userAccountRepository.findByExternalUserId("user-003")).thenReturn(Optional.of(user));
        when(orderAggregateRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(orderAggregateRepository.save(any(UserOrderAggregate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Void> response = orderStatsService.updateOrderStats("user-003", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(orderAggregateRepository).save(any(UserOrderAggregate.class));
    }

    @Test
    void updateOrderStats_updatesExistingAggregate() {
        UserAccount user = UserAccount.builder()
                .id(1L)
                .externalUserId("user-002")
                .build();
        UserOrderAggregate aggregate = UserOrderAggregate.builder()
                .user(user)
                .totalOrders(5)
                .monthlyOrderValue(BigDecimal.valueOf(1200))
                .build();
        UpdateOrderStatsRequest request = UpdateOrderStatsRequest.builder()
                .totalOrders(15)
                .monthlyOrderValue(BigDecimal.valueOf(3500))
                .build();

        when(userAccountRepository.findByExternalUserId("user-002")).thenReturn(Optional.of(user));
        when(orderAggregateRepository.findByUserId(1L)).thenReturn(Optional.of(aggregate));
        when(orderAggregateRepository.save(aggregate)).thenReturn(aggregate);

        ResponseEntity<Void> response = orderStatsService.updateOrderStats("user-002", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(aggregate.getTotalOrders()).isEqualTo(15);
        assertThat(aggregate.getMonthlyOrderValue()).isEqualByComparingTo("3500");
        assertThat(aggregate.getLastOrderAt()).isNotNull();
    }
}
