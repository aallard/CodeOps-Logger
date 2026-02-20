package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating an anomaly baseline configuration.
 */
public record UpdateBaselineRequest(
        @Min(1) @Max(720) Integer windowHours,
        @Min(1) @Max(5) Double deviationThreshold,
        Boolean isActive
) {}
