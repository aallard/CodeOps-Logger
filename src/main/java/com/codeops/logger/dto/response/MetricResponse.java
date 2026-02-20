package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a metric definition.
 */
public record MetricResponse(
        UUID id,
        String name,
        String metricType,
        String description,
        String unit,
        String serviceName,
        String tags,
        UUID teamId,
        Instant createdAt,
        Instant updatedAt
) {}
