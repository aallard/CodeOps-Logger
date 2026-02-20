package com.codeops.logger.dto.response;

import java.util.List;

/**
 * Assembled trace flow showing all spans for a given correlation ID,
 * used for waterfall visualization.
 */
public record TraceFlowResponse(
        String correlationId,
        String traceId,
        List<TraceSpanResponse> spans,
        Long totalDurationMs,
        int spanCount,
        boolean hasErrors
) {}
