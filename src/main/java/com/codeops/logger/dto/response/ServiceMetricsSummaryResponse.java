package com.codeops.logger.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Summary of all metrics for a service.
 */
public record ServiceMetricsSummaryResponse(
        String serviceName,
        int metricCount,
        Map<String, Long> metricsByType,
        List<MetricResponse> metrics
) {}
