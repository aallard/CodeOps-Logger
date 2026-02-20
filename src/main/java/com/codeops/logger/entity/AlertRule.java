package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.AlertSeverity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Connects a log trap to an alert channel with routing configuration.
 * When a trap fires, all active rules route the alert to their configured channels.
 */
@Entity
@Table(name = "alert_rules", indexes = {
        @Index(name = "idx_alert_rule_team_id", columnList = "team_id"),
        @Index(name = "idx_alert_rule_trap_id", columnList = "trap_id"),
        @Index(name = "idx_alert_rule_channel_id", columnList = "channel_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trap_id", nullable = false)
    private LogTrap trap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private AlertChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Minimum minutes between repeated alerts from the same rule. */
    @Column(name = "throttle_minutes", nullable = false)
    @Builder.Default
    private Integer throttleMinutes = 5;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;
}
