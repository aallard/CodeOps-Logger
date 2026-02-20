package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single trace span.
 */
public record TraceSpanResponse(
        UUID id,
        String correlationId,
        String traceId,
        String spanId,
        String parentSpanId,
        String serviceName,
        String operationName,
        Instant startTime,
        Instant endTime,
        Long durationMs,
        String status,
        String statusMessage,
        String tags,
        UUID teamId,
        Instant createdAt
) {}
