package com.codeops.logger.entity.enums;

/**
 * Classification of log trap trigger mechanisms.
 */
public enum TrapType {
    /** Triggers when log entries match a pattern (regex or keyword). */
    PATTERN,
    /** Triggers when log frequency exceeds a threshold within a time window. */
    FREQUENCY,
    /** Triggers when expected logs are absent for a configured duration. */
    ABSENCE
}
