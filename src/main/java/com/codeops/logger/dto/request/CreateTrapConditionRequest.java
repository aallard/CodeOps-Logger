package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for a condition within a log trap.
 */
public record CreateTrapConditionRequest(
        @NotBlank(message = "Condition type is required")
        String conditionType,

        @NotBlank(message = "Field is required")
        @Size(max = 100, message = "Field must not exceed 100 characters")
        String field,

        String pattern,

        @Min(value = 1, message = "Threshold must be at least 1")
        Integer threshold,

        @Min(value = 1, message = "Window must be at least 1 second")
        Integer windowSeconds,

        @Size(max = 200) String serviceName,
        String logLevel
) {}
