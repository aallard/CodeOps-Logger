package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.TrapType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pattern-based alerting trigger that fires when log entries match configured conditions.
 * Supports regex match, keyword match, frequency threshold, and absence detection.
 */
@Entity
@Table(name = "log_traps", indexes = {
        @Index(name = "idx_log_trap_team_id", columnList = "team_id"),
        @Index(name = "idx_log_trap_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogTrap extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trap_type", nullable = false, length = 20)
    private TrapType trapType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "trigger_count", nullable = false)
    @Builder.Default
    private Long triggerCount = 0L;

    @OneToMany(mappedBy = "trap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TrapCondition> conditions = new ArrayList<>();
}
