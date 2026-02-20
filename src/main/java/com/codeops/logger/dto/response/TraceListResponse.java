package com.codeops.logger.dto.response;

import java.time.Instant;

/**
 * Summary entry for listing recent traces.
 */
public record TraceListResponse(
        String correlationId,
        String traceId,
        String rootService,
        String rootOperation,
        int spanCount,
        int serviceCount,
        Long totalDurationMs,
        boolean hasErrors,
        Instant startTime,
        Instant endTime
) {}
