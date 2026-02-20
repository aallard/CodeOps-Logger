package com.codeops.logger.entity.enums;

/**
 * Supported notification channel types for alert delivery.
 */
public enum AlertChannelType {
    /** Email notification via CodeOps-Server SES integration. */
    EMAIL,
    /** Generic HTTP webhook POST. */
    WEBHOOK,
    /** Microsoft Teams incoming webhook (MessageCard format). */
    TEAMS,
    /** Slack incoming webhook. */
    SLACK
}
