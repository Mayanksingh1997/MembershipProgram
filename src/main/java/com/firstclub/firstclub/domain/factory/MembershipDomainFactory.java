package com.firstclub.firstclub.domain.factory;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.constants.RuleType;
import com.firstclub.firstclub.domain.subscription.state.MembershipState;
import com.firstclub.firstclub.domain.tier.TierEligibilityEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Abstract Factory: creates related domain objects (evaluators and state handlers)
 * without exposing concrete implementations to callers.
 */
@Component
public class MembershipDomainFactory {

    @Autowired
    private TierEvaluatorFactory tierEvaluatorFactory;

    @Autowired
    private MembershipStateFactory membershipStateFactory;

    public TierEligibilityEvaluator createTierEvaluator(RuleType ruleType) {
        return tierEvaluatorFactory.create(ruleType);
    }

    public MembershipState createMembershipState(MembershipStatus status) {
        return membershipStateFactory.create(status);
    }
}
