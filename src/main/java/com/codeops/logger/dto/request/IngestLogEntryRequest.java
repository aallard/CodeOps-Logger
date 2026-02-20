package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request DTO for ingesting a single log entry via HTTP push.
 */
public record IngestLogEntryRequest(
        @NotBlank(message = "Level is required")
        String level,

        @NotBlank(message = "Message is required")
        @Size(max = 65536, message = "Message must not exceed 65536 characters")
        String message,

        /** ISO-8601 timestamp when the log was generated. Defaults to server time if null. */
        Instant timestamp,

        @NotBlank(message = "Service name is required")
        @Size(max = 200, message = "Service name must not exceed 200 characters")
        String serviceName,

        @Size(max = 100) String correlationId,
        @Size(max = 100) String traceId,
        @Size(max = 100) String spanId,
        @Size(max = 500) String loggerName,
        @Size(max = 200) String threadName,
        @Size(max = 500) String exceptionClass,
        String exceptionMessage,
        String stackTrace,

        /** Arbitrary key-value pairs as JSON string. */
        String customFields,

        @Size(max = 200) String hostName,
        @Size(max = 45) String ipAddress
) {}
