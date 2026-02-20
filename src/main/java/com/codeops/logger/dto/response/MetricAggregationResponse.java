package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for aggregated metric data over a time window.
 */
public record MetricAggregationResponse(
        UUID metricId,
        String metricName,
        String serviceName,
        Instant startTime,
        Instant endTime,
        long dataPointCount,
        Double sum,
        Double avg,
        Double min,
        Double max,
        Double p50,
        Double p95,
        Double p99,
        Double stddev
) {}
