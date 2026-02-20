package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Position update for a single widget in a grid layout update.
 */
public record WidgetPositionUpdate(
        @NotNull(message = "Widget ID is required")
        UUID widgetId,

        @NotNull @Min(0) Integer gridX,
        @NotNull @Min(0) Integer gridY,
        @NotNull @Min(1) @Max(12) Integer gridWidth,
        @NotNull @Min(1) @Max(12) Integer gridHeight
) {}
