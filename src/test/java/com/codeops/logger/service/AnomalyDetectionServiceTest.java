package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.AnomalyBaselineMapper;
import com.codeops.logger.dto.request.CreateBaselineRequest;
import com.codeops.logger.dto.request.UpdateBaselineRequest;
import com.codeops.logger.dto.response.AnomalyBaselineResponse;
import com.codeops.logger.dto.response.AnomalyCheckResponse;
import com.codeops.logger.dto.response.AnomalyReportResponse;
import com.codeops.logger.entity.AnomalyBaseline;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.AnomalyBaselineRepository;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.MetricRepository;
import com.codeops.logger.repository.MetricSeriesRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnomalyDetectionService}.
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private AnomalyBaselineRepository baselineRepository;

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private MetricSeriesRepository metricSeriesRepository;

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private AnomalyBaselineMapper baselineMapper;

    @Mock
    private AnomalyBaselineCalculator calculator;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    private static final UUID TEAM_ID = UUID.randomUUID();

    private AnomalyBaseline createBaseline(String serviceName, String metricName,
            double mean, double stddev) {
        AnomalyBaseline baseline = new AnomalyBaseline();
        baseline.setId(UUID.randomUUID());
        baseline.setServiceName(serviceName);
        baseline.setMetricName(metricName);
        baseline.setBaselineValue(mean);
        baseline.setStandardDeviation(stddev);
        baseline.setSampleCount(168L);
        baseline.setWindowStartTime(Instant.parse("2026-02-12T00:00:00Z"));
        baseline.setWindowEndTime(Instant.parse("2026-02-19T00:00:00Z"));
        baseline.setDeviationThreshold(AppConstants.DEFAULT_DEVIATION_THRESHOLD);
        baseline.setIsActive(true);
        baseline.setTeamId(TEAM_ID);
        baseline.setCreatedAt(Instant.now());
        baseline.setUpdatedAt(Instant.now());
        return baseline;
    }

    private AnomalyBaselineResponse createBaselineResponse(AnomalyBaseline b) {
        return new AnomalyBaselineResponse(
                b.getId(), b.getServiceName(), b.getMetricName(),
                b.getBaselineValue(), b.getStandardDeviation(), b.getSampleCount(),
                b.getWindowStartTime(), b.getWindowEndTime(),
                b.getDeviationThreshold(), b.getIsActive(), b.getTeamId(),
                b.getLastComputedAt(), b.getCreatedAt(), b.getUpdatedAt()
        );
    }

    // ==================== Baseline CRUD ====================

    @Test
    void testCreateBaseline_new_computesAndSaves() {
        CreateBaselineRequest request = new CreateBaselineRequest(
                "auth-service", "log_volume", 48, 2.5);

        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "auth-service", "log_volume"))
                .thenReturn(Optional.empty());

        // Hourly log volume collection — all hours return 10
        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(10L);

        AnomalyBaselineCalculator.BaselineStats stats =
                new AnomalyBaselineCalculator.BaselineStats(10.0, 1.5, 48);
        when(calculator.computeBaseline(anyList())).thenReturn(Optional.of(stats));

        AnomalyBaseline savedEntity = createBaseline("auth-service", "log_volume", 10.0, 1.5);
        when(baselineRepository.save(any(AnomalyBaseline.class))).thenReturn(savedEntity);

        AnomalyBaselineResponse response = createBaselineResponse(savedEntity);
        when(baselineMapper.toResponse(any(AnomalyBaseline.class))).thenReturn(response);

        AnomalyBaselineResponse result = anomalyDetectionService
                .createOrUpdateBaseline(request, TEAM_ID);

        assertThat(result.serviceName()).isEqualTo("auth-service");
        assertThat(result.metricName()).isEqualTo("log_volume");
        verify(baselineRepository).save(any(AnomalyBaseline.class));
    }

    @Test
    void testCreateBaseline_existing_updatesBaseline() {
        CreateBaselineRequest request = new CreateBaselineRequest(
                "auth-service", "log_volume", 48, null);

        AnomalyBaseline existing = createBaseline("auth-service", "log_volume", 50.0, 5.0);
        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "auth-service", "log_volume"))
                .thenReturn(Optional.of(existing));

        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(20L);

        AnomalyBaselineCalculator.BaselineStats stats =
                new AnomalyBaselineCalculator.BaselineStats(20.0, 2.0, 48);
        when(calculator.computeBaseline(anyList())).thenReturn(Optional.of(stats));

        when(baselineRepository.save(any(AnomalyBaseline.class))).thenReturn(existing);

        AnomalyBaselineResponse response = createBaselineResponse(existing);
        when(baselineMapper.toResponse(any(AnomalyBaseline.class))).thenReturn(response);

        AnomalyBaselineResponse result = anomalyDetectionService
                .createOrUpdateBaseline(request, TEAM_ID);

        assertThat(result).isNotNull();
        assertThat(existing.getBaselineValue()).isEqualTo(20.0);
        assertThat(existing.getStandardDeviation()).isEqualTo(2.0);
        verify(baselineRepository).save(existing);
    }

    @Test
    void testCreateBaseline_insufficientData_throwsValidation() {
        CreateBaselineRequest request = new CreateBaselineRequest(
                "auth-service", "log_volume", 48, null);

        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "auth-service", "log_volume"))
                .thenReturn(Optional.empty());

        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);

        when(calculator.computeBaseline(anyList())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyDetectionService.createOrUpdateBaseline(request, TEAM_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Insufficient data");
    }

    @Test
    void testGetBaselinesByTeam_returnsList() {
        AnomalyBaseline b1 = createBaseline("svc1", "log_volume", 100.0, 10.0);
        AnomalyBaseline b2 = createBaseline("svc2", "error_rate", 5.0, 2.0);
        List<AnomalyBaseline> baselines = List.of(b1, b2);
        List<AnomalyBaselineResponse> responses = List.of(
                createBaselineResponse(b1), createBaselineResponse(b2));

        when(baselineRepository.findByTeamId(TEAM_ID)).thenReturn(baselines);
        when(baselineMapper.toResponseList(baselines)).thenReturn(responses);

        List<AnomalyBaselineResponse> result = anomalyDetectionService.getBaselinesByTeam(TEAM_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void testGetBaselinesByService_filtersCorrectly() {
        AnomalyBaseline b1 = createBaseline("auth-service", "log_volume", 100.0, 10.0);
        List<AnomalyBaseline> baselines = List.of(b1);
        List<AnomalyBaselineResponse> responses = List.of(createBaselineResponse(b1));

        when(baselineRepository.findByTeamIdAndServiceName(TEAM_ID, "auth-service"))
                .thenReturn(baselines);
        when(baselineMapper.toResponseList(baselines)).thenReturn(responses);

        List<AnomalyBaselineResponse> result = anomalyDetectionService
                .getBaselinesByService(TEAM_ID, "auth-service");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).serviceName()).isEqualTo("auth-service");
    }

    @Test
    void testGetBaseline_notFound() {
        UUID baselineId = UUID.randomUUID();
        when(baselineRepository.findById(baselineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyDetectionService.getBaseline(baselineId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(baselineId.toString());
    }

    @Test
    void testUpdateBaseline_updatesThreshold() {
        AnomalyBaseline baseline = createBaseline("auth-service", "log_volume", 100.0, 10.0);
        UpdateBaselineRequest request = new UpdateBaselineRequest(null, 3.0, null);

        when(baselineRepository.findById(baseline.getId())).thenReturn(Optional.of(baseline));
        when(baselineRepository.save(any(AnomalyBaseline.class))).thenReturn(baseline);
        when(baselineMapper.toResponse(any(AnomalyBaseline.class)))
                .thenReturn(createBaselineResponse(baseline));

        AnomalyBaselineResponse result = anomalyDetectionService
                .updateBaseline(baseline.getId(), request);

        assertThat(result).isNotNull();
        assertThat(baseline.getDeviationThreshold()).isEqualTo(3.0);
        verify(baselineRepository).save(baseline);
    }

    @Test
    void testDeleteBaseline_succeeds() {
        AnomalyBaseline baseline = createBaseline("auth-service", "log_volume", 100.0, 10.0);

        when(baselineRepository.findById(baseline.getId())).thenReturn(Optional.of(baseline));

        anomalyDetectionService.deleteBaseline(baseline.getId());

        verify(baselineRepository).delete(baseline);
    }

    @Test
    void testDeleteBaseline_notFound() {
        UUID baselineId = UUID.randomUUID();
        when(baselineRepository.findById(baselineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyDetectionService.deleteBaseline(baselineId))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== Anomaly Checking ====================

    @Test
    void testCheckAnomaly_normalValue_notAnomaly() {
        AnomalyBaseline baseline = createBaseline("auth-service", "log_volume", 100.0, 10.0);

        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "auth-service", "log_volume"))
                .thenReturn(Optional.of(baseline));

        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(105L);

        when(calculator.calculateZScore(105.0, 100.0, 10.0)).thenReturn(0.5);
        when(calculator.isAnomaly(105.0, 100.0, 10.0, 2.0)).thenReturn(false);
        when(calculator.getDirection(105.0, 100.0)).thenReturn("ABOVE");

        AnomalyCheckResponse result = anomalyDetectionService
                .checkAnomaly(TEAM_ID, "auth-service", "log_volume");

        assertThat(result.isAnomaly()).isFalse();
        assertThat(result.currentValue()).isEqualTo(105.0);
        assertThat(result.zScore()).isEqualTo(0.5);
        assertThat(result.direction()).isEqualTo("ABOVE");
    }

    @Test
    void testCheckAnomaly_anomalousValue_detected() {
        AnomalyBaseline baseline = createBaseline("auth-service", "log_volume", 100.0, 10.0);

        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "auth-service", "log_volume"))
                .thenReturn(Optional.of(baseline));

        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(500L);

        when(calculator.calculateZScore(500.0, 100.0, 10.0)).thenReturn(40.0);
        when(calculator.isAnomaly(500.0, 100.0, 10.0, 2.0)).thenReturn(true);
        when(calculator.getDirection(500.0, 100.0)).thenReturn("ABOVE");

        AnomalyCheckResponse result = anomalyDetectionService
                .checkAnomaly(TEAM_ID, "auth-service", "log_volume");

        assertThat(result.isAnomaly()).isTrue();
        assertThat(result.currentValue()).isEqualTo(500.0);
        assertThat(result.zScore()).isEqualTo(40.0);
    }

    @Test
    void testCheckAnomaly_noBaseline_throwsNotFound() {
        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "auth-service", "log_volume"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyDetectionService
                .checkAnomaly(TEAM_ID, "auth-service", "log_volume"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No baseline found");
    }

    @Test
    void testRunFullCheck_reportsAllAnomalies() {
        AnomalyBaseline b1 = createBaseline("svc1", "log_volume", 100.0, 10.0);
        AnomalyBaseline b2 = createBaseline("svc2", "log_volume", 100.0, 10.0);

        when(baselineRepository.findByTeamIdAndIsActiveTrue(TEAM_ID))
                .thenReturn(List.of(b1, b2));

        // b1: normal check
        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "svc1", "log_volume"))
                .thenReturn(Optional.of(b1));
        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("svc1"), any(Instant.class), any(Instant.class)))
                .thenReturn(100L);
        when(calculator.calculateZScore(100.0, 100.0, 10.0)).thenReturn(0.0);
        when(calculator.isAnomaly(100.0, 100.0, 10.0, 2.0)).thenReturn(false);
        when(calculator.getDirection(100.0, 100.0)).thenReturn("NORMAL");

        // b2: anomalous check
        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "svc2", "log_volume"))
                .thenReturn(Optional.of(b2));
        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("svc2"), any(Instant.class), any(Instant.class)))
                .thenReturn(500L);
        when(calculator.calculateZScore(500.0, 100.0, 10.0)).thenReturn(40.0);
        when(calculator.isAnomaly(500.0, 100.0, 10.0, 2.0)).thenReturn(true);
        when(calculator.getDirection(500.0, 100.0)).thenReturn("ABOVE");

        AnomalyReportResponse result = anomalyDetectionService.runFullCheck(TEAM_ID);

        assertThat(result.totalBaselines()).isEqualTo(2);
        assertThat(result.anomaliesDetected()).isEqualTo(1);
        assertThat(result.allChecks()).hasSize(2);
        assertThat(result.anomalies()).hasSize(1);
        assertThat(result.anomalies().get(0).serviceName()).isEqualTo("svc2");
    }

    @Test
    void testRunFullCheck_handlesErrors_continues() {
        AnomalyBaseline b1 = createBaseline("svc1", "log_volume", 100.0, 10.0);
        AnomalyBaseline b2 = createBaseline("svc2", "log_volume", 100.0, 10.0);

        when(baselineRepository.findByTeamIdAndIsActiveTrue(TEAM_ID))
                .thenReturn(List.of(b1, b2));

        // b1: fails (no baseline found on re-fetch)
        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "svc1", "log_volume"))
                .thenReturn(Optional.empty());

        // b2: succeeds
        when(baselineRepository.findByTeamIdAndServiceNameAndMetricName(
                TEAM_ID, "svc2", "log_volume"))
                .thenReturn(Optional.of(b2));
        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("svc2"), any(Instant.class), any(Instant.class)))
                .thenReturn(100L);
        when(calculator.calculateZScore(100.0, 100.0, 10.0)).thenReturn(0.0);
        when(calculator.isAnomaly(100.0, 100.0, 10.0, 2.0)).thenReturn(false);
        when(calculator.getDirection(100.0, 100.0)).thenReturn("NORMAL");

        AnomalyReportResponse result = anomalyDetectionService.runFullCheck(TEAM_ID);

        // b1 error was caught, b2 processed successfully
        assertThat(result.totalBaselines()).isEqualTo(2);
        assertThat(result.allChecks()).hasSize(1);
        assertThat(result.anomaliesDetected()).isEqualTo(0);
    }

    @Test
    void testRecalculateAllBaselines_processesActive() {
        AnomalyBaseline baseline = createBaseline("auth-service", "log_volume", 100.0, 10.0);

        when(baselineRepository.findByIsActiveTrue()).thenReturn(List.of(baseline));

        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(15L);

        AnomalyBaselineCalculator.BaselineStats stats =
                new AnomalyBaselineCalculator.BaselineStats(15.0, 2.0, 168);
        when(calculator.computeBaseline(anyList())).thenReturn(Optional.of(stats));

        when(baselineRepository.save(any(AnomalyBaseline.class))).thenReturn(baseline);

        anomalyDetectionService.recalculateAllBaselines();

        verify(baselineRepository).save(baseline);
        assertThat(baseline.getBaselineValue()).isEqualTo(15.0);
        assertThat(baseline.getStandardDeviation()).isEqualTo(2.0);
        assertThat(baseline.getLastComputedAt()).isNotNull();
    }

    // ==================== Data Collection ====================

    @Test
    void testCollectHourlyLogVolume_correctCounts() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T03:00:00Z");

        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(10L, 20L, 30L);

        List<Double> result = anomalyDetectionService
                .collectHourlyLogVolume(TEAM_ID, "auth-service", start, end);

        assertThat(result).containsExactly(10.0, 20.0, 30.0);
    }

    @Test
    void testCollectHourlyErrorRate_correctPercentages() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-01T02:00:00Z");

        // Hour 1: total=100, errors=5, fatals=5 → 10%
        // Hour 2: total=0 → 0%
        when(logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), any(Instant.class), any(Instant.class)))
                .thenReturn(100L, 0L);

        when(logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), eq(LogLevel.ERROR),
                any(Instant.class), any(Instant.class)))
                .thenReturn(5L);

        when(logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                eq(TEAM_ID), eq("auth-service"), eq(LogLevel.FATAL),
                any(Instant.class), any(Instant.class)))
                .thenReturn(5L);

        List<Double> result = anomalyDetectionService
                .collectHourlyErrorRate(TEAM_ID, "auth-service", start, end);

        assertThat(result).containsExactly(10.0, 0.0);
    }
}
