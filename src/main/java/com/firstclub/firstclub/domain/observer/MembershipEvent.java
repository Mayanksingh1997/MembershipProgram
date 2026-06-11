package com.firstclub.firstclub.domain.observer;

import com.firstclub.firstclub.constants.MembershipEventType;
import com.firstclub.firstclub.entity.UserMembership;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipEvent {

    private UserMembership membership;
    private MembershipEventType eventType;
    private String oldValueJson;
    private String newValueJson;
    private String triggeredBy;
}
