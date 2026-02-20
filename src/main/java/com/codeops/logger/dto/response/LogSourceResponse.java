package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for log source details.
 */
public record LogSourceResponse(
        UUID id,
        String name,
        UUID serviceId,
        String description,
        String environment,
        Boolean isActive,
        UUID teamId,
        Instant lastLogReceivedAt,
        Long logCount,
        Instant createdAt,
        Instant updatedAt
) {}
