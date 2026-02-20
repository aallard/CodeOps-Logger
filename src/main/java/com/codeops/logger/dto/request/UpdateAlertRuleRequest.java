package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating an alert rule.
 */
public record UpdateAlertRuleRequest(
        @Size(max = 200) String name,
        UUID trapId,
        UUID channelId,
        String severity,
        Boolean isActive,
        @Min(1) @Max(1440) Integer throttleMinutes
) {}
