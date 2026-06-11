package com.firstclub.firstclub.domain.payment;

import com.firstclub.firstclub.constants.PaymentStrategyType;

public interface PaymentStrategy {

    PaymentStrategyType supportedStrategy();

    String processPayment(PaymentContext context);
}
