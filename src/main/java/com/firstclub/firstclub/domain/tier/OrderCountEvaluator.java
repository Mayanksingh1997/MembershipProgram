package com.firstclub.firstclub.domain.tier;

import com.firstclub.firstclub.configuration.catalog.EligibilityRuleDefinition;
import com.firstclub.firstclub.constants.ComparisonOperator;
import com.firstclub.firstclub.constants.RuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderCountEvaluator implements TierEligibilityEvaluator {

    @Override
    public RuleType supportedRuleType() {
        return RuleType.ORDER_COUNT;
    }

    @Override
    public boolean evaluate(EligibilityRuleDefinition rule, TierEvaluationContext context) {
        int actual = context.getTotalOrders() != null ? context.getTotalOrders() : 0;
        BigDecimal threshold = rule.getThresholdValue();
        if (threshold == null) {
            return false;
        }
        return compare(actual, threshold.intValue(), rule.getOperator());
    }

    private boolean compare(int actual, int threshold, ComparisonOperator operator) {
        if (operator == ComparisonOperator.GTE) {
            return actual >= threshold;
        }
        if (operator == ComparisonOperator.LTE) {
            return actual <= threshold;
        }
        return actual == threshold;
    }
}
