package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.entity.RetentionPolicy;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.entity.enums.RetentionAction;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.MetricSeriesRepository;
import com.codeops.logger.repository.RetentionPolicyRepository;
import com.codeops.logger.repository.TraceSpanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RetentionExecutor}.
 */
@ExtendWith(MockitoExtension.class)
class RetentionExecutorTest {

    @Mock
    private RetentionPolicyRepository retentionPolicyRepository;

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private MetricSeriesRepository metricSeriesRepository;

    @Mock
    private TraceSpanRepository traceSpanRepository;

    @InjectMocks
    private RetentionExecutor retentionExecutor;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private RetentionPolicy createPolicy(String name, RetentionAction action, int days) {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setId(UUID.randomUUID());
        policy.setName(name);
        policy.setAction(action);
        policy.setRetentionDays(days);
        policy.setIsActive(true);
        policy.setTeamId(TEAM_ID);
        policy.setCreatedBy(USER_ID);
        policy.setCreatedAt(Instant.now());
        policy.setUpdatedAt(Instant.now());
        return policy;
    }

    @Test
    void testExecuteAllActivePolicies_success() {
        RetentionPolicy p1 = createPolicy("Policy 1", RetentionAction.PURGE, 30);
        RetentionPolicy p2 = createPolicy("Policy 2", RetentionAction.PURGE, 60);

        when(retentionPolicyRepository.findByIsActiveTrue()).thenReturn(List.of(p1, p2));
        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executeAllActivePolicies();

        verify(logEntryRepository, times(2)).deleteByTeamIdAndTimestampBefore(eq(TEAM_ID), any(Instant.class));
        verify(retentionPolicyRepository, times(2)).save(any(RetentionPolicy.class));
    }

    @Test
    void testExecuteAllActivePolicies_partialFailure() {
        RetentionPolicy p1 = createPolicy("Failing policy", RetentionAction.PURGE, 30);
        RetentionPolicy p2 = createPolicy("Working policy", RetentionAction.PURGE, 60);

        when(retentionPolicyRepository.findByIsActiveTrue()).thenReturn(List.of(p1, p2));
        doThrow(new RuntimeException("DB error"))
                .when(logEntryRepository).deleteByTeamIdAndTimestampBefore(eq(p1.getTeamId()), any(Instant.class));
        // Second call succeeds — need to reset behavior for second policy
        // Since both policies have the same teamId, we need different approach
        // Use doThrow for first call, then doNothing for second
        // Actually, since they share TEAM_ID, both calls will throw. Let's use different team IDs.
        UUID team2 = UUID.randomUUID();
        p2.setTeamId(team2);

        doNothing().when(logEntryRepository).deleteByTeamIdAndTimestampBefore(eq(team2), any(Instant.class));
        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executeAllActivePolicies();

        // p1 failed, p2 succeeded — only p2 saved (lastExecutedAt update)
        verify(retentionPolicyRepository, times(1)).save(any(RetentionPolicy.class));
    }

    @Test
    void testExecutePolicy_purgeNoFilters() {
        RetentionPolicy policy = createPolicy("No filters", RetentionAction.PURGE, 30);

        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executePolicy(policy);

        verify(logEntryRepository).deleteByTeamIdAndTimestampBefore(eq(TEAM_ID), any(Instant.class));
        assertThat(policy.getLastExecutedAt()).isNotNull();
    }

    @Test
    void testExecutePolicy_purgeWithSourceName() {
        RetentionPolicy policy = createPolicy("Source filter", RetentionAction.PURGE, 30);
        policy.setSourceName("auth-service");

        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executePolicy(policy);

        verify(logEntryRepository).deleteByTeamIdAndServiceNameAndTimestampBefore(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class));
    }

    @Test
    void testExecutePolicy_purgeWithLogLevel() {
        RetentionPolicy policy = createPolicy("Level filter", RetentionAction.PURGE, 30);
        policy.setLogLevel(LogLevel.WARN);

        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executePolicy(policy);

        List<LogLevel> expectedLevels = List.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN);
        verify(logEntryRepository).deleteByTeamIdAndTimestampBeforeAndLevelIn(
                eq(TEAM_ID), any(Instant.class), eq(expectedLevels));
    }

    @Test
    void testExecutePolicy_purgeWithBothFilters() {
        RetentionPolicy policy = createPolicy("Both filters", RetentionAction.PURGE, 30);
        policy.setSourceName("api-gateway");
        policy.setLogLevel(LogLevel.INFO);

        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executePolicy(policy);

        List<LogLevel> expectedLevels = List.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO);
        verify(logEntryRepository).deleteByTeamIdAndServiceNameAndTimestampBeforeAndLevelIn(
                eq(TEAM_ID), eq("api-gateway"), any(Instant.class), eq(expectedLevels));
    }

    @Test
    void testExecutePolicy_archiveFallsToPurge() {
        RetentionPolicy policy = createPolicy("Archive fallback", RetentionAction.ARCHIVE, 90);
        policy.setArchiveDestination("s3://bucket/archive");

        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executePolicy(policy);

        // ARCHIVE falls back to PURGE behavior
        verify(logEntryRepository).deleteByTeamIdAndTimestampBefore(eq(TEAM_ID), any(Instant.class));
        assertThat(policy.getLastExecutedAt()).isNotNull();
    }

    @Test
    void testExecutePolicy_updatesLastExecutedAt() {
        RetentionPolicy policy = createPolicy("Timestamp check", RetentionAction.PURGE, 30);
        assertThat(policy.getLastExecutedAt()).isNull();

        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.executePolicy(policy);

        assertThat(policy.getLastExecutedAt()).isNotNull();
        verify(retentionPolicyRepository).save(policy);
    }

    @Test
    void testManualExecute_success() {
        RetentionPolicy policy = createPolicy("Manual exec", RetentionAction.PURGE, 30);

        when(retentionPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(retentionPolicyRepository.save(any(RetentionPolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        retentionExecutor.manualExecute(policy.getId(), TEAM_ID);

        verify(logEntryRepository).deleteByTeamIdAndTimestampBefore(eq(TEAM_ID), any(Instant.class));
        assertThat(policy.getLastExecutedAt()).isNotNull();
    }

    @Test
    void testManualExecute_notFound() {
        UUID policyId = UUID.randomUUID();
        when(retentionPolicyRepository.findById(policyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retentionExecutor.manualExecute(policyId, TEAM_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(policyId.toString());
    }

    @Test
    void testManualExecute_wrongTeam() {
        RetentionPolicy policy = createPolicy("Wrong team", RetentionAction.PURGE, 30);
        UUID wrongTeam = UUID.randomUUID();

        when(retentionPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        assertThatThrownBy(() -> retentionExecutor.manualExecute(policy.getId(), wrongTeam))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGlobalPurge_success() {
        retentionExecutor.globalPurge(30);

        verify(logEntryRepository).deleteByTimestampBefore(any(Instant.class));
        verify(metricSeriesRepository).deleteByTimestampBefore(any(Instant.class));
        verify(traceSpanRepository).deleteByStartTimeBefore(any(Instant.class));
    }

    @Test
    void testGlobalPurge_invalidRetentionDays_tooLow() {
        assertThatThrownBy(() -> retentionExecutor.globalPurge(0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Retention days must be between");
    }

    @Test
    void testGlobalPurge_invalidRetentionDays_tooHigh() {
        assertThatThrownBy(() -> retentionExecutor.globalPurge(366))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Retention days must be between");
    }

    @Test
    void testGetLevelsAtOrBelow() {
        List<LogLevel> traceOnly = retentionExecutor.getLevelsAtOrBelow(LogLevel.TRACE);
        assertThat(traceOnly).containsExactly(LogLevel.TRACE);

        List<LogLevel> upToWarn = retentionExecutor.getLevelsAtOrBelow(LogLevel.WARN);
        assertThat(upToWarn).containsExactly(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN);

        List<LogLevel> allLevels = retentionExecutor.getLevelsAtOrBelow(LogLevel.FATAL);
        assertThat(allLevels).containsExactly(
                LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO,
                LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL
        );
    }
}
