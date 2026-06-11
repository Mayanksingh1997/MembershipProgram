package com.firstclub.firstclub.domain.subscription.state;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.entity.UserMembership;
import com.firstclub.firstclub.exception.MembershipException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipStateTest {

    @Test
    void activeState_allowsCancelAndExpire() {
        ActiveMembershipState state = new ActiveMembershipState();
        UserMembership membership = UserMembership.builder()
                .status(MembershipStatus.ACTIVE)
                .autoRenew(true)
                .build();

        state.cancel(membership, true);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.CANCELLED);

        membership.setStatus(MembershipStatus.ACTIVE);
        state.expire(membership);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
        assertThat(membership.getAutoRenew()).isFalse();
    }

    @Test
    void cancelledState_rejectsFurtherChanges() {
        CancelledMembershipState state = new CancelledMembershipState();
        UserMembership membership = UserMembership.builder()
                .status(MembershipStatus.CANCELLED)
                .build();

        assertThatThrownBy(() -> state.changeTier(membership))
                .isInstanceOf(MembershipException.class);
    }
}
