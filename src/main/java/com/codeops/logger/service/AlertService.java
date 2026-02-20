package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.AlertHistoryMapper;
import com.codeops.logger.dto.mapper.AlertRuleMapper;
import com.codeops.logger.dto.request.CreateAlertRuleRequest;
import com.codeops.logger.dto.request.UpdateAlertRuleRequest;
import com.codeops.logger.dto.request.UpdateAlertStatusRequest;
import com.codeops.logger.dto.response.AlertHistoryResponse;
import com.codeops.logger.dto.response.AlertRuleResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.entity.AlertChannel;
import com.codeops.logger.entity.AlertHistory;
import com.codeops.logger.entity.AlertRule;
import com.codeops.logger.entity.LogTrap;
import com.codeops.logger.entity.enums.AlertSeverity;
import com.codeops.logger.entity.enums.AlertStatus;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.AlertChannelRepository;
import com.codeops.logger.repository.AlertHistoryRepository;
import com.codeops.logger.repository.AlertRuleRepository;
import com.codeops.logger.repository.LogTrapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Manages alert rules (connecting traps to channels) and handles the alert lifecycle:
 * firing, throttling, delivery, acknowledgment, and resolution.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final LogTrapRepository logTrapRepository;
    private final AlertChannelRepository alertChannelRepository;
    private final AlertRuleMapper alertRuleMapper;
    private final AlertHistoryMapper alertHistoryMapper;
    private final AlertChannelService alertChannelService;

    // ==================== Rule CRUD ====================

    /**
     * Creates a new alert rule connecting a trap to a channel.
     *
     * @param request the rule configuration
     * @param teamId  the team scope
     * @return the created rule response
     * @throws NotFoundException   if trap or channel not found
     * @throws ValidationException if severity is invalid
     */
    @Transactional
    public AlertRuleResponse createRule(CreateAlertRuleRequest request, UUID teamId) {
        LogTrap trap = logTrapRepository.findById(request.trapId())
                .orElseThrow(() -> new NotFoundException("Trap not found: " + request.trapId()));

        AlertChannel channel = alertChannelRepository.findById(request.channelId())
                .orElseThrow(() -> new NotFoundException("Channel not found: " + request.channelId()));

        AlertSeverity severity = parseSeverity(request.severity());

        AlertRule rule = alertRuleMapper.toEntity(request);
        rule.setTrap(trap);
        rule.setChannel(channel);
        rule.setSeverity(severity);
        rule.setTeamId(teamId);
        rule.setIsActive(true);
        rule.setThrottleMinutes(
                request.throttleMinutes() != null ? request.throttleMinutes()
                        : AppConstants.DEFAULT_THROTTLE_MINUTES);

        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule '{}' (trap='{}' → channel='{}')",
                saved.getName(), trap.getName(), channel.getName());
        return alertRuleMapper.toResponse(saved);
    }

    /**
     * Returns all rules for a team.
     *
     * @param teamId the team scope
     * @return list of rule responses
     */
    public List<AlertRuleResponse> getRulesByTeam(UUID teamId) {
        List<AlertRule> rules = alertRuleRepository.findByTeamId(teamId);
        return alertRuleMapper.toResponseList(rules);
    }

    /**
     * Returns paginated rules for a team.
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated rule responses
     */
    public PageResponse<AlertRuleResponse> getRulesByTeamPaged(UUID teamId, int page, int size) {
        Page<AlertRule> springPage = alertRuleRepository.findByTeamId(teamId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AlertRuleResponse> content = alertRuleMapper.toResponseList(springPage.getContent());
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
     * Returns rules for a specific trap.
     *
     * @param trapId the trap ID
     * @return list of rule responses
     */
    public List<AlertRuleResponse> getRulesByTrap(UUID trapId) {
        List<AlertRule> rules = alertRuleRepository.findByTrapId(trapId);
        return alertRuleMapper.toResponseList(rules);
    }

    /**
     * Returns a single rule by ID.
     *
     * @param ruleId the rule ID
     * @return the rule response
     * @throws NotFoundException if not found
     */
    public AlertRuleResponse getRule(UUID ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Alert rule not found: " + ruleId));
        return alertRuleMapper.toResponse(rule);
    }

    /**
     * Updates an existing alert rule.
     *
     * @param ruleId  the rule ID
     * @param request the update request
     * @return the updated rule response
     * @throws NotFoundException if not found
     */
    @Transactional
    public AlertRuleResponse updateRule(UUID ruleId, UpdateAlertRuleRequest request) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Alert rule not found: " + ruleId));

        if (request.name() != null) {
            rule.setName(request.name());
        }
        if (request.trapId() != null) {
            LogTrap trap = logTrapRepository.findById(request.trapId())
                    .orElseThrow(() -> new NotFoundException("Trap not found: " + request.trapId()));
            rule.setTrap(trap);
        }
        if (request.channelId() != null) {
            AlertChannel channel = alertChannelRepository.findById(request.channelId())
                    .orElseThrow(() -> new NotFoundException("Channel not found: " + request.channelId()));
            rule.setChannel(channel);
        }
        if (request.severity() != null) {
            rule.setSeverity(parseSeverity(request.severity()));
        }
        if (request.isActive() != null) {
            rule.setIsActive(request.isActive());
        }
        if (request.throttleMinutes() != null) {
            rule.setThrottleMinutes(request.throttleMinutes());
        }

        AlertRule saved = alertRuleRepository.save(rule);
        return alertRuleMapper.toResponse(saved);
    }

    /**
     * Deletes an alert rule.
     *
     * @param ruleId the rule ID
     * @throws NotFoundException if not found
     */
    public void deleteRule(UUID ruleId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Alert rule not found: " + ruleId));
        alertRuleRepository.delete(rule);
        log.info("Deleted alert rule '{}' ({})", rule.getName(), ruleId);
    }

    // ==================== Alert Firing ====================

    /**
     * Fires alerts for a trap that has triggered. Looks up all active rules for the trap,
     * applies throttling, creates alert history records, and dispatches notifications.
     *
     * @param trapId         the trap that fired
     * @param triggerMessage description of what triggered the trap
     */
    @Transactional
    public void fireAlerts(UUID trapId, String triggerMessage) {
        LogTrap trap = logTrapRepository.findById(trapId).orElse(null);
        if (trap == null) {
            log.warn("Attempted to fire alerts for unknown trap: {}", trapId);
            return;
        }

        List<AlertRule> activeRules = alertRuleRepository.findByTrapIdAndIsActiveTrue(trapId);
        int fired = 0;
        int throttled = 0;

        for (AlertRule rule : activeRules) {
            if (isThrottled(rule.getId(), rule.getThrottleMinutes())) {
                log.debug("Alert throttled for rule '{}' ({})", rule.getName(), rule.getId());
                throttled++;
                continue;
            }

            AlertHistory history = new AlertHistory();
            history.setRule(rule);
            history.setTrap(trap);
            history.setChannel(rule.getChannel());
            history.setSeverity(rule.getSeverity());
            history.setStatus(AlertStatus.FIRED);
            history.setMessage(triggerMessage);
            history.setTeamId(rule.getTeamId());
            alertHistoryRepository.save(history);

            alertChannelService.deliverNotification(
                    rule.getChannel(), triggerMessage, rule.getSeverity(), trap.getName());
            fired++;
        }

        log.info("Fired {} alerts for trap '{}' ({} throttled)", fired, trap.getName(), throttled);
    }

    /**
     * Checks whether an alert rule is currently throttled.
     *
     * @param ruleId          the rule to check
     * @param throttleMinutes the throttle window
     * @return true if the rule is throttled (should NOT fire)
     */
    boolean isThrottled(UUID ruleId, int throttleMinutes) {
        Instant since = Instant.now().minus(throttleMinutes, ChronoUnit.MINUTES);
        return alertHistoryRepository.existsByRuleIdAndCreatedAtAfter(ruleId, since);
    }

    // ==================== Alert Lifecycle ====================

    /**
     * Acknowledges a fired alert.
     *
     * @param alertId the alert to acknowledge
     * @param userId  the user acknowledging
     * @return the updated alert response
     * @throws ValidationException if alert is already resolved
     */
    @Transactional
    public AlertHistoryResponse acknowledgeAlert(UUID alertId, UUID userId) {
        AlertHistory alert = alertHistoryRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert not found: " + alertId));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ValidationException("Cannot acknowledge a resolved alert");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedAt(Instant.now());

        AlertHistory saved = alertHistoryRepository.save(alert);
        log.info("Alert {} acknowledged by user {}", alertId, userId);
        return alertHistoryMapper.toResponse(saved);
    }

    /**
     * Resolves an alert (marks it as handled).
     *
     * @param alertId the alert to resolve
     * @param userId  the user resolving
     * @return the updated alert response
     */
    @Transactional
    public AlertHistoryResponse resolveAlert(UUID alertId, UUID userId) {
        AlertHistory alert = alertHistoryRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert not found: " + alertId));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ValidationException("Alert is already resolved");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(userId);
        alert.setResolvedAt(Instant.now());

        if (alert.getAcknowledgedBy() == null) {
            alert.setAcknowledgedBy(userId);
            alert.setAcknowledgedAt(Instant.now());
        }

        AlertHistory saved = alertHistoryRepository.save(alert);
        log.info("Alert {} resolved by user {}", alertId, userId);
        return alertHistoryMapper.toResponse(saved);
    }

    /**
     * Updates an alert's status (generic handler for status transitions).
     *
     * @param alertId the alert
     * @param request the new status
     * @param userId  the acting user
     * @return the updated alert response
     */
    public AlertHistoryResponse updateAlertStatus(UUID alertId, UpdateAlertStatusRequest request,
                                                   UUID userId) {
        AlertStatus status;
        try {
            status = AlertStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid alert status: " + request.status());
        }

        return switch (status) {
            case ACKNOWLEDGED -> acknowledgeAlert(alertId, userId);
            case RESOLVED -> resolveAlert(alertId, userId);
            case FIRED -> throw new ValidationException("Cannot set status back to FIRED");
        };
    }

    // ==================== Alert History Queries ====================

    /**
     * Returns paginated alert history for a team (most recent first).
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated alert history
     */
    public PageResponse<AlertHistoryResponse> getAlertHistory(UUID teamId, int page, int size) {
        Page<AlertHistory> springPage = alertHistoryRepository.findByTeamIdOrderByCreatedAtDesc(
                teamId, PageRequest.of(page, size));
        List<AlertHistoryResponse> content = alertHistoryMapper.toResponseList(springPage.getContent());
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
     * Returns paginated alert history filtered by status.
     *
     * @param teamId the team scope
     * @param status the alert status filter
     * @param page   page number
     * @param size   page size
     * @return paginated alert history
     */
    public PageResponse<AlertHistoryResponse> getAlertHistoryByStatus(UUID teamId,
                                                                       AlertStatus status, int page, int size) {
        Page<AlertHistory> springPage = alertHistoryRepository.findByTeamIdAndStatus(
                teamId, status, PageRequest.of(page, size));
        List<AlertHistoryResponse> content = alertHistoryMapper.toResponseList(springPage.getContent());
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
     * Returns paginated alert history filtered by severity.
     *
     * @param teamId   the team scope
     * @param severity the severity filter
     * @param page     page number
     * @param size     page size
     * @return paginated alert history
     */
    public PageResponse<AlertHistoryResponse> getAlertHistoryBySeverity(UUID teamId,
                                                                         AlertSeverity severity, int page, int size) {
        Page<AlertHistory> springPage = alertHistoryRepository.findByTeamIdAndSeverity(
                teamId, severity, PageRequest.of(page, size));
        List<AlertHistoryResponse> content = alertHistoryMapper.toResponseList(springPage.getContent());
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
     * Returns alert history for a specific rule.
     *
     * @param ruleId the rule ID
     * @param page   page number
     * @param size   page size
     * @return paginated alert history
     */
    public PageResponse<AlertHistoryResponse> getAlertHistoryByRule(UUID ruleId, int page, int size) {
        Page<AlertHistory> springPage = alertHistoryRepository.findByRuleId(
                ruleId, PageRequest.of(page, size));
        List<AlertHistoryResponse> content = alertHistoryMapper.toResponseList(springPage.getContent());
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
     * Returns count of active (non-resolved) alerts by severity for a team.
     *
     * @param teamId the team scope
     * @return map of severity to count
     */
    public Map<String, Long> getActiveAlertCounts(UUID teamId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (AlertSeverity severity : AlertSeverity.values()) {
            long firedCount = alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                    teamId, severity, AlertStatus.FIRED);
            long ackCount = alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                    teamId, severity, AlertStatus.ACKNOWLEDGED);
            counts.put(severity.name(), firedCount + ackCount);
        }
        return counts;
    }

    /**
     * Parses a severity string to the AlertSeverity enum.
     */
    private AlertSeverity parseSeverity(String severity) {
        try {
            return AlertSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid alert severity: " + severity);
        }
    }
}
