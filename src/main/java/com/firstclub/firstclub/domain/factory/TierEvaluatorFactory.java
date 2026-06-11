package com.firstclub.firstclub.domain.factory;

import com.firstclub.firstclub.constants.RuleType;
import com.firstclub.firstclub.domain.tier.TierEligibilityEvaluator;
import com.firstclub.firstclub.exception.MembershipException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class TierEvaluatorFactory {

    private final Map<RuleType, TierEligibilityEvaluator> evaluators = new EnumMap<>(RuleType.class);

    @Autowired
    public TierEvaluatorFactory(List<TierEligibilityEvaluator> evaluatorList) {
        for (TierEligibilityEvaluator evaluator : evaluatorList) {
            evaluators.put(evaluator.supportedRuleType(), evaluator);
        }
    }

    public TierEligibilityEvaluator create(RuleType ruleType) {
        TierEligibilityEvaluator evaluator = evaluators.get(ruleType);
        if (evaluator == null) {
            throw new MembershipException(
                    "No evaluator configured for rule type: " + ruleType,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "EVALUATOR_NOT_FOUND");
        }
        return evaluator;
    }
}
