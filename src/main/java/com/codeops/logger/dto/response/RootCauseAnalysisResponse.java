package com.codeops.logger.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Root cause analysis for a failed trace, identifying the earliest error
 * and its causal chain.
 */
public record RootCauseAnalysisResponse(
        String correlationId,
        String traceId,
        /** The span where the first error occurred. */
        TraceSpanResponse rootCauseSpan,
        /** Service where the root cause originated. */
        String rootCauseService,
        /** Error message from the root cause span. */
        String rootCauseMessage,
        /** Chain of error spans from root cause to final error. */
        List<TraceSpanResponse> errorChain,
        /** Log entries associated with the root cause span. */
        List<UUID> relatedLogEntryIds,
        /** Total number of services impacted. */
        int impactedServiceCount,
        /** Total duration of the failed trace. */
        Long totalDurationMs
) {}
