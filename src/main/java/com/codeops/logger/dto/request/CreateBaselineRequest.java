package com.codeops.logger.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for manually creating or recalculating an anomaly baseline.
 */
public record CreateBaselineRequest(
        @NotBlank(message = "Service name is required")
        @Size(max = 200) String serviceName,

        @NotBlank(message = "Metric name is required")
        @Size(max = 200) String metricName,

        @Min(value = 1, message = "Window hours must be at least 1")
        @Max(value = 720, message = "Window hours must not exceed 720")
        Integer windowHours,

        @Min(value = 1) @Max(value = 5)
        Double deviationThreshold
) {}
