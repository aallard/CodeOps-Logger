package com.codeops.logger.service;

import com.codeops.logger.dto.mapper.RetentionPolicyMapper;
import com.codeops.logger.dto.request.CreateRetentionPolicyRequest;
import com.codeops.logger.dto.request.UpdateRetentionPolicyRequest;
import com.codeops.logger.dto.response.RetentionPolicyResponse;
import com.codeops.logger.dto.response.StorageUsageResponse;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RetentionService}.
 */
@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

    @Mock
    private RetentionPolicyRepository retentionPolicyRepository;

    @Mock
    private RetentionPolicyMapper retentionPolicyMapper;

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private MetricSeriesRepository metricSeriesRepository;

    @Mock
    private TraceSpanRepository traceSpanRepository;

    @InjectMocks
    private RetentionService retentionService;

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

    private RetentionPolicyResponse createResponse(RetentionPolicy p) {
        return new RetentionPolicyResponse(
                p.getId(), p.getName(), p.getSourceName(),
                p.getLogLevel() != null ? p.getLogLevel().name() : null,
                p.getRetentionDays(), p.getAction().name(),
                p.getArchiveDestination(), p.getIsActive(),
                p.getTeamId(), p.getCreatedBy(), p.getLastExecutedAt(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }

    @Test
    void testCreatePolicy_success() {
        CreateRetentionPolicyRequest request = new CreateRetentionPolicyRequest(
                "30-day purge", null, null, 30, "PURGE", null
        );
        RetentionPolicy entity = createPolicy("30-day purge", RetentionAction.PURGE, 30);
        RetentionPolicyResponse response = createResponse(entity);

        when(retentionPolicyMapper.toEntity(request)).thenReturn(entity);
        when(retentionPolicyRepository.save(any(RetentionPolicy.class))).thenReturn(entity);
        when(retentionPolicyMapper.toResponse(entity)).thenReturn(response);

        RetentionPolicyResponse result = retentionService.createPolicy(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("30-day purge");
        assertThat(result.action()).isEqualTo("PURGE");
        verify(retentionPolicyRepository).save(any(RetentionPolicy.class));
    }

    @Test
    void testCreatePolicy_archiveRequiresDestination() {
        CreateRetentionPolicyRequest request = new CreateRetentionPolicyRequest(
                "Archive policy", null, null, 90, "ARCHIVE", null
        );

        assertThatThrownBy(() -> retentionService.createPolicy(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Archive destination is required");
    }

    @Test
    void testCreatePolicy_archiveWithDestination() {
        CreateRetentionPolicyRequest request = new CreateRetentionPolicyRequest(
                "Archive policy", null, null, 90, "ARCHIVE", "s3://bucket/logs"
        );
        RetentionPolicy entity = createPolicy("Archive policy", RetentionAction.ARCHIVE, 90);
        entity.setArchiveDestination("s3://bucket/logs");
        RetentionPolicyResponse response = createResponse(entity);

        when(retentionPolicyMapper.toEntity(request)).thenReturn(entity);
        when(retentionPolicyRepository.save(any(RetentionPolicy.class))).thenReturn(entity);
        when(retentionPolicyMapper.toResponse(entity)).thenReturn(response);

        RetentionPolicyResponse result = retentionService.createPolicy(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("Archive policy");
        verify(retentionPolicyRepository).save(any(RetentionPolicy.class));
    }

    @Test
    void testUpdatePolicy_success() {
        RetentionPolicy existing = createPolicy("Old name", RetentionAction.PURGE, 30);
        UpdateRetentionPolicyRequest request = new UpdateRetentionPolicyRequest(
                "New name", null, null, 60, null, null, null
        );
        RetentionPolicyResponse response = new RetentionPolicyResponse(
                existing.getId(), "New name", null, null, 60, "PURGE",
                null, true, TEAM_ID, USER_ID, null, existing.getCreatedAt(), existing.getUpdatedAt()
        );

        when(retentionPolicyRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(retentionPolicyRepository.save(any(RetentionPolicy.class))).thenReturn(existing);
        when(retentionPolicyMapper.toResponse(existing)).thenReturn(response);

        RetentionPolicyResponse result = retentionService.updatePolicy(existing.getId(), request, TEAM_ID);

        assertThat(result.name()).isEqualTo("New name");
        assertThat(result.retentionDays()).isEqualTo(60);
    }

    @Test
    void testUpdatePolicy_notFound() {
        UUID policyId = UUID.randomUUID();
        UpdateRetentionPolicyRequest request = new UpdateRetentionPolicyRequest(
                "x", null, null, null, null, null, null
        );

        when(retentionPolicyRepository.findById(policyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retentionService.updatePolicy(policyId, request, TEAM_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(policyId.toString());
    }

    @Test
    void testUpdatePolicy_wrongTeam() {
        RetentionPolicy existing = createPolicy("Policy", RetentionAction.PURGE, 30);
        UUID wrongTeam = UUID.randomUUID();
        UpdateRetentionPolicyRequest request = new UpdateRetentionPolicyRequest(
                "x", null, null, null, null, null, null
        );

        when(retentionPolicyRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> retentionService.updatePolicy(existing.getId(), request, wrongTeam))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testUpdatePolicy_archiveWithoutDestination() {
        RetentionPolicy existing = createPolicy("Policy", RetentionAction.PURGE, 30);
        UpdateRetentionPolicyRequest request = new UpdateRetentionPolicyRequest(
                null, null, null, null, "ARCHIVE", null, null
        );

        when(retentionPolicyRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> retentionService.updatePolicy(existing.getId(), request, TEAM_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Archive destination is required");
    }

    @Test
    void testGetPolicy_success() {
        RetentionPolicy policy = createPolicy("My policy", RetentionAction.PURGE, 30);
        RetentionPolicyResponse response = createResponse(policy);

        when(retentionPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(retentionPolicyMapper.toResponse(policy)).thenReturn(response);

        RetentionPolicyResponse result = retentionService.getPolicy(policy.getId(), TEAM_ID);

        assertThat(result.name()).isEqualTo("My policy");
    }

    @Test
    void testGetPolicy_notFound() {
        UUID policyId = UUID.randomUUID();
        when(retentionPolicyRepository.findById(policyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retentionService.getPolicy(policyId, TEAM_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testGetPoliciesByTeam() {
        RetentionPolicy p1 = createPolicy("Policy 1", RetentionAction.PURGE, 30);
        RetentionPolicy p2 = createPolicy("Policy 2", RetentionAction.PURGE, 60);
        List<RetentionPolicy> policies = List.of(p1, p2);
        List<RetentionPolicyResponse> responses = List.of(createResponse(p1), createResponse(p2));

        when(retentionPolicyRepository.findByTeamId(TEAM_ID)).thenReturn(policies);
        when(retentionPolicyMapper.toResponseList(policies)).thenReturn(responses);

        List<RetentionPolicyResponse> result = retentionService.getPoliciesByTeam(TEAM_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void testDeletePolicy_success() {
        RetentionPolicy policy = createPolicy("To delete", RetentionAction.PURGE, 30);

        when(retentionPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));

        retentionService.deletePolicy(policy.getId(), TEAM_ID);

        verify(retentionPolicyRepository).delete(policy);
    }

    @Test
    void testDeletePolicy_notFound() {
        UUID policyId = UUID.randomUUID();
        when(retentionPolicyRepository.findById(policyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retentionService.deletePolicy(policyId, TEAM_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testTogglePolicyActive() {
        RetentionPolicy policy = createPolicy("Toggle me", RetentionAction.PURGE, 30);
        policy.setIsActive(true);

        RetentionPolicyResponse response = new RetentionPolicyResponse(
                policy.getId(), "Toggle me", null, null, 30, "PURGE",
                null, false, TEAM_ID, USER_ID, null, policy.getCreatedAt(), policy.getUpdatedAt()
        );

        when(retentionPolicyRepository.findById(policy.getId())).thenReturn(Optional.of(policy));
        when(retentionPolicyRepository.save(any(RetentionPolicy.class))).thenReturn(policy);
        when(retentionPolicyMapper.toResponse(policy)).thenReturn(response);

        RetentionPolicyResponse result = retentionService.togglePolicyActive(policy.getId(), TEAM_ID, false);

        assertThat(result.isActive()).isFalse();
        verify(retentionPolicyRepository).save(policy);
    }

    @Test
    void testGetStorageUsage() {
        when(logEntryRepository.count()).thenReturn(1000L);
        when(metricSeriesRepository.count()).thenReturn(500L);
        when(traceSpanRepository.count()).thenReturn(200L);

        List<Object[]> serviceGroups = List.of(
                new Object[]{"auth-service", 600L},
                new Object[]{"api-gateway", 400L}
        );
        when(logEntryRepository.countGroupByServiceName()).thenReturn(serviceGroups);

        List<Object[]> levelGroups = List.of(
                new Object[]{LogLevel.INFO, 700L},
                new Object[]{LogLevel.ERROR, 300L}
        );
        when(logEntryRepository.countGroupByLevel()).thenReturn(levelGroups);

        RetentionPolicy activePolicy = createPolicy("Active", RetentionAction.PURGE, 30);
        when(retentionPolicyRepository.findByIsActiveTrue()).thenReturn(List.of(activePolicy));

        Instant oldest = Instant.parse("2025-01-01T00:00:00Z");
        Instant newest = Instant.parse("2026-02-19T00:00:00Z");
        when(logEntryRepository.findOldestTimestamp()).thenReturn(Optional.of(oldest));
        when(logEntryRepository.findNewestTimestamp()).thenReturn(Optional.of(newest));

        StorageUsageResponse result = retentionService.getStorageUsage();

        assertThat(result.totalLogEntries()).isEqualTo(1000L);
        assertThat(result.totalMetricDataPoints()).isEqualTo(500L);
        assertThat(result.totalTraceSpans()).isEqualTo(200L);
        assertThat(result.logEntriesByService()).containsEntry("auth-service", 600L);
        assertThat(result.logEntriesByService()).containsEntry("api-gateway", 400L);
        assertThat(result.logEntriesByLevel()).containsEntry("INFO", 700L);
        assertThat(result.logEntriesByLevel()).containsEntry("ERROR", 300L);
        assertThat(result.activeRetentionPolicies()).isEqualTo(1);
        assertThat(result.oldestLogEntry()).isEqualTo(oldest);
        assertThat(result.newestLogEntry()).isEqualTo(newest);
    }
}
