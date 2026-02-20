package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a query execution history entry.
 */
public record QueryHistoryResponse(
        UUID id,
        String queryJson,
        String queryDsl,
        Long resultCount,
        Long executionTimeMs,
        UUID createdBy,
        Instant createdAt
) {}
