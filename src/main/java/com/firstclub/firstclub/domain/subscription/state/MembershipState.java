package com.firstclub.firstclub.domain.subscription.state;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.entity.UserMembership;

public interface MembershipState {

    MembershipStatus status();

    void cancel(UserMembership membership, boolean immediate);

    void changePlan(UserMembership membership);

    void changeTier(UserMembership membership);

    void expire(UserMembership membership);
}
