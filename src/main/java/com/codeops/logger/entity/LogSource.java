package com.codeops.logger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a registered log source (service or application) that sends logs to Logger.
 * Sources are tagged with service identity from the Registry.
 */
@Entity
@Table(name = "log_sources", indexes = {
        @Index(name = "idx_log_source_team_id", columnList = "team_id"),
        @Index(name = "idx_log_source_name", columnList = "name"),
        @Index(name = "idx_log_source_service_id", columnList = "service_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogSource extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** UUID reference to the service in CodeOps-Registry (nullable if manually registered). */
    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "environment", length = 50)
    private String environment;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "last_log_received_at")
    private Instant lastLogReceivedAt;

    @Column(name = "log_count", nullable = false)
    @Builder.Default
    private Long logCount = 0L;
}
