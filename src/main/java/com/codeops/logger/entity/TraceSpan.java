package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.SpanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Individual span in a distributed trace. Spans are assembled into cross-service
 * request traces via shared correlationId/traceId for waterfall visualization.
 */
@Entity
@Table(name = "trace_spans", indexes = {
        @Index(name = "idx_trace_span_team_id", columnList = "team_id"),
        @Index(name = "idx_trace_span_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_trace_span_trace_id", columnList = "trace_id"),
        @Index(name = "idx_trace_span_service_name", columnList = "service_name"),
        @Index(name = "idx_trace_span_start_time", columnList = "start_time"),
        @Index(name = "idx_trace_span_parent_span_id", columnList = "parent_span_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TraceSpan extends BaseEntity {

    /** Links to LogEntry.correlationId for associating logs with traces. */
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    /** Groups multiple spans into a single distributed trace. */
    @Column(name = "trace_id", nullable = false, length = 100)
    private String traceId;

    /** Unique identifier for this span within the trace. */
    @Column(name = "span_id", nullable = false, length = 100)
    private String spanId;

    /** Parent span ID (null for root span). */
    @Column(name = "parent_span_id", length = 100)
    private String parentSpanId;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "operation_name", nullable = false, length = 500)
    private String operationName;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    /** Duration in milliseconds (computed: endTime - startTime). */
    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private SpanStatus status = SpanStatus.OK;

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    /** JSON key-value tags for span metadata. */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;
}
