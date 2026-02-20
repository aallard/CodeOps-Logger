package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a fired alert history record.
 */
public record AlertHistoryResponse(
        UUID id,
        UUID ruleId,
        String ruleName,
        UUID trapId,
        String trapName,
        UUID channelId,
        String channelName,
        String severity,
        String status,
        String message,
        UUID acknowledgedBy,
        Instant acknowledgedAt,
        UUID resolvedBy,
        Instant resolvedAt,
        UUID teamId,
        Instant createdAt
) {}
