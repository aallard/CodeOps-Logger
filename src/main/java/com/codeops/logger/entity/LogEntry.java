package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.LogLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Individual log entry ingested via HTTP push or Kafka consumer.
 * The core data record for the Logger query engine.
 */
@Entity
@Table(name = "log_entries", indexes = {
        @Index(name = "idx_log_entry_team_id", columnList = "team_id"),
        @Index(name = "idx_log_entry_source_id", columnList = "source_id"),
        @Index(name = "idx_log_entry_level", columnList = "level"),
        @Index(name = "idx_log_entry_timestamp", columnList = "timestamp"),
        @Index(name = "idx_log_entry_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_log_entry_service_name", columnList = "service_name"),
        @Index(name = "idx_log_entry_service_level_ts", columnList = "service_name, level, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private LogSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 10)
    private LogLevel level;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Timestamp when the log was originally generated (not when ingested). */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /** Denormalized service name for fast queries without joining LogSource. */
    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "span_id", length = 100)
    private String spanId;

    @Column(name = "logger_name", length = 500)
    private String loggerName;

    @Column(name = "thread_name", length = 200)
    private String threadName;

    @Column(name = "exception_class", length = 500)
    private String exceptionClass;

    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    /** Arbitrary key-value pairs stored as JSON. */
    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields;

    @Column(name = "host_name", length = 200)
    private String hostName;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;
}
