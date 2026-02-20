package com.codeops.logger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Time-series data point for a metric. Stores individual or aggregated values
 * at configurable time resolutions.
 */
@Entity
@Table(name = "metric_series", indexes = {
        @Index(name = "idx_metric_series_metric_id", columnList = "metric_id"),
        @Index(name = "idx_metric_series_timestamp", columnList = "timestamp"),
        @Index(name = "idx_metric_series_metric_ts", columnList = "metric_id, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricSeries extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private Metric metric;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "value", nullable = false)
    private Double value;

    /** JSON dimension tags for this specific data point. */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    /** Aggregation window in seconds (e.g., 60 for 1-minute resolution). */
    @Column(name = "resolution", nullable = false)
    private Integer resolution;
}
