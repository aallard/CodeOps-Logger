package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an alert rule connecting traps to channels.
 */
public record AlertRuleResponse(
        UUID id,
        String name,
        UUID trapId,
        String trapName,
        UUID channelId,
        String channelName,
        String severity,
        Boolean isActive,
        Integer throttleMinutes,
        UUID teamId,
        Instant createdAt,
        Instant updatedAt
) {}
