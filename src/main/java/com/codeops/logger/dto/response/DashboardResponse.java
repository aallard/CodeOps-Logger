package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a dashboard with its widgets.
 */
public record DashboardResponse(
        UUID id,
        String name,
        String description,
        UUID teamId,
        UUID createdBy,
        Boolean isShared,
        Boolean isTemplate,
        Integer refreshIntervalSeconds,
        String layoutJson,
        List<DashboardWidgetResponse> widgets,
        Instant createdAt,
        Instant updatedAt
) {}
