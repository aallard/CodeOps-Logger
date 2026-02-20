package com.codeops.logger.entity.enums;

/**
 * Lifecycle states of a fired alert.
 */
public enum AlertStatus {
    /** Alert has been fired and is awaiting attention. */
    FIRED,
    /** Alert has been acknowledged by a team member. */
    ACKNOWLEDGED,
    /** Alert has been resolved. */
    RESOLVED
}
