package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request DTO for recording a trace span.
 */
public record CreateTraceSpanRequest(
        @NotBlank(message = "Correlation ID is required")
        @Size(max = 100) String correlationId,

        @NotBlank(message = "Trace ID is required")
        @Size(max = 100) String traceId,

        @NotBlank(message = "Span ID is required")
        @Size(max = 100) String spanId,

        @Size(max = 100) String parentSpanId,

        @NotBlank(message = "Service name is required")
        @Size(max = 200) String serviceName,

        @NotBlank(message = "Operation name is required")
        @Size(max = 500) String operationName,

        @NotNull(message = "Start time is required")
        Instant startTime,

        Instant endTime,
        Long durationMs,

        String status,
        String statusMessage,
        String tags
) {}
