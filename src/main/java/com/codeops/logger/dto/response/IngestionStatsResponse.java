package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for log ingestion statistics.
 */
public record IngestionStatsResponse(
        long totalLogsIngested,
        double logsPerSecond,
        int activeSourceCount,
        Map<String, Long> logsByLevel,
        Map<String, Long> logsByService,
        Instant since
) {}
