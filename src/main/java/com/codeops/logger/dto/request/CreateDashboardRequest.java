package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new dashboard.
 */
public record CreateDashboardRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 5000) String description,

        Boolean isShared,

        @Min(value = 5, message = "Refresh interval must be at least 5 seconds")
        @Max(value = 3600, message = "Refresh interval must not exceed 3600 seconds")
        Integer refreshIntervalSeconds,

        String layoutJson
) {}
