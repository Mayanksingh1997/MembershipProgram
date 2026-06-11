package com.firstclub.firstclub.domain.tier;

import com.firstclub.firstclub.configuration.catalog.EligibilityRuleDefinition;
import com.firstclub.firstclub.constants.ComparisonOperator;
import com.firstclub.firstclub.constants.RuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MonthlyOrderValueEvaluator implements TierEligibilityEvaluator {

    @Override
    public RuleType supportedRuleType() {
        return RuleType.MONTHLY_ORDER_VALUE;
    }

    @Override
    public boolean evaluate(EligibilityRuleDefinition rule, TierEvaluationContext context) {
        BigDecimal actual = context.getMonthlyOrderValue() != null
                ? context.getMonthlyOrderValue()
                : BigDecimal.ZERO;
        BigDecimal threshold = rule.getThresholdValue();
        if (threshold == null) {
            return false;
        }
        return compare(actual, threshold, rule.getOperator());
    }

    private boolean compare(BigDecimal actual, BigDecimal threshold, ComparisonOperator operator) {
        int comparison = actual.compareTo(threshold);
        if (operator == ComparisonOperator.GTE) {
            return comparison >= 0;
        }
        if (operator == ComparisonOperator.LTE) {
            return comparison <= 0;
        }
        return comparison == 0;
    }
}
