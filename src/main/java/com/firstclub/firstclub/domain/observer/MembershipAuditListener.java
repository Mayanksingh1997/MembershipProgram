package com.firstclub.firstclub.domain.observer;

import com.firstclub.firstclub.entity.MembershipAuditLog;
import com.firstclub.firstclub.repository.MembershipAuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class MembershipAuditListener implements MembershipEventListener {

    private final MembershipAuditLogRepository auditLogRepository;

    public MembershipAuditListener(MembershipAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void onMembershipEvent(MembershipEvent event) {
        MembershipAuditLog auditLog = MembershipAuditLog.builder()
                .membership(event.getMembership())
                .eventType(event.getEventType())
                .oldValueJson(event.getOldValueJson())
                .newValueJson(event.getNewValueJson())
                .triggeredBy(event.getTriggeredBy())
                .build();
        auditLogRepository.save(auditLog);
    }
}
