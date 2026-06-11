package com.firstclub.firstclub.domain.payment;

import com.firstclub.firstclub.constants.PaymentStrategyType;
import org.springframework.stereotype.Component;

@Component
public class CardPaymentStrategy implements PaymentStrategy {

    private static final String SUCCESS_MESSAGE = "Payment received successfully";

    @Override
    public PaymentStrategyType supportedStrategy() {
        return PaymentStrategyType.CARD;
    }

    @Override
    public String processPayment(PaymentContext context) {
        return SUCCESS_MESSAGE;
    }
}
