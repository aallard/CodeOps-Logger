package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single log entry.
 */
public record LogEntryResponse(
        UUID id,
        UUID sourceId,
        String sourceName,
        String level,
        String message,
        Instant timestamp,
        String serviceName,
        String correlationId,
        String traceId,
        String spanId,
        String loggerName,
        String threadName,
        String exceptionClass,
        String exceptionMessage,
        String stackTrace,
        String customFields,
        String hostName,
        String ipAddress,
        UUID teamId,
        Instant createdAt
) {}
