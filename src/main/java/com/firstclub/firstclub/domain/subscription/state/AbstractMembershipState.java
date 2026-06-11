package com.firstclub.firstclub.domain.subscription.state;

import com.firstclub.firstclub.exception.MembershipException;
import org.springframework.http.HttpStatus;

public abstract class AbstractMembershipState implements MembershipState {

    protected void reject(String action) {
        throw new MembershipException(
                "Cannot " + action + " while membership is " + status().name(),
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_STATE_TRANSITION");
    }
}
