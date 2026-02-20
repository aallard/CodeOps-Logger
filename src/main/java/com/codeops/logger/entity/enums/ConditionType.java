package com.codeops.logger.entity.enums;

/**
 * Specific condition evaluation strategies within a trap.
 */
public enum ConditionType {
    /** Match against a regular expression pattern. */
    REGEX,
    /** Match against a keyword or phrase. */
    KEYWORD,
    /** Trigger when count exceeds threshold in time window. */
    FREQUENCY_THRESHOLD,
    /** Trigger when no matching logs appear in time window. */
    ABSENCE
}
