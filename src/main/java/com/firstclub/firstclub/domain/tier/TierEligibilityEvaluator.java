package com.firstclub.firstclub.domain.tier;

import com.firstclub.firstclub.configuration.catalog.EligibilityRuleDefinition;
import com.firstclub.firstclub.constants.RuleType;

public interface TierEligibilityEvaluator {

    RuleType supportedRuleType();

    boolean evaluate(EligibilityRuleDefinition rule, TierEvaluationContext context);
}
