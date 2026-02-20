package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for log storage usage statistics.
 */
public record StorageUsageResponse(
        long totalLogEntries,
        long totalMetricDataPoints,
        long totalTraceSpans,
        Map<String, Long> logEntriesByService,
        Map<String, Long> logEntriesByLevel,
        int activeRetentionPolicies,
        Instant oldestLogEntry,
        Instant newestLogEntry
) {}
