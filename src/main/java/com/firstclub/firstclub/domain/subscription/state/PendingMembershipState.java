package com.firstclub.firstclub.domain.subscription.state;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.entity.UserMembership;
import org.springframework.stereotype.Component;

@Component
public class PendingMembershipState extends AbstractMembershipState {

    @Override
    public MembershipStatus status() {
        return MembershipStatus.PENDING;
    }

    @Override
    public void cancel(UserMembership membership, boolean immediate) {
        membership.setStatus(MembershipStatus.CANCELLED);
    }

    @Override
    public void changePlan(UserMembership membership) {
        reject("change plan");
    }

    @Override
    public void changeTier(UserMembership membership) {
        reject("change tier");
    }

    @Override
    public void expire(UserMembership membership) {
        membership.setStatus(MembershipStatus.EXPIRED);
    }
}
