package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.AlertSeverity;
import com.codeops.logger.entity.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Record of a fired alert, tracking its lifecycle from firing through acknowledgment to resolution.
 */
@Entity
@Table(name = "alert_history", indexes = {
        @Index(name = "idx_alert_history_team_id", columnList = "team_id"),
        @Index(name = "idx_alert_history_rule_id", columnList = "rule_id"),
        @Index(name = "idx_alert_history_status", columnList = "status"),
        @Index(name = "idx_alert_history_severity", columnList = "severity"),
        @Index(name = "idx_alert_history_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRule rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trap_id", nullable = false)
    private LogTrap trap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private AlertChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.FIRED;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;
}
