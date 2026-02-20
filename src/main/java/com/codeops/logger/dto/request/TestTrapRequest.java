package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for testing a trap against historical logs.
 */
public record TestTrapRequest(
        @Min(value = 1, message = "Hours back must be at least 1")
        @Max(value = 168, message = "Hours back must not exceed 168 (7 days)")
        int hoursBack
) {}
