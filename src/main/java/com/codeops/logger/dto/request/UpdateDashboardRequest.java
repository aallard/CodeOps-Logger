package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a dashboard.
 */
public record UpdateDashboardRequest(
        @Size(max = 200) String name,
        @Size(max = 5000) String description,
        Boolean isShared,
        Boolean isTemplate,
        @Min(5) @Max(3600) Integer refreshIntervalSeconds,
        String layoutJson
) {}
