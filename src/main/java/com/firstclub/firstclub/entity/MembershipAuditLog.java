package com.firstclub.firstclub.entity;

import com.firstclub.firstclub.constants.MembershipEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "membership_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private UserMembership membership;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private MembershipEventType eventType;

    @Column(name = "old_value_json", columnDefinition = "JSON")
    private String oldValueJson;

    @Column(name = "new_value_json", columnDefinition = "JSON")
    private String newValueJson;

    @Column(name = "triggered_by", length = 64)
    private String triggeredBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
