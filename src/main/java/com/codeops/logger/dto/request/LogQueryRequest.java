package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * Request DTO for querying logs with structured filters.
 */
public record LogQueryRequest(
        String serviceName,
        String level,
        Instant startTime,
        Instant endTime,
        String correlationId,
        String query,
        String loggerName,
        String exceptionClass,
        String hostName,

        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {}
