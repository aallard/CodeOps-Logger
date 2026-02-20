package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for reordering widgets within a dashboard.
 */
public record ReorderWidgetsRequest(
        @NotEmpty(message = "Widget IDs list must not be empty")
        List<UUID> widgetIds
) {}
