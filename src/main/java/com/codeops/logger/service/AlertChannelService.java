package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.AlertChannelMapper;
import com.codeops.logger.dto.request.CreateAlertChannelRequest;
import com.codeops.logger.dto.request.UpdateAlertChannelRequest;
import com.codeops.logger.dto.response.AlertChannelResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.entity.AlertChannel;
import com.codeops.logger.entity.enums.AlertChannelType;
import com.codeops.logger.entity.enums.AlertSeverity;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.AlertChannelRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Manages alert notification channel lifecycle (CRUD) and handles
 * asynchronous notification delivery to email, webhook, Microsoft Teams, and Slack.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertChannelService {

    private final AlertChannelRepository alertChannelRepository;
    private final AlertChannelMapper alertChannelMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost", "127.0.0.1", "::1", "0.0.0.0"
    );

    private static final List<String> BLOCKED_PREFIXES = List.of(
            "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
            "172.25.", "172.26.", "172.27.", "172.28.", "172.29.",
            "172.30.", "172.31.", "192.168.", "169.254."
    );

    // ==================== CRUD ====================

    /**
     * Creates a new alert channel.
     *
     * @param request the channel configuration
     * @param teamId  the team scope
     * @param userId  the creating user
     * @return the created channel response
     * @throws ValidationException if team has reached MAX_ALERT_CHANNELS or config is invalid
     */
    public AlertChannelResponse createChannel(CreateAlertChannelRequest request,
                                               UUID teamId, UUID userId) {
        long currentCount = alertChannelRepository.countByTeamId(teamId);
        if (currentCount >= AppConstants.MAX_ALERT_CHANNELS) {
            throw new ValidationException(
                    "Team has reached maximum channel limit (" + AppConstants.MAX_ALERT_CHANNELS + ")");
        }

        AlertChannelType channelType = parseChannelType(request.channelType());
        validateConfiguration(channelType, request.configuration());

        AlertChannel entity = alertChannelMapper.toEntity(request);
        entity.setChannelType(channelType);
        entity.setTeamId(teamId);
        entity.setCreatedBy(userId);
        entity.setIsActive(true);

        AlertChannel saved = alertChannelRepository.save(entity);
        log.info("Created alert channel '{}' (type={}) for team {}",
                saved.getName(), channelType, teamId);
        return alertChannelMapper.toResponse(saved);
    }

    /**
     * Returns all channels for a team.
     *
     * @param teamId the team scope
     * @return list of channel responses
     */
    public List<AlertChannelResponse> getChannelsByTeam(UUID teamId) {
        List<AlertChannel> channels = alertChannelRepository.findByTeamId(teamId);
        return alertChannelMapper.toResponseList(channels);
    }

    /**
     * Returns paginated channels for a team.
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated channel responses
     */
    public PageResponse<AlertChannelResponse> getChannelsByTeamPaged(UUID teamId, int page, int size) {
        Page<AlertChannel> springPage = alertChannelRepository.findByTeamId(teamId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AlertChannelResponse> content = alertChannelMapper.toResponseList(springPage.getContent());
        return new PageResponse<>(
                content,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }

    /**
     * Returns a single channel by ID.
     *
     * @param channelId the channel ID
     * @return the channel response
     * @throws NotFoundException if not found
     */
    public AlertChannelResponse getChannel(UUID channelId) {
        AlertChannel channel = alertChannelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Alert channel not found: " + channelId));
        return alertChannelMapper.toResponse(channel);
    }

    /**
     * Updates an existing alert channel.
     *
     * @param channelId the channel ID
     * @param request   the update request
     * @return the updated channel response
     * @throws NotFoundException if not found
     */
    public AlertChannelResponse updateChannel(UUID channelId, UpdateAlertChannelRequest request) {
        AlertChannel channel = alertChannelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Alert channel not found: " + channelId));

        if (request.name() != null) {
            channel.setName(request.name());
        }
        if (request.configuration() != null) {
            validateConfiguration(channel.getChannelType(), request.configuration());
            channel.setConfiguration(request.configuration());
        }
        if (request.isActive() != null) {
            channel.setIsActive(request.isActive());
        }

        AlertChannel saved = alertChannelRepository.save(channel);
        return alertChannelMapper.toResponse(saved);
    }

    /**
     * Deletes an alert channel.
     *
     * @param channelId the channel ID
     * @throws NotFoundException if not found
     */
    public void deleteChannel(UUID channelId) {
        AlertChannel channel = alertChannelRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Alert channel not found: " + channelId));
        alertChannelRepository.delete(channel);
        log.info("Deleted alert channel '{}' ({})", channel.getName(), channelId);
    }

    // ==================== Notification Delivery ====================

    /**
     * Delivers a notification to a channel asynchronously.
     * This method is called by AlertService when an alert fires.
     *
     * @param channel      the target channel
     * @param alertMessage the alert message to deliver
     * @param severity     the alert severity
     * @param trapName     the name of the trap that fired
     */
    @Async
    public void deliverNotification(AlertChannel channel, String alertMessage,
                                     AlertSeverity severity, String trapName) {
        try {
            switch (channel.getChannelType()) {
                case EMAIL -> deliverEmail(channel, alertMessage, severity, trapName);
                case WEBHOOK -> deliverWebhook(channel, alertMessage, severity, trapName);
                case TEAMS -> deliverTeams(channel, alertMessage, severity, trapName);
                case SLACK -> deliverSlack(channel, alertMessage, severity, trapName);
            }
            log.info("Alert delivered to channel '{}' (type={})",
                    channel.getName(), channel.getChannelType());
        } catch (Exception e) {
            log.error("Failed to deliver alert to channel '{}' (type={}): {}",
                    channel.getName(), channel.getChannelType(), e.getMessage(), e);
        }
    }

    /**
     * Delivers alert via email. In development, logs the content instead of sending.
     */
    void deliverEmail(AlertChannel channel, String alertMessage,
                      AlertSeverity severity, String trapName) {
        try {
            JsonNode config = objectMapper.readTree(channel.getConfiguration());
            JsonNode recipients = config.get("recipients");
            String subjectPrefix = config.has("subject_prefix")
                    ? config.get("subject_prefix").asText() : "[CodeOps Logger]";

            if (recipients != null && recipients.isArray()) {
                for (JsonNode recipient : recipients) {
                    log.info("EMAIL ALERT to {}: {} [{}] {} - {}",
                            recipient.asText(), subjectPrefix, severity, trapName, alertMessage);
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse email channel configuration: {}", e.getMessage());
        }
    }

    /**
     * Delivers alert via generic HTTP webhook POST.
     */
    void deliverWebhook(AlertChannel channel, String alertMessage,
                        AlertSeverity severity, String trapName) {
        try {
            JsonNode config = objectMapper.readTree(channel.getConfiguration());
            String url = config.get("url").asText();
            validateWebhookUrl(url);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("severity", severity.name());
            payload.put("trapName", trapName);
            payload.put("message", alertMessage);
            payload.put("timestamp", Instant.now().toString());
            payload.put("service", "codeops-logger");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (config.has("headers") && config.get("headers").isObject()) {
                config.get("headers").fields().forEachRemaining(entry ->
                        headers.set(entry.getKey(), entry.getValue().asText()));
            }

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(payload), headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Webhook delivery failed: {}", e.getMessage());
        }
    }

    /**
     * Delivers alert via Microsoft Teams incoming webhook (MessageCard format).
     */
    void deliverTeams(AlertChannel channel, String alertMessage,
                      AlertSeverity severity, String trapName) {
        try {
            JsonNode config = objectMapper.readTree(channel.getConfiguration());
            String webhookUrl = config.get("webhook_url").asText();
            validateWebhookUrl(webhookUrl);

            String themeColor = switch (severity) {
                case INFO -> "0076D7";
                case WARNING -> "FFA500";
                case CRITICAL -> "FF0000";
            };

            Map<String, Object> card = new LinkedHashMap<>();
            card.put("@type", "MessageCard");
            card.put("@context", "http://schema.org/extensions");
            card.put("themeColor", themeColor);
            card.put("summary", "CodeOps Logger Alert");

            Map<String, Object> section = new LinkedHashMap<>();
            section.put("activityTitle", "[" + severity + "] " + trapName);
            section.put("facts", List.of(
                    Map.of("name", "Severity", "value", severity.name()),
                    Map.of("name", "Trap", "value", trapName),
                    Map.of("name", "Message", "value", alertMessage),
                    Map.of("name", "Time", "value", Instant.now().toString())
            ));
            section.put("markdown", true);
            card.put("sections", List.of(section));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(card), headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Teams delivery failed: {}", e.getMessage());
        }
    }

    /**
     * Delivers alert via Slack incoming webhook.
     */
    void deliverSlack(AlertChannel channel, String alertMessage,
                      AlertSeverity severity, String trapName) {
        try {
            JsonNode config = objectMapper.readTree(channel.getConfiguration());
            String webhookUrl = config.get("webhook_url").asText();
            validateWebhookUrl(webhookUrl);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("text", "CodeOps Logger Alert: [" + severity + "] " + trapName);

            Map<String, Object> header = Map.of(
                    "type", "header",
                    "text", Map.of("type", "plain_text", "text", "Logger Alert"));
            Map<String, Object> section = Map.of(
                    "type", "section",
                    "fields", List.of(
                            Map.of("type", "mrkdwn", "text", "*Severity:* " + severity),
                            Map.of("type", "mrkdwn", "text", "*Trap:* " + trapName),
                            Map.of("type", "mrkdwn", "text", "*Message:* " + alertMessage)
                    ));
            payload.put("blocks", List.of(header, section));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(payload), headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Slack delivery failed: {}", e.getMessage());
        }
    }

    // ==================== Validation ====================

    /**
     * Validates a webhook URL for SSRF protection.
     * Requires HTTPS and rejects internal/loopback addresses.
     *
     * @param url the URL to validate
     * @throws ValidationException if the URL is invalid or targets an internal address
     */
    void validateWebhookUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ValidationException("Webhook URL is required");
        }
        if (!url.startsWith("https://")) {
            throw new ValidationException("Webhook URL must use HTTPS");
        }
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                throw new ValidationException("Invalid webhook URL: no host");
            }
            String hostLower = host.toLowerCase();
            if (BLOCKED_HOSTS.contains(hostLower)) {
                throw new ValidationException("Webhook URL must not target internal addresses");
            }
            for (String prefix : BLOCKED_PREFIXES) {
                if (hostLower.startsWith(prefix)) {
                    throw new ValidationException("Webhook URL must not target internal addresses");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid webhook URL: " + e.getMessage());
        }
    }

    /**
     * Validates channel configuration JSON based on channel type.
     *
     * @param channelType   the type of channel
     * @param configuration the JSON configuration string
     * @throws ValidationException if the configuration is invalid for the type
     */
    void validateConfiguration(AlertChannelType channelType, String configuration) {
        JsonNode config;
        try {
            config = objectMapper.readTree(configuration);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Channel configuration must be valid JSON");
        }

        switch (channelType) {
            case EMAIL -> {
                if (!config.has("recipients") || !config.get("recipients").isArray()
                        || config.get("recipients").isEmpty()) {
                    throw new ValidationException(
                            "Email channel requires 'recipients' array with at least 1 email");
                }
            }
            case WEBHOOK -> {
                if (!config.has("url") || config.get("url").asText().isBlank()) {
                    throw new ValidationException("Webhook channel requires 'url'");
                }
                validateWebhookUrl(config.get("url").asText());
            }
            case TEAMS -> {
                if (!config.has("webhook_url") || config.get("webhook_url").asText().isBlank()) {
                    throw new ValidationException("Teams channel requires 'webhook_url'");
                }
                validateWebhookUrl(config.get("webhook_url").asText());
            }
            case SLACK -> {
                if (!config.has("webhook_url") || config.get("webhook_url").asText().isBlank()) {
                    throw new ValidationException("Slack channel requires 'webhook_url'");
                }
                String slackUrl = config.get("webhook_url").asText();
                if (!slackUrl.startsWith("https://hooks.slack.com/")) {
                    throw new ValidationException(
                            "Slack webhook URL must start with https://hooks.slack.com/");
                }
            }
        }
    }

    /**
     * Parses a channel type string to the AlertChannelType enum.
     */
    private AlertChannelType parseChannelType(String channelType) {
        try {
            return AlertChannelType.valueOf(channelType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid channel type: " + channelType);
        }
    }
}
