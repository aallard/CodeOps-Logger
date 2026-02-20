package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a widget to a dashboard.
 */
public record CreateDashboardWidgetRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @NotBlank(message = "Widget type is required")
        String widgetType,

        String queryJson,
        String configJson,

        @Min(0) Integer gridX,
        @Min(0) Integer gridY,
        @Min(1) @Max(12) Integer gridWidth,
        @Min(1) @Max(12) Integer gridHeight,

        Integer sortOrder
) {}
