package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for metric time-series data, either raw or aggregated by resolution window.
 */
public record MetricTimeSeriesResponse(
        UUID metricId,
        String metricName,
        String serviceName,
        String metricType,
        String unit,
        Instant startTime,
        Instant endTime,
        Integer resolution,
        List<DataPoint> dataPoints
) {
    /**
     * Single data point in the time series.
     */
    public record DataPoint(
            Instant timestamp,
            Double value,
            String tags
    ) {}
}
