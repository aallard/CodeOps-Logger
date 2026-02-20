package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.AlertHistoryMapper;
import com.codeops.logger.dto.mapper.AlertRuleMapper;
import com.codeops.logger.dto.request.CreateAlertRuleRequest;
import com.codeops.logger.dto.response.AlertHistoryResponse;
import com.codeops.logger.dto.response.AlertRuleResponse;
import com.codeops.logger.entity.AlertChannel;
import com.codeops.logger.entity.AlertHistory;
import com.codeops.logger.entity.AlertRule;
import com.codeops.logger.entity.LogTrap;
import com.codeops.logger.entity.enums.AlertChannelType;
import com.codeops.logger.entity.enums.AlertSeverity;
import com.codeops.logger.entity.enums.AlertStatus;
import com.codeops.logger.entity.enums.TrapType;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.AlertChannelRepository;
import com.codeops.logger.repository.AlertHistoryRepository;
import com.codeops.logger.repository.AlertRuleRepository;
import com.codeops.logger.repository.LogTrapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AlertService}.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private LogTrapRepository logTrapRepository;

    @Mock
    private AlertChannelRepository alertChannelRepository;

    @Mock
    private AlertRuleMapper alertRuleMapper;

    @Mock
    private AlertHistoryMapper alertHistoryMapper;

    @Mock
    private AlertChannelService alertChannelService;

    @InjectMocks
    private AlertService alertService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private LogTrap createTrap(String name) {
        LogTrap trap = new LogTrap();
        trap.setId(UUID.randomUUID());
        trap.setName(name);
        trap.setTrapType(TrapType.PATTERN);
        trap.setTeamId(TEAM_ID);
        trap.setIsActive(true);
        trap.setTriggerCount(0L);
        return trap;
    }

    private AlertChannel createChannel(String name) {
        AlertChannel channel = new AlertChannel();
        channel.setId(UUID.randomUUID());
        channel.setName(name);
        channel.setChannelType(AlertChannelType.WEBHOOK);
        channel.setConfiguration("{\"url\":\"https://example.com/hook\"}");
        channel.setTeamId(TEAM_ID);
        channel.setIsActive(true);
        return channel;
    }

    private AlertRule createRule(String name, LogTrap trap, AlertChannel channel) {
        AlertRule rule = new AlertRule();
        rule.setId(UUID.randomUUID());
        rule.setName(name);
        rule.setTrap(trap);
        rule.setChannel(channel);
        rule.setSeverity(AlertSeverity.CRITICAL);
        rule.setIsActive(true);
        rule.setThrottleMinutes(AppConstants.DEFAULT_THROTTLE_MINUTES);
        rule.setTeamId(TEAM_ID);
        return rule;
    }

    // ==================== Rule CRUD Tests ====================

    @Test
    void testCreateRule_valid_succeeds() {
        LogTrap trap = createTrap("Test Trap");
        AlertChannel channel = createChannel("Test Channel");
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(
                "Test Rule", trap.getId(), channel.getId(), "CRITICAL", null);

        AlertRule entity = createRule("Test Rule", trap, channel);
        AlertRuleResponse response = new AlertRuleResponse(
                entity.getId(), "Test Rule", trap.getId(), "Test Trap",
                channel.getId(), "Test Channel", "CRITICAL", true,
                AppConstants.DEFAULT_THROTTLE_MINUTES, TEAM_ID, Instant.now(), Instant.now());

        when(logTrapRepository.findById(trap.getId())).thenReturn(Optional.of(trap));
        when(alertChannelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(alertRuleMapper.toEntity(request)).thenReturn(entity);
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(entity);
        when(alertRuleMapper.toResponse(entity)).thenReturn(response);

        AlertRuleResponse result = alertService.createRule(request, TEAM_ID);

        assertThat(result.name()).isEqualTo("Test Rule");
        assertThat(result.severity()).isEqualTo("CRITICAL");
        verify(alertRuleRepository).save(any(AlertRule.class));
    }

    @Test
    void testCreateRule_trapNotFound_throwsNotFound() {
        UUID fakeTrapId = UUID.randomUUID();
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(
                "Rule", fakeTrapId, UUID.randomUUID(), "CRITICAL", null);

        when(logTrapRepository.findById(fakeTrapId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.createRule(request, TEAM_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Trap not found");
    }

    @Test
    void testCreateRule_channelNotFound_throwsNotFound() {
        LogTrap trap = createTrap("Trap");
        UUID fakeChannelId = UUID.randomUUID();
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(
                "Rule", trap.getId(), fakeChannelId, "CRITICAL", null);

        when(logTrapRepository.findById(trap.getId())).thenReturn(Optional.of(trap));
        when(alertChannelRepository.findById(fakeChannelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.createRule(request, TEAM_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Channel not found");
    }

    @Test
    void testCreateRule_defaultThrottle() {
        LogTrap trap = createTrap("Trap");
        AlertChannel channel = createChannel("Channel");
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(
                "Rule", trap.getId(), channel.getId(), "WARNING", null);

        AlertRule entity = createRule("Rule", trap, channel);

        when(logTrapRepository.findById(trap.getId())).thenReturn(Optional.of(trap));
        when(alertChannelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(alertRuleMapper.toEntity(request)).thenReturn(entity);
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(entity);
        when(alertRuleMapper.toResponse(entity)).thenReturn(mock(AlertRuleResponse.class));

        alertService.createRule(request, TEAM_ID);

        ArgumentCaptor<AlertRule> captor = ArgumentCaptor.forClass(AlertRule.class);
        verify(alertRuleRepository).save(captor.capture());
        assertThat(captor.getValue().getThrottleMinutes()).isEqualTo(AppConstants.DEFAULT_THROTTLE_MINUTES);
    }

    @Test
    void testGetRulesByTeam_returnsList() {
        LogTrap trap = createTrap("Trap");
        AlertChannel channel = createChannel("Channel");
        AlertRule r1 = createRule("Rule1", trap, channel);
        AlertRule r2 = createRule("Rule2", trap, channel);

        when(alertRuleRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(r1, r2));
        when(alertRuleMapper.toResponseList(any())).thenReturn(List.of(
                mock(AlertRuleResponse.class), mock(AlertRuleResponse.class)));

        List<AlertRuleResponse> result = alertService.getRulesByTeam(TEAM_ID);

        assertThat(result).hasSize(2);
    }

    // ==================== Alert Firing Tests ====================

    @Test
    void testFireAlerts_activeRules_fires() {
        LogTrap trap = createTrap("Error Trap");
        AlertChannel channel = createChannel("Webhook");
        AlertRule rule = createRule("Fire Rule", trap, channel);

        when(logTrapRepository.findById(trap.getId())).thenReturn(Optional.of(trap));
        when(alertRuleRepository.findByTrapIdAndIsActiveTrue(trap.getId())).thenReturn(List.of(rule));
        when(alertHistoryRepository.existsByRuleIdAndCreatedAtAfter(eq(rule.getId()), any(Instant.class)))
                .thenReturn(false);
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(new AlertHistory());

        alertService.fireAlerts(trap.getId(), "Error threshold exceeded");

        verify(alertHistoryRepository).save(any(AlertHistory.class));
        verify(alertChannelService).deliverNotification(
                eq(channel), eq("Error threshold exceeded"), eq(AlertSeverity.CRITICAL), eq("Error Trap"));
    }

    @Test
    void testFireAlerts_throttledRule_skips() {
        LogTrap trap = createTrap("Throttled Trap");
        AlertChannel channel = createChannel("Channel");
        AlertRule rule = createRule("Throttled Rule", trap, channel);

        when(logTrapRepository.findById(trap.getId())).thenReturn(Optional.of(trap));
        when(alertRuleRepository.findByTrapIdAndIsActiveTrue(trap.getId())).thenReturn(List.of(rule));
        when(alertHistoryRepository.existsByRuleIdAndCreatedAtAfter(eq(rule.getId()), any(Instant.class)))
                .thenReturn(true);

        alertService.fireAlerts(trap.getId(), "Throttled message");

        verify(alertHistoryRepository, never()).save(any(AlertHistory.class));
        verify(alertChannelService, never()).deliverNotification(any(), any(), any(), any());
    }

    @Test
    void testFireAlerts_noActiveRules_noAlerts() {
        LogTrap trap = createTrap("No Rules Trap");

        when(logTrapRepository.findById(trap.getId())).thenReturn(Optional.of(trap));
        when(alertRuleRepository.findByTrapIdAndIsActiveTrue(trap.getId())).thenReturn(List.of());

        alertService.fireAlerts(trap.getId(), "No one listening");

        verifyNoInteractions(alertHistoryRepository);
        verify(alertChannelService, never()).deliverNotification(any(), any(), any(), any());
    }

    // ==================== Lifecycle Tests ====================

    @Test
    void testAcknowledgeAlert_fromFired_succeeds() {
        UUID alertId = UUID.randomUUID();
        AlertHistory alert = new AlertHistory();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.FIRED);

        AlertHistoryResponse response = mock(AlertHistoryResponse.class);

        when(alertHistoryRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(alert);
        when(alertHistoryMapper.toResponse(alert)).thenReturn(response);

        alertService.acknowledgeAlert(alertId, USER_ID);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(alert.getAcknowledgedBy()).isEqualTo(USER_ID);
        assertThat(alert.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void testAcknowledgeAlert_alreadyResolved_throwsValidation() {
        UUID alertId = UUID.randomUUID();
        AlertHistory alert = new AlertHistory();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.RESOLVED);

        when(alertHistoryRepository.findById(alertId)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> alertService.acknowledgeAlert(alertId, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot acknowledge a resolved alert");
    }

    @Test
    void testResolveAlert_fromFired_succeeds() {
        UUID alertId = UUID.randomUUID();
        AlertHistory alert = new AlertHistory();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.FIRED);

        AlertHistoryResponse response = mock(AlertHistoryResponse.class);

        when(alertHistoryRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(alert);
        when(alertHistoryMapper.toResponse(alert)).thenReturn(response);

        alertService.resolveAlert(alertId, USER_ID);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getResolvedBy()).isEqualTo(USER_ID);
        assertThat(alert.getResolvedAt()).isNotNull();
        // Also auto-acknowledged since it wasn't previously acked
        assertThat(alert.getAcknowledgedBy()).isEqualTo(USER_ID);
        assertThat(alert.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void testResolveAlert_fromAcknowledged_succeeds() {
        UUID alertId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        AlertHistory alert = new AlertHistory();
        alert.setId(alertId);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(otherUser);
        alert.setAcknowledgedAt(Instant.now().minusSeconds(60));

        AlertHistoryResponse response = mock(AlertHistoryResponse.class);

        when(alertHistoryRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenReturn(alert);
        when(alertHistoryMapper.toResponse(alert)).thenReturn(response);

        alertService.resolveAlert(alertId, USER_ID);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getResolvedBy()).isEqualTo(USER_ID);
        // AcknowledgedBy should remain as the original user
        assertThat(alert.getAcknowledgedBy()).isEqualTo(otherUser);
    }

    @Test
    void testGetActiveAlertCounts_returnsBySeverity() {
        when(alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                TEAM_ID, AlertSeverity.INFO, AlertStatus.FIRED)).thenReturn(2L);
        when(alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                TEAM_ID, AlertSeverity.INFO, AlertStatus.ACKNOWLEDGED)).thenReturn(1L);
        when(alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                TEAM_ID, AlertSeverity.WARNING, AlertStatus.FIRED)).thenReturn(3L);
        when(alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                TEAM_ID, AlertSeverity.WARNING, AlertStatus.ACKNOWLEDGED)).thenReturn(0L);
        when(alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                TEAM_ID, AlertSeverity.CRITICAL, AlertStatus.FIRED)).thenReturn(1L);
        when(alertHistoryRepository.countByTeamIdAndSeverityAndStatus(
                TEAM_ID, AlertSeverity.CRITICAL, AlertStatus.ACKNOWLEDGED)).thenReturn(0L);

        Map<String, Long> counts = alertService.getActiveAlertCounts(TEAM_ID);

        assertThat(counts).containsEntry("INFO", 3L);
        assertThat(counts).containsEntry("WARNING", 3L);
        assertThat(counts).containsEntry("CRITICAL", 1L);
    }
}
