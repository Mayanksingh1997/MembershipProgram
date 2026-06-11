package com.firstclub.firstclub.domain.factory;

import com.firstclub.firstclub.constants.PaymentStrategyType;
import com.firstclub.firstclub.domain.payment.CardPaymentStrategy;
import com.firstclub.firstclub.domain.payment.PaymentStrategy;
import com.firstclub.firstclub.domain.payment.UpiPaymentStrategy;
import com.firstclub.firstclub.exception.MembershipException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStrategyFactoryTest {

    private PaymentStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PaymentStrategyFactory(List.of(
                new UpiPaymentStrategy(),
                new CardPaymentStrategy()));
    }

    @Test
    void create_returnsUpiStrategy() {
        PaymentStrategy strategy = factory.create(PaymentStrategyType.UPI);
        assertThat(strategy.supportedStrategy()).isEqualTo(PaymentStrategyType.UPI);
    }

    @Test
    void create_returnsCardStrategy() {
        PaymentStrategy strategy = factory.create(PaymentStrategyType.CARD);
        assertThat(strategy.supportedStrategy()).isEqualTo(PaymentStrategyType.CARD);
    }

    @Test
    void create_throwsWhenStrategyMissing() {
        PaymentStrategyFactory emptyFactory = new PaymentStrategyFactory(List.of());

        assertThatThrownBy(() -> emptyFactory.create(PaymentStrategyType.UPI))
                .isInstanceOf(MembershipException.class)
                .hasMessageContaining("No payment strategy configured");
    }
}
