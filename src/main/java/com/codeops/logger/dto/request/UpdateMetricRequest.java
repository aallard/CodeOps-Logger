package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a metric definition.
 */
public record UpdateMetricRequest(
        @Size(max = 5000) String description,
        @Size(max = 50) String unit,
        String tags
) {}
