package com.firstclub.firstclub.domain.factory;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.domain.subscription.state.ActiveMembershipState;
import com.firstclub.firstclub.domain.subscription.state.CancelledMembershipState;
import com.firstclub.firstclub.domain.subscription.state.ExpiredMembershipState;
import com.firstclub.firstclub.domain.subscription.state.MembershipState;
import com.firstclub.firstclub.domain.subscription.state.PendingMembershipState;
import com.firstclub.firstclub.exception.MembershipException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MembershipStateFactory {

    private final Map<MembershipStatus, MembershipState> states = new EnumMap<>(MembershipStatus.class);

    @Autowired
    public MembershipStateFactory(
            PendingMembershipState pendingState,
            ActiveMembershipState activeState,
            CancelledMembershipState cancelledState,
            ExpiredMembershipState expiredState) {
        states.put(MembershipStatus.PENDING, pendingState);
        states.put(MembershipStatus.ACTIVE, activeState);
        states.put(MembershipStatus.CANCELLED, cancelledState);
        states.put(MembershipStatus.EXPIRED, expiredState);
    }

    public MembershipState create(MembershipStatus status) {
        MembershipState state = states.get(status);
        if (state == null) {
            throw new MembershipException(
                    "No state handler for status: " + status,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "STATE_NOT_FOUND");
        }
        return state;
    }
}
