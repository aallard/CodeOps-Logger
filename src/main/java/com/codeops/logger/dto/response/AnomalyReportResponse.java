package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Comprehensive anomaly report for a team showing all detected anomalies.
 */
public record AnomalyReportResponse(
        UUID teamId,
        Instant generatedAt,
        int totalBaselines,
        int anomaliesDetected,
        List<AnomalyCheckResponse> anomalies,
        List<AnomalyCheckResponse> allChecks
) {}
