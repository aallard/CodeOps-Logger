package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a log retention policy.
 */
public record RetentionPolicyResponse(
        UUID id,
        String name,
        String sourceName,
        String logLevel,
        Integer retentionDays,
        String action,
        String archiveDestination,
        Boolean isActive,
        UUID teamId,
        UUID createdBy,
        Instant lastExecutedAt,
        Instant createdAt,
        Instant updatedAt
) {}
