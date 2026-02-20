package com.codeops.logger.service;

import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.TrapCondition;
import com.codeops.logger.entity.enums.ConditionType;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.repository.LogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Stateless engine for evaluating log trap conditions against log entries.
 * Conditions within a trap use AND logic — all conditions must match for the trap to fire.
 * Supports regex matching, keyword matching, frequency threshold, and absence detection.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrapEvaluationEngine {

    private final LogEntryRepository logEntryRepository;

    /** Thread-safe cache for compiled regex patterns. */
    private static final int MAX_PATTERN_CACHE_SIZE = 1000;
    private final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    /**
     * Evaluates whether a log entry satisfies all conditions in a PATTERN trap.
     * Only evaluates REGEX and KEYWORD conditions — frequency and absence
     * are handled separately by scheduled evaluation.
     *
     * @param entry      the log entry to evaluate
     * @param conditions the trap conditions to check
     * @return true if ALL conditions match
     */
    public boolean evaluatePatternConditions(LogEntry entry, List<TrapCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (TrapCondition condition : conditions) {
            // Check optional service name filter
            if (condition.getServiceName() != null && !condition.getServiceName().isBlank()) {
                if (!condition.getServiceName().equals(entry.getServiceName())) {
                    return false;
                }
            }
            // Check optional log level filter
            if (condition.getLogLevel() != null) {
                if (!isLevelAtOrAbove(entry.getLevel(), condition.getLogLevel())) {
                    return false;
                }
            }

            ConditionType type = condition.getConditionType();
            if (type == ConditionType.REGEX) {
                if (!evaluateRegex(entry, condition)) {
                    return false;
                }
            } else if (type == ConditionType.KEYWORD) {
                if (!evaluateKeyword(entry, condition)) {
                    return false;
                }
            }
            // Skip FREQUENCY_THRESHOLD and ABSENCE — not applicable to single-entry eval
        }
        return true;
    }

    /**
     * Evaluates a regex condition against the specified field of a log entry.
     *
     * @param entry     the log entry
     * @param condition the regex condition (field + pattern)
     * @return true if the field value matches the regex pattern
     */
    boolean evaluateRegex(LogEntry entry, TrapCondition condition) {
        String fieldValue = extractFieldValue(entry, condition.getField());
        if (fieldValue == null) {
            return false;
        }
        try {
            Pattern pattern = getCompiledPattern(condition.getPattern());
            if (pattern == null) {
                return false;
            }
            return pattern.matcher(fieldValue).find();
        } catch (Exception e) {
            log.warn("Error evaluating regex '{}' on field '{}': {}",
                    condition.getPattern(), condition.getField(), e.getMessage());
            return false;
        }
    }

    /**
     * Evaluates a keyword condition against the specified field of a log entry.
     * Performs case-insensitive substring matching.
     *
     * @param entry     the log entry
     * @param condition the keyword condition (field + pattern as keyword)
     * @return true if the field value contains the keyword (case-insensitive)
     */
    boolean evaluateKeyword(LogEntry entry, TrapCondition condition) {
        String fieldValue = extractFieldValue(entry, condition.getField());
        if (fieldValue == null) {
            return false;
        }
        if (condition.getPattern() == null) {
            return false;
        }
        return fieldValue.toLowerCase().contains(condition.getPattern().toLowerCase());
    }

    /**
     * Evaluates a frequency threshold condition by counting matching logs
     * within the configured time window.
     *
     * @param condition the frequency condition
     * @param teamId    the team scope
     * @return true if the count of matching logs exceeds the threshold
     */
    public boolean evaluateFrequencyThreshold(TrapCondition condition, UUID teamId) {
        if (condition.getThreshold() == null || condition.getWindowSeconds() == null) {
            log.warn("Frequency condition missing threshold or windowSeconds");
            return false;
        }

        Instant windowStart = Instant.now().minusSeconds(condition.getWindowSeconds());
        Instant windowEnd = Instant.now();

        long count;
        if (condition.getServiceName() != null && condition.getLogLevel() != null) {
            count = logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                    teamId, condition.getServiceName(), condition.getLogLevel(),
                    windowStart, windowEnd);
        } else {
            count = logEntryRepository.countByTeamIdAndTimestampBetween(
                    teamId, windowStart, windowEnd);
        }

        return count >= condition.getThreshold();
    }

    /**
     * Evaluates an absence condition by checking if any matching logs exist
     * within the configured time window.
     *
     * @param condition the absence condition (windowSeconds defines expected log interval)
     * @param teamId    the team scope
     * @return true if NO matching logs found in the window (absence detected)
     */
    public boolean evaluateAbsence(TrapCondition condition, UUID teamId) {
        if (condition.getWindowSeconds() == null) {
            log.warn("Absence condition missing windowSeconds");
            return false;
        }

        Instant windowStart = Instant.now().minusSeconds(condition.getWindowSeconds());
        Instant windowEnd = Instant.now();

        long count;
        if (condition.getServiceName() != null && condition.getLogLevel() != null) {
            count = logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                    teamId, condition.getServiceName(), condition.getLogLevel(),
                    windowStart, windowEnd);
        } else {
            count = logEntryRepository.countByTeamIdAndTimestampBetween(
                    teamId, windowStart, windowEnd);
        }

        return count == 0;
    }

    /**
     * Extracts a field value from a LogEntry by field name.
     * Supports both snake_case and camelCase field names.
     *
     * @param entry     the log entry
     * @param fieldName the field to extract
     * @return the field value, or null if the field is unrecognized or null
     */
    String extractFieldValue(LogEntry entry, String fieldName) {
        if (entry == null || fieldName == null) {
            return null;
        }
        return switch (fieldName) {
            case "message" -> entry.getMessage();
            case "logger_name", "loggerName" -> entry.getLoggerName();
            case "thread_name", "threadName" -> entry.getThreadName();
            case "exception_class", "exceptionClass" -> entry.getExceptionClass();
            case "exception_message", "exceptionMessage" -> entry.getExceptionMessage();
            case "stack_trace", "stackTrace" -> entry.getStackTrace();
            case "service_name", "serviceName" -> entry.getServiceName();
            case "host_name", "hostName" -> entry.getHostName();
            case "ip_address", "ipAddress" -> entry.getIpAddress();
            case "correlation_id", "correlationId" -> entry.getCorrelationId();
            case "custom_fields", "customFields" -> entry.getCustomFields();
            case "level" -> entry.getLevel() != null ? entry.getLevel().name() : null;
            default -> {
                log.debug("Unknown field name for extraction: '{}'", fieldName);
                yield null;
            }
        };
    }

    /**
     * Checks if a log entry's level is at or above the required level.
     * LogLevel ordering: TRACE(0) &lt; DEBUG(1) &lt; INFO(2) &lt; WARN(3) &lt; ERROR(4) &lt; FATAL(5).
     *
     * @param entryLevel    the log entry's level
     * @param requiredLevel the minimum required level
     * @return true if entryLevel &gt;= requiredLevel
     */
    boolean isLevelAtOrAbove(LogLevel entryLevel, LogLevel requiredLevel) {
        if (entryLevel == null || requiredLevel == null) {
            return false;
        }
        return entryLevel.ordinal() >= requiredLevel.ordinal();
    }

    /**
     * Gets a compiled pattern from the cache, compiling and caching on first access.
     * Returns null for invalid patterns.
     */
    private Pattern getCompiledPattern(String regex) {
        if (regex == null) {
            return null;
        }
        return patternCache.computeIfAbsent(regex, key -> {
            if (patternCache.size() >= MAX_PATTERN_CACHE_SIZE) {
                patternCache.clear();
                log.info("Cleared regex pattern cache (exceeded {} entries)", MAX_PATTERN_CACHE_SIZE);
            }
            try {
                return Pattern.compile(key);
            } catch (PatternSyntaxException e) {
                log.warn("Invalid regex pattern: '{}' — {}", key, e.getMessage());
                return null;
            }
        });
    }
}
