package com.firstclub.firstclub.repository;

import com.firstclub.firstclub.entity.MembershipAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipAuditLogRepository extends JpaRepository<MembershipAuditLog, Long> {
}
