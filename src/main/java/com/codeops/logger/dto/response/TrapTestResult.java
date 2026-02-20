package com.codeops.logger.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Result of testing a trap against historical logs.
 */
public record TrapTestResult(
        /** Number of log entries that matched the trap conditions. */
        int matchCount,

        /** Total number of log entries evaluated. */
        int totalEvaluated,

        /** Sample of matching log entry IDs (up to 100). */
        List<UUID> sampleMatchIds,

        /** Start of the evaluated time range. */
        Instant evaluatedFrom,

        /** End of the evaluated time range. */
        Instant evaluatedTo,

        /** Match percentage. */
        double matchPercentage
) {

    /**
     * Factory method to create a TrapTestResult with computed match percentage.
     *
     * @param matchCount     number of matching entries
     * @param totalEvaluated total entries evaluated
     * @param sampleMatchIds sample of matching entry IDs
     * @param from           evaluation start time
     * @param to             evaluation end time
     * @return a new TrapTestResult
     */
    public static TrapTestResult of(int matchCount, int totalEvaluated,
                                     List<UUID> sampleMatchIds, Instant from, Instant to) {
        double pct = totalEvaluated > 0 ? (matchCount * 100.0 / totalEvaluated) : 0.0;
        return new TrapTestResult(matchCount, totalEvaluated, sampleMatchIds, from, to, pct);
    }
}
