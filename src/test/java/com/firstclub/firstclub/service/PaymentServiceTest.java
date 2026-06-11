package com.firstclub.firstclub.service;

import com.firstclub.firstclub.configuration.catalog.PlanDefinition;
import com.firstclub.firstclub.constants.PaymentStrategyType;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.domain.factory.PaymentStrategyFactory;
import com.firstclub.firstclub.domain.payment.CardPaymentStrategy;
import com.firstclub.firstclub.domain.payment.PaymentStrategy;
import com.firstclub.firstclub.domain.payment.UpiPaymentStrategy;
import com.firstclub.firstclub.exception.MembershipException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentStrategyFactory factory = new PaymentStrategyFactory(List.of(
                new UpiPaymentStrategy(),
                new CardPaymentStrategy()));
        paymentService = new PaymentService(factory);
    }

    @Test
    void processPayment_upiStrategy_returnsSuccessMessage() {
        PlanDefinition plan = monthlyPlan();

        String status = paymentService.processPayment(
                PaymentStrategyType.UPI,
                BigDecimal.valueOf(99),
                plan,
                "user-001");

        assertThat(status).isEqualTo("Payment received successfully");
    }

    @Test
    void processPayment_cardStrategy_returnsSuccessMessage() {
        PlanDefinition plan = monthlyPlan();

        String status = paymentService.processPayment(
                PaymentStrategyType.CARD,
                BigDecimal.valueOf(99),
                plan,
                "user-001");

        assertThat(status).isEqualTo("Payment received successfully");
    }

    @Test
    void processPayment_throwsWhenAmountDoesNotMatchPlanPrice() {
        PlanDefinition plan = monthlyPlan();

        assertThatThrownBy(() -> paymentService.processPayment(
                PaymentStrategyType.UPI,
                BigDecimal.valueOf(50),
                plan,
                "user-001"))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("Payment amount must match plan price");
    }

    private PlanDefinition monthlyPlan() {
        PlanDefinition plan = new PlanDefinition();
        plan.setCode(PlanCode.MONTHLY);
        plan.setPrice(BigDecimal.valueOf(99));
        plan.setDurationDays(30);
        plan.setActive(true);
        return plan;
    }
}
