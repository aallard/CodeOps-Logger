package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a log trap with its conditions.
 */
public record LogTrapResponse(
        UUID id,
        String name,
        String description,
        String trapType,
        Boolean isActive,
        UUID teamId,
        UUID createdBy,
        Instant lastTriggeredAt,
        Long triggerCount,
        List<TrapConditionResponse> conditions,
        Instant createdAt,
        Instant updatedAt
) {}
