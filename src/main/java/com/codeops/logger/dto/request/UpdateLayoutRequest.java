package com.codeops.logger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for batch-updating widget grid positions.
 */
public record UpdateLayoutRequest(
        @NotEmpty(message = "Positions list must not be empty")
        List<@Valid WidgetPositionUpdate> positions
) {}
