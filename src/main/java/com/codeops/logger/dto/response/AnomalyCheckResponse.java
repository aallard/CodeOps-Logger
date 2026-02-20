package com.codeops.logger.dto.response;

import java.time.Instant;

/**
 * Result of checking a current value against an anomaly baseline.
 */
public record AnomalyCheckResponse(
        String serviceName,
        String metricName,
        double currentValue,
        double baselineValue,
        double standardDeviation,
        double deviationThreshold,
        double zScore,
        boolean isAnomaly,
        String direction,
        Instant checkedAt
) {}
