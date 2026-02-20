package com.codeops.logger.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a log retention policy.
 */
public record CreateRetentionPolicyRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 200) String sourceName,

        String logLevel,

        @NotNull(message = "Retention days is required")
        @Min(value = 1, message = "Retention days must be at least 1")
        @Max(value = 365, message = "Retention days must not exceed 365")
        Integer retentionDays,

        @NotBlank(message = "Action is required")
        String action,

        @Size(max = 500) String archiveDestination
) {}
