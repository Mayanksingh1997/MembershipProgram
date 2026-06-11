package com.firstclub.firstclub.domain.tier;

import com.firstclub.firstclub.configuration.catalog.EligibilityRuleDefinition;
import com.firstclub.firstclub.constants.ComparisonOperator;
import com.firstclub.firstclub.constants.RuleType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CohortEvaluator implements TierEligibilityEvaluator {

    @Override
    public RuleType supportedRuleType() {
        return RuleType.COHORT;
    }

    @Override
    public boolean evaluate(EligibilityRuleDefinition rule, TierEvaluationContext context) {
        List<String> cohorts = context.getCohortCodes();
        if (rule.getCohortCode() == null || cohorts == null) {
            return false;
        }
        boolean member = false;
        for (String cohortCode : cohorts) {
            if (rule.getCohortCode().equals(cohortCode)) {
                member = true;
                break;
            }
        }
        if (rule.getOperator() == ComparisonOperator.EQ) {
            return member;
        }
        return !member;
    }
}
