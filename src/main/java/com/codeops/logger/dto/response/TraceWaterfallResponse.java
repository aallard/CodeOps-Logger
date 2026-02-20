package com.codeops.logger.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Waterfall visualization data for a trace, showing hierarchical span timing
 * relative to the root span's start time.
 */
public record TraceWaterfallResponse(
        String correlationId,
        String traceId,
        Long totalDurationMs,
        int spanCount,
        int serviceCount,
        boolean hasErrors,
        List<WaterfallSpan> spans
) {
    /**
     * Span within the waterfall, with relative timing for visualization.
     */
    public record WaterfallSpan(
            UUID id,
            String spanId,
            String parentSpanId,
            String serviceName,
            String operationName,
            /** Offset from root span start in milliseconds. */
            long offsetMs,
            /** Duration of this span in milliseconds. */
            long durationMs,
            String status,
            String statusMessage,
            int depth,
            List<UUID> relatedLogIds
    ) {}
}
