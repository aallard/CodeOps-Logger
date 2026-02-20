package com.codeops.logger.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * Request DTO for creating an alert rule connecting a trap to a channel.
 */
public record CreateAlertRuleRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotNull(message = "Trap ID is required")
        UUID trapId,

        @NotNull(message = "Channel ID is required")
        UUID channelId,

        @NotBlank(message = "Severity is required")
        String severity,

        @Min(value = 1, message = "Throttle must be at least 1 minute")
        @Max(value = 1440, message = "Throttle must not exceed 1440 minutes")
        Integer throttleMinutes
) {}
