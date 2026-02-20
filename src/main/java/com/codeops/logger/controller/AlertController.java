package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateAlertChannelRequest;
import com.codeops.logger.dto.request.CreateAlertRuleRequest;
import com.codeops.logger.dto.request.UpdateAlertChannelRequest;
import com.codeops.logger.dto.request.UpdateAlertRuleRequest;
import com.codeops.logger.dto.request.UpdateAlertStatusRequest;
import com.codeops.logger.dto.response.AlertChannelResponse;
import com.codeops.logger.dto.response.AlertHistoryResponse;
import com.codeops.logger.dto.response.AlertRuleResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.entity.enums.AlertSeverity;
import com.codeops.logger.entity.enums.AlertStatus;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.service.AlertChannelService;
import com.codeops.logger.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing alert channels, rules, and alert history.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Alerts", description = "Manage alert channels, rules, and history")
public class AlertController extends BaseController {

    private final AlertService alertService;
    private final AlertChannelService alertChannelService;

    // ==================== Channels ====================

    /**
     * Creates a new alert channel.
     *
     * @param request     the channel configuration
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created channel
     */
    @PostMapping("/channels")
    @Operation(summary = "Create an alert channel")
    public ResponseEntity<AlertChannelResponse> createChannel(@Valid @RequestBody CreateAlertChannelRequest request,
                                                               HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                alertChannelService.createChannel(request, teamId, userId));
    }

    /**
     * Lists all alert channels for the team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return list of channels
     */
    @GetMapping("/channels")
    @Operation(summary = "List alert channels for the team")
    public ResponseEntity<List<AlertChannelResponse>> getChannels(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(alertChannelService.getChannelsByTeam(teamId));
    }

    /**
     * Lists alert channels with pagination.
     *
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated channels
     */
    @GetMapping("/channels/paged")
    @Operation(summary = "List alert channels with pagination")
    public ResponseEntity<PageResponse<AlertChannelResponse>> getChannelsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(alertChannelService.getChannelsByTeamPaged(teamId, page, size));
    }

    /**
     * Gets an alert channel by ID.
     *
     * @param channelId the channel ID
     * @return the channel
     */
    @GetMapping("/channels/{channelId}")
    @Operation(summary = "Get an alert channel by ID")
    public ResponseEntity<AlertChannelResponse> getChannel(@PathVariable UUID channelId) {
        return ResponseEntity.ok(alertChannelService.getChannel(channelId));
    }

    /**
     * Updates an alert channel.
     *
     * @param channelId the channel ID
     * @param request   the update data
     * @return the updated channel
     */
    @PutMapping("/channels/{channelId}")
    @Operation(summary = "Update an alert channel")
    public ResponseEntity<AlertChannelResponse> updateChannel(@PathVariable UUID channelId,
                                                               @Valid @RequestBody UpdateAlertChannelRequest request) {
        return ResponseEntity.ok(alertChannelService.updateChannel(channelId, request));
    }

    /**
     * Deletes an alert channel.
     *
     * @param channelId the channel ID
     * @return 204 No Content
     */
    @DeleteMapping("/channels/{channelId}")
    @Operation(summary = "Delete an alert channel")
    public ResponseEntity<Void> deleteChannel(@PathVariable UUID channelId) {
        alertChannelService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Rules ====================

    /**
     * Creates a new alert rule connecting a trap to a channel.
     *
     * @param request     the rule configuration
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created rule
     */
    @PostMapping("/rules")
    @Operation(summary = "Create an alert rule")
    public ResponseEntity<AlertRuleResponse> createRule(@Valid @RequestBody CreateAlertRuleRequest request,
                                                         HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.createRule(request, teamId));
    }

    /**
     * Lists all alert rules for the team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return list of rules
     */
    @GetMapping("/rules")
    @Operation(summary = "List alert rules for the team")
    public ResponseEntity<List<AlertRuleResponse>> getRules(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(alertService.getRulesByTeam(teamId));
    }

    /**
     * Lists alert rules with pagination.
     *
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated rules
     */
    @GetMapping("/rules/paged")
    @Operation(summary = "List alert rules with pagination")
    public ResponseEntity<PageResponse<AlertRuleResponse>> getRulesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(alertService.getRulesByTeamPaged(teamId, page, size));
    }

    /**
     * Gets an alert rule by ID.
     *
     * @param ruleId the rule ID
     * @return the rule
     */
    @GetMapping("/rules/{ruleId}")
    @Operation(summary = "Get an alert rule by ID")
    public ResponseEntity<AlertRuleResponse> getRule(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(alertService.getRule(ruleId));
    }

    /**
     * Updates an alert rule.
     *
     * @param ruleId  the rule ID
     * @param request the update data
     * @return the updated rule
     */
    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "Update an alert rule")
    public ResponseEntity<AlertRuleResponse> updateRule(@PathVariable UUID ruleId,
                                                         @Valid @RequestBody UpdateAlertRuleRequest request) {
        return ResponseEntity.ok(alertService.updateRule(ruleId, request));
    }

    /**
     * Deletes an alert rule.
     *
     * @param ruleId the rule ID
     * @return 204 No Content
     */
    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Delete an alert rule")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        alertService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Alert History & Lifecycle ====================

    /**
     * Gets paginated alert history for the team.
     *
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated alert history
     */
    @GetMapping("/history")
    @Operation(summary = "Get alert history for the team")
    public ResponseEntity<PageResponse<AlertHistoryResponse>> getAlertHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(alertService.getAlertHistory(teamId, page, size));
    }

    /**
     * Gets alert history filtered by status.
     *
     * @param status      the status filter
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated filtered alert history
     */
    @GetMapping("/history/status/{status}")
    @Operation(summary = "Get alert history filtered by status")
    public ResponseEntity<PageResponse<AlertHistoryResponse>> getAlertHistoryByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        AlertStatus alertStatus = parseAlertStatus(status);
        return ResponseEntity.ok(alertService.getAlertHistoryByStatus(teamId, alertStatus, page, size));
    }

    /**
     * Gets alert history filtered by severity.
     *
     * @param severity    the severity filter
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated filtered alert history
     */
    @GetMapping("/history/severity/{severity}")
    @Operation(summary = "Get alert history filtered by severity")
    public ResponseEntity<PageResponse<AlertHistoryResponse>> getAlertHistoryBySeverity(
            @PathVariable String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        AlertSeverity alertSeverity = parseAlertSeverity(severity);
        return ResponseEntity.ok(alertService.getAlertHistoryBySeverity(teamId, alertSeverity, page, size));
    }

    /**
     * Updates an alert's status (acknowledge or resolve).
     *
     * @param alertId the alert ID
     * @param request the new status
     * @return the updated alert
     */
    @PutMapping("/history/{alertId}/status")
    @Operation(summary = "Update an alert's status (acknowledge or resolve)")
    public ResponseEntity<AlertHistoryResponse> updateAlertStatus(@PathVariable UUID alertId,
                                                                    @Valid @RequestBody UpdateAlertStatusRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(alertService.updateAlertStatus(alertId, request, userId));
    }

    /**
     * Gets counts of active (non-resolved) alerts by severity.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return map of severity to count
     */
    @GetMapping("/active-counts")
    @Operation(summary = "Get counts of active alerts by severity")
    public ResponseEntity<Map<String, Long>> getActiveAlertCounts(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(alertService.getActiveAlertCounts(teamId));
    }

    /**
     * Parses an alert status string to the AlertStatus enum.
     *
     * @param status the status string
     * @return the parsed AlertStatus
     * @throws ValidationException if the status is invalid
     */
    private AlertStatus parseAlertStatus(String status) {
        try {
            return AlertStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid alert status: " + status);
        }
    }

    /**
     * Parses an alert severity string to the AlertSeverity enum.
     *
     * @param severity the severity string
     * @return the parsed AlertSeverity
     * @throws ValidationException if the severity is invalid
     */
    private AlertSeverity parseAlertSeverity(String severity) {
        try {
            return AlertSeverity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid alert severity: " + severity);
        }
    }
}
