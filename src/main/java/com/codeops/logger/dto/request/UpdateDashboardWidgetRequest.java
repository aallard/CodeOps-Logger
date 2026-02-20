package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a dashboard widget.
 */
public record UpdateDashboardWidgetRequest(
        @Size(max = 200) String title,
        String widgetType,
        String queryJson,
        String configJson,
        @Min(0) Integer gridX,
        @Min(0) Integer gridY,
        @Min(1) @Max(12) Integer gridWidth,
        @Min(1) @Max(12) Integer gridHeight,
        Integer sortOrder
) {}
