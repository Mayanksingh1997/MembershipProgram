package com.firstclub.firstclub.domain.payment;

import com.firstclub.firstclub.constants.PaymentStrategyType;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentStrategy implements PaymentStrategy {

    private static final String SUCCESS_MESSAGE = "Payment received successfully";

    @Override
    public PaymentStrategyType supportedStrategy() {
        return PaymentStrategyType.UPI;
    }

    @Override
    public String processPayment(PaymentContext context) {
        return SUCCESS_MESSAGE;
    }
}
