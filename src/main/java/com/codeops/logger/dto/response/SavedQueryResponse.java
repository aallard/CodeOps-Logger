package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a saved log query.
 */
public record SavedQueryResponse(
        UUID id,
        String name,
        String description,
        String queryJson,
        String queryDsl,
        UUID teamId,
        UUID createdBy,
        Boolean isShared,
        Instant lastExecutedAt,
        Long executionCount,
        Instant createdAt,
        Instant updatedAt
) {}
