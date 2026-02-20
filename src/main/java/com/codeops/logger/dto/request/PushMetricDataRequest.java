package com.codeops.logger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for pushing metric data points.
 */
public record PushMetricDataRequest(
        @NotNull(message = "Metric ID is required")
        UUID metricId,

        @NotEmpty(message = "At least one data point is required")
        @Size(max = 1000, message = "Maximum 1000 data points per push")
        List<@Valid MetricDataPoint> dataPoints
) {

    /**
     * Individual metric data point within a push request.
     */
    public record MetricDataPoint(
            @NotNull(message = "Timestamp is required")
            Instant timestamp,

            @NotNull(message = "Value is required")
            Double value,

            /** JSON dimension tags. */
            String tags
    ) {}
}
