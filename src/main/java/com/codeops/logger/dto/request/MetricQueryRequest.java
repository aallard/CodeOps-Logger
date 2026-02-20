package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for querying metric time-series data.
 */
public record MetricQueryRequest(
        @NotNull(message = "Metric ID is required")
        UUID metricId,

        @NotNull(message = "Start time is required")
        Instant startTime,

        @NotNull(message = "End time is required")
        Instant endTime,

        /** Aggregation window in seconds. Null = raw data points. */
        @Min(value = 10, message = "Resolution must be at least 10 seconds")
        @Max(value = 3600, message = "Resolution must not exceed 3600 seconds")
        Integer resolution
) {}
