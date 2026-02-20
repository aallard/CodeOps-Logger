package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for registering a new metric definition.
 */
public record RegisterMetricRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotBlank(message = "Metric type is required")
        String metricType,

        @Size(max = 5000) String description,

        @Size(max = 50) String unit,

        @NotBlank(message = "Service name is required")
        @Size(max = 200, message = "Service name must not exceed 200 characters")
        String serviceName,

        /** JSON key-value tags. */
        String tags
) {}
