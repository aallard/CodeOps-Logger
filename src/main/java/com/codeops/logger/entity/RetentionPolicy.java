package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.entity.enums.RetentionAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Configurable log retention policy that determines how long logs are kept
 * and what happens when they expire (purge or archive to S3).
 */
@Entity
@Table(name = "retention_policies", indexes = {
        @Index(name = "idx_retention_policy_team_id", columnList = "team_id"),
        @Index(name = "idx_retention_policy_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetentionPolicy extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Optional filter: only apply to logs from this source name. */
    @Column(name = "source_name", length = 200)
    private String sourceName;

    /** Optional filter: only apply to logs at this level or below. */
    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", length = 10)
    private LogLevel logLevel;

    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private RetentionAction action;

    /** S3 bucket/path destination for ARCHIVE action (null for PURGE). */
    @Column(name = "archive_destination", length = 500)
    private String archiveDestination;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;
}
