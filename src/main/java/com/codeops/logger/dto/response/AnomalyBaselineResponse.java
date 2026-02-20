package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an anomaly detection baseline.
 */
public record AnomalyBaselineResponse(
        UUID id,
        String serviceName,
        String metricName,
        Double baselineValue,
        Double standardDeviation,
        Long sampleCount,
        Instant windowStartTime,
        Instant windowEndTime,
        Double deviationThreshold,
        Boolean isActive,
        UUID teamId,
        Instant lastComputedAt,
        Instant createdAt,
        Instant updatedAt
) {}
