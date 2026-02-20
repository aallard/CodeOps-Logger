package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.MetricType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Metric definition and registration. Each metric has a name, type, and belongs to a service.
 * Metric data points are stored in {@link MetricSeries}.
 */
@Entity
@Table(name = "metrics", indexes = {
        @Index(name = "idx_metric_team_id", columnList = "team_id"),
        @Index(name = "idx_metric_service_name", columnList = "service_name"),
        @Index(name = "idx_metric_name_service", columnList = "name, service_name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_metric_name_service_team",
                columnNames = {"name", "service_name", "team_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metric extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 20)
    private MetricType metricType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Unit of measurement (e.g., "ms", "bytes", "count", "percent"). */
    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    /** JSON key-value labels for metric dimensions. */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;
}
