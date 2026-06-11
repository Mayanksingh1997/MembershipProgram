package com.firstclub.firstclub.service;

import com.firstclub.firstclub.configuration.catalog.PlanDefinition;
import com.firstclub.firstclub.constants.PaymentStrategyType;
import com.firstclub.firstclub.domain.factory.PaymentStrategyFactory;
import com.firstclub.firstclub.domain.payment.PaymentContext;
import com.firstclub.firstclub.domain.payment.PaymentStrategy;
import com.firstclub.firstclub.exception.MembershipException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private final PaymentStrategyFactory paymentStrategyFactory;

    public PaymentService(PaymentStrategyFactory paymentStrategyFactory) {
        this.paymentStrategyFactory = paymentStrategyFactory;
    }

    public String processPayment(
            PaymentStrategyType strategyType,
            BigDecimal paymentAmount,
            PlanDefinition plan,
            String externalUserId) {
        validatePaymentAmount(paymentAmount, plan);

        PaymentStrategy strategy = paymentStrategyFactory.create(strategyType);
        PaymentContext context = PaymentContext.builder()
                .userId(externalUserId)
                .amount(paymentAmount)
                .plan(plan)
                .build();
        return strategy.processPayment(context);
    }

    private void validatePaymentAmount(BigDecimal paymentAmount, PlanDefinition plan) {
        if (paymentAmount == null || plan.getPrice() == null) {
            throw new MembershipException(
                    "Payment amount is required",
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR");
        }
        if (paymentAmount.compareTo(plan.getPrice()) != 0) {
            throw new MembershipException(
                    "Payment amount must match plan price: " + plan.getPrice(),
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PAYMENT_AMOUNT_MISMATCH");
        }
    }
}
