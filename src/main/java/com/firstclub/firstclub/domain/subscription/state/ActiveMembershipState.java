package com.firstclub.firstclub.domain.subscription.state;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.entity.UserMembership;
import org.springframework.stereotype.Component;

@Component
public class ActiveMembershipState extends AbstractMembershipState {

    @Override
    public MembershipStatus status() {
        return MembershipStatus.ACTIVE;
    }

    @Override
    public void cancel(UserMembership membership, boolean immediate) {
        membership.setStatus(MembershipStatus.CANCELLED);
        if (immediate) {
            membership.setAutoRenew(false);
        }
    }

    @Override
    public void changePlan(UserMembership membership) {
        // Plan change is applied by the service; state allows the transition.
    }

    @Override
    public void changeTier(UserMembership membership) {
        // Tier change is applied by the service; state allows the transition.
    }

    @Override
    public void expire(UserMembership membership) {
        membership.setStatus(MembershipStatus.EXPIRED);
        membership.setAutoRenew(false);
    }
}
