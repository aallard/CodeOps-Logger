package com.codeops.logger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Baseline pattern learned from historical data for anomaly detection.
 * When current metric values deviate beyond the threshold from the baseline,
 * an anomaly alert is triggered.
 */
@Entity
@Table(name = "anomaly_baselines", indexes = {
        @Index(name = "idx_anomaly_baseline_team_id", columnList = "team_id"),
        @Index(name = "idx_anomaly_baseline_service", columnList = "service_name"),
        @Index(name = "idx_anomaly_baseline_metric", columnList = "service_name, metric_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyBaseline extends BaseEntity {

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    /** Metric being tracked (e.g., "log_volume", "error_rate", "avg_latency"). */
    @Column(name = "metric_name", nullable = false, length = 200)
    private String metricName;

    /** Computed average value from historical data. */
    @Column(name = "baseline_value", nullable = false)
    private Double baselineValue;

    @Column(name = "standard_deviation", nullable = false)
    private Double standardDeviation;

    /** Number of data points used to compute the baseline. */
    @Column(name = "sample_count", nullable = false)
    private Long sampleCount;

    @Column(name = "window_start_time", nullable = false)
    private Instant windowStartTime;

    @Column(name = "window_end_time", nullable = false)
    private Instant windowEndTime;

    /** Number of standard deviations from baseline to trigger anomaly. */
    @Column(name = "deviation_threshold", nullable = false)
    @Builder.Default
    private Double deviationThreshold = 2.0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "last_computed_at")
    private Instant lastComputedAt;
}
