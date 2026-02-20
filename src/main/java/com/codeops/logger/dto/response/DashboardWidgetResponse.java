package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a dashboard widget.
 */
public record DashboardWidgetResponse(
        UUID id,
        UUID dashboardId,
        String title,
        String widgetType,
        String queryJson,
        String configJson,
        Integer gridX,
        Integer gridY,
        Integer gridWidth,
        Integer gridHeight,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {}
