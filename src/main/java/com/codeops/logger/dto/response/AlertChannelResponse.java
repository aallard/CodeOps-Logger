package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for alert channel configuration.
 */
public record AlertChannelResponse(
        UUID id,
        String name,
        String channelType,
        String configuration,
        Boolean isActive,
        UUID teamId,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {}
