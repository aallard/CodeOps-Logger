package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.AlertChannelType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Notification channel configuration for alert delivery.
 * Supports email, webhook, Microsoft Teams, and Slack.
 */
@Entity
@Table(name = "alert_channels", indexes = {
        @Index(name = "idx_alert_channel_team_id", columnList = "team_id"),
        @Index(name = "idx_alert_channel_type", columnList = "channel_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertChannel extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private AlertChannelType channelType;

    /**
     * JSON configuration specific to channel type:
     * <ul>
     *     <li>EMAIL: {"recipients": ["a@b.com"], "subject_prefix": "[Logger]"}</li>
     *     <li>WEBHOOK: {"url": "https://...", "headers": {"X-Token": "..."}, "method": "POST"}</li>
     *     <li>TEAMS: {"webhook_url": "https://..."}</li>
     *     <li>SLACK: {"webhook_url": "https://hooks.slack.com/..."}</li>
     * </ul>
     */
    @Column(name = "configuration", nullable = false, columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
