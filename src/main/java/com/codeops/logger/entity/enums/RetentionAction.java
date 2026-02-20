package com.codeops.logger.entity.enums;

/**
 * Actions to perform when log retention policy triggers.
 */
public enum RetentionAction {
    /** Permanently delete expired logs. */
    PURGE,
    /** Archive expired logs to cold storage (S3) before deletion. */
    ARCHIVE
}
