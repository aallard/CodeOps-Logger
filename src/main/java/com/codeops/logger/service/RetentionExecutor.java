package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.entity.RetentionPolicy;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.entity.enums.RetentionAction;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.MetricSeriesRepository;
import com.codeops.logger.repository.RetentionPolicyRepository;
import com.codeops.logger.repository.TraceSpanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Executes retention policies on a schedule, deleting or archiving log entries
 * that exceed their configured retention period. Supports manual execution
 * and global purge across all data tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionExecutor {

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final LogEntryRepository logEntryRepository;
    private final MetricSeriesRepository metricSeriesRepository;
    private final TraceSpanRepository traceSpanRepository;

    /**
     * Scheduled task that runs daily at 2:00 AM to execute all active retention policies.
     * Each policy is executed independently; failures are logged but do not stop other policies.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void executeAllActivePolicies() {
        log.info("Starting scheduled retention policy execution");
        List<RetentionPolicy> activePolicies = retentionPolicyRepository.findByIsActiveTrue();
        int executed = 0;
        int failed = 0;

        for (RetentionPolicy policy : activePolicies) {
            try {
                executePolicy(policy);
                executed++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to execute retention policy '{}' ({}): {}",
                        policy.getName(), policy.getId(), e.getMessage(), e);
            }
        }

        log.info("Retention policy execution completed: {} executed, {} failed out of {} active policies",
                executed, failed, activePolicies.size());
    }

    /**
     * Executes a single retention policy by deleting log entries older than the
     * policy's retention period. Applies optional source name and log level filters.
     * ARCHIVE action logs a warning and falls back to PURGE behavior.
     *
     * @param policy the retention policy to execute
     */
    @Transactional
    public void executePolicy(RetentionPolicy policy) {
        Instant cutoff = Instant.now().minus(policy.getRetentionDays(), ChronoUnit.DAYS);

        log.info("Executing retention policy '{}': action={}, retentionDays={}, cutoff={}, teamId={}",
                policy.getName(), policy.getAction(), policy.getRetentionDays(), cutoff, policy.getTeamId());

        if (policy.getAction() == RetentionAction.ARCHIVE) {
            log.warn("ARCHIVE action not yet implemented for policy '{}'. Falling back to PURGE.",
                    policy.getName());
        }

        deleteMatchingEntries(policy, cutoff);

        policy.setLastExecutedAt(Instant.now());
        retentionPolicyRepository.save(policy);

        log.info("Completed retention policy '{}' execution", policy.getName());
    }

    /**
     * Manually triggers execution of a specific retention policy.
     *
     * @param policyId the policy ID to execute
     * @param teamId   the owning team ID (for authorization)
     * @throws NotFoundException if the policy does not exist or belongs to another team
     */
    @Transactional
    public void manualExecute(UUID policyId, UUID teamId) {
        RetentionPolicy policy = retentionPolicyRepository.findById(policyId)
                .orElseThrow(() -> new NotFoundException("Retention policy not found: " + policyId));
        if (!policy.getTeamId().equals(teamId)) {
            throw new NotFoundException("Retention policy not found: " + policyId);
        }

        log.info("Manual execution requested for retention policy '{}' ({})", policy.getName(), policyId);
        executePolicy(policy);
    }

    /**
     * Purges all data older than the specified number of days across log entries,
     * metric data points, and trace spans.
     *
     * @param retentionDays the number of days of data to retain
     * @throws ValidationException if retentionDays is outside allowed bounds
     */
    @Transactional
    public void globalPurge(int retentionDays) {
        if (retentionDays < AppConstants.MIN_RETENTION_DAYS || retentionDays > AppConstants.MAX_RETENTION_DAYS) {
            throw new ValidationException("Retention days must be between "
                    + AppConstants.MIN_RETENTION_DAYS + " and " + AppConstants.MAX_RETENTION_DAYS);
        }

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Executing global purge: retentionDays={}, cutoff={}", retentionDays, cutoff);

        logEntryRepository.deleteByTimestampBefore(cutoff);
        metricSeriesRepository.deleteByTimestampBefore(cutoff);
        traceSpanRepository.deleteByStartTimeBefore(cutoff);

        log.info("Global purge completed for data older than {} days", retentionDays);
    }

    /**
     * Returns all {@link LogLevel} values at or below the specified level (less severe).
     * Used to determine which log levels a retention policy applies to.
     *
     * @param level the maximum log level (inclusive)
     * @return list of log levels with ordinal values {@code <=} the given level
     */
    List<LogLevel> getLevelsAtOrBelow(LogLevel level) {
        List<LogLevel> levels = new ArrayList<>();
        for (LogLevel l : LogLevel.values()) {
            if (l.ordinal() <= level.ordinal()) {
                levels.add(l);
            }
        }
        return levels;
    }

    /**
     * Deletes log entries matching the policy's filters (source name and/or log level)
     * that are older than the given cutoff timestamp.
     *
     * @param policy the retention policy defining filters
     * @param cutoff the cutoff timestamp; entries before this are deleted
     */
    private void deleteMatchingEntries(RetentionPolicy policy, Instant cutoff) {
        boolean hasSource = policy.getSourceName() != null && !policy.getSourceName().isBlank();
        boolean hasLevel = policy.getLogLevel() != null;

        if (hasSource && hasLevel) {
            List<LogLevel> levels = getLevelsAtOrBelow(policy.getLogLevel());
            logEntryRepository.deleteByTeamIdAndServiceNameAndTimestampBeforeAndLevelIn(
                    policy.getTeamId(), policy.getSourceName(), cutoff, levels);
        } else if (hasSource) {
            logEntryRepository.deleteByTeamIdAndServiceNameAndTimestampBefore(
                    policy.getTeamId(), policy.getSourceName(), cutoff);
        } else if (hasLevel) {
            List<LogLevel> levels = getLevelsAtOrBelow(policy.getLogLevel());
            logEntryRepository.deleteByTeamIdAndTimestampBeforeAndLevelIn(
                    policy.getTeamId(), cutoff, levels);
        } else {
            logEntryRepository.deleteByTeamIdAndTimestampBefore(policy.getTeamId(), cutoff);
        }
    }
}
