package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a metric time-series data point.
 */
public record MetricDataPointResponse(
        UUID id,
        UUID metricId,
        Instant timestamp,
        Double value,
        String tags,
        Integer resolution
) {}
