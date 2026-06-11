package com.firstclub.firstclub.domain.factory;

import com.firstclub.firstclub.constants.PaymentStrategyType;
import com.firstclub.firstclub.domain.payment.PaymentStrategy;
import com.firstclub.firstclub.exception.MembershipException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentStrategyType, PaymentStrategy> strategies = new EnumMap<>(PaymentStrategyType.class);

    @Autowired
    public PaymentStrategyFactory(List<PaymentStrategy> strategyList) {
        for (PaymentStrategy strategy : strategyList) {
            strategies.put(strategy.supportedStrategy(), strategy);
        }
    }

    public PaymentStrategy create(PaymentStrategyType strategyType) {
        PaymentStrategy strategy = strategies.get(strategyType);
        if (strategy == null) {
            throw new MembershipException(
                    "No payment strategy configured for: " + strategyType,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PAYMENT_STRATEGY_NOT_FOUND");
        }
        return strategy;
    }
}
