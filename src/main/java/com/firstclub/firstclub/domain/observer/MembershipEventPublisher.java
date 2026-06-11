package com.firstclub.firstclub.domain.observer;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MembershipEventPublisher {

    private final List<MembershipEventListener> listeners;

    public MembershipEventPublisher(List<MembershipEventListener> listeners) {
        this.listeners = listeners;
    }

    public void publish(MembershipEvent event) {
        for (MembershipEventListener listener : listeners) {
            listener.onMembershipEvent(event);
        }
    }
}
