package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.MetricMapper;
import com.codeops.logger.dto.request.PushMetricDataRequest;
import com.codeops.logger.dto.request.RegisterMetricRequest;
import com.codeops.logger.dto.request.UpdateMetricRequest;
import com.codeops.logger.dto.response.*;
import com.codeops.logger.entity.Metric;
import com.codeops.logger.entity.MetricSeries;
import com.codeops.logger.entity.enums.MetricType;
import com.codeops.logger.exception.AuthorizationException;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.MetricRepository;
import com.codeops.logger.repository.MetricSeriesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MetricsService}.
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private MetricSeriesRepository metricSeriesRepository;

    @Mock
    private MetricMapper metricMapper;

    @Mock
    private MetricAggregationService aggregationService;

    @InjectMocks
    private MetricsService metricsService;

    private static final UUID TEAM_ID = UUID.randomUUID();

    private Metric createMetric(String name, MetricType type, String serviceName) {
        Metric metric = new Metric();
        metric.setId(UUID.randomUUID());
        metric.setName(name);
        metric.setMetricType(type);
        metric.setServiceName(serviceName);
        metric.setTeamId(TEAM_ID);
        return metric;
    }

    private MetricResponse createResponse(Metric metric) {
        return new MetricResponse(
                metric.getId(), metric.getName(), metric.getMetricType().name(),
                metric.getDescription(), metric.getUnit(), metric.getServiceName(),
                metric.getTags(), metric.getTeamId(), Instant.now(), Instant.now());
    }

    // ==================== Registration Tests ====================

    @Test
    void testRegisterMetric_new_creates() {
        RegisterMetricRequest request = new RegisterMetricRequest(
                "request_count", "COUNTER", "Total HTTP requests", "count", "api-service", null);

        Metric entity = createMetric("request_count", MetricType.COUNTER, "api-service");
        MetricResponse response = createResponse(entity);

        when(metricRepository.findByTeamIdAndNameAndServiceName(TEAM_ID, "request_count", "api-service"))
                .thenReturn(Optional.empty());
        when(metricMapper.toEntity(request)).thenReturn(entity);
        when(metricRepository.save(any(Metric.class))).thenReturn(entity);
        when(metricMapper.toResponse(entity)).thenReturn(response);

        MetricResponse result = metricsService.registerMetric(request, TEAM_ID);

        assertThat(result.name()).isEqualTo("request_count");
        verify(metricRepository).save(any(Metric.class));
    }

    @Test
    void testRegisterMetric_existing_returnsExisting() {
        RegisterMetricRequest request = new RegisterMetricRequest(
                "request_count", "COUNTER", null, null, "api-service", null);

        Metric existing = createMetric("request_count", MetricType.COUNTER, "api-service");
        MetricResponse response = createResponse(existing);

        when(metricRepository.findByTeamIdAndNameAndServiceName(TEAM_ID, "request_count", "api-service"))
                .thenReturn(Optional.of(existing));
        when(metricMapper.toResponse(existing)).thenReturn(response);

        MetricResponse result = metricsService.registerMetric(request, TEAM_ID);

        assertThat(result.name()).isEqualTo("request_count");
        verify(metricRepository, never()).save(any(Metric.class));
    }

    @Test
    void testRegisterMetric_invalidType_throwsValidation() {
        RegisterMetricRequest request = new RegisterMetricRequest(
                "bad_metric", "INVALID_TYPE", null, null, "api-service", null);

        when(metricRepository.findByTeamIdAndNameAndServiceName(TEAM_ID, "bad_metric", "api-service"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricsService.registerMetric(request, TEAM_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid metric type");
    }

    // ==================== Query Tests ====================

    @Test
    void testGetMetricsByTeam_returnsList() {
        Metric m1 = createMetric("metric1", MetricType.COUNTER, "svc1");
        Metric m2 = createMetric("metric2", MetricType.GAUGE, "svc2");

        when(metricRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(m1, m2));
        when(metricMapper.toResponseList(any())).thenReturn(List.of(
                mock(MetricResponse.class), mock(MetricResponse.class)));

        List<MetricResponse> result = metricsService.getMetricsByTeam(TEAM_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void testGetMetricsByService_filtersCorrectly() {
        Metric m1 = createMetric("latency", MetricType.TIMER, "api-service");

        when(metricRepository.findByTeamIdAndServiceName(TEAM_ID, "api-service"))
                .thenReturn(List.of(m1));
        when(metricMapper.toResponseList(any())).thenReturn(List.of(mock(MetricResponse.class)));

        List<MetricResponse> result = metricsService.getMetricsByService(TEAM_ID, "api-service");

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetMetric_found() {
        Metric metric = createMetric("cpu_usage", MetricType.GAUGE, "host-agent");
        MetricResponse response = createResponse(metric);

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));
        when(metricMapper.toResponse(metric)).thenReturn(response);

        MetricResponse result = metricsService.getMetric(metric.getId());

        assertThat(result.name()).isEqualTo("cpu_usage");
    }

    @Test
    void testGetMetric_notFound_throwsNotFound() {
        UUID fakeId = UUID.randomUUID();
        when(metricRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricsService.getMetric(fakeId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Metric not found");
    }

    @Test
    void testUpdateMetric_updatesDescriptionAndUnit() {
        Metric metric = createMetric("latency", MetricType.TIMER, "api-service");
        UpdateMetricRequest request = new UpdateMetricRequest("New description", "ms", null);
        MetricResponse response = createResponse(metric);

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));
        when(metricRepository.save(any(Metric.class))).thenReturn(metric);
        when(metricMapper.toResponse(metric)).thenReturn(response);

        metricsService.updateMetric(metric.getId(), request);

        assertThat(metric.getDescription()).isEqualTo("New description");
        assertThat(metric.getUnit()).isEqualTo("ms");
    }

    @Test
    void testDeleteMetric_deletesSeriesAndMetric() {
        Metric metric = createMetric("old_metric", MetricType.COUNTER, "svc");

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));

        metricsService.deleteMetric(metric.getId());

        verify(metricSeriesRepository).deleteByMetricId(metric.getId());
        verify(metricRepository).delete(metric);
    }

    // ==================== Data Push Tests ====================

    @Test
    void testPushMetricData_validPoints_saves() {
        Metric metric = createMetric("request_count", MetricType.GAUGE, "api-service");
        Instant now = Instant.parse("2026-02-20T10:00:00Z");
        PushMetricDataRequest request = new PushMetricDataRequest(
                metric.getId(),
                List.of(
                        new PushMetricDataRequest.MetricDataPoint(now, 100.0, null),
                        new PushMetricDataRequest.MetricDataPoint(now.plusSeconds(60), 110.0, null)
                )
        );

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));

        int count = metricsService.pushMetricData(request, TEAM_ID);

        assertThat(count).isEqualTo(2);
        verify(metricSeriesRepository).saveAll(anyList());
    }

    @Test
    void testPushMetricData_metricNotFound_throwsNotFound() {
        UUID fakeId = UUID.randomUUID();
        PushMetricDataRequest request = new PushMetricDataRequest(
                fakeId,
                List.of(new PushMetricDataRequest.MetricDataPoint(Instant.now(), 1.0, null))
        );

        when(metricRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricsService.pushMetricData(request, TEAM_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Metric not found");
    }

    @Test
    void testPushMetricData_wrongTeam_throwsAuthorization() {
        Metric metric = createMetric("metric", MetricType.GAUGE, "svc");
        UUID otherTeamId = UUID.randomUUID();
        PushMetricDataRequest request = new PushMetricDataRequest(
                metric.getId(),
                List.of(new PushMetricDataRequest.MetricDataPoint(Instant.now(), 1.0, null))
        );

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));

        assertThatThrownBy(() -> metricsService.pushMetricData(request, otherTeamId))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("does not belong to this team");
    }

    @Test
    void testPushMetricData_exceedsBatchSize_throwsValidation() {
        Metric metric = createMetric("metric", MetricType.GAUGE, "svc");
        List<PushMetricDataRequest.MetricDataPoint> tooMany = new java.util.ArrayList<>();
        for (int i = 0; i <= AppConstants.MAX_BATCH_SIZE; i++) {
            tooMany.add(new PushMetricDataRequest.MetricDataPoint(Instant.now(), (double) i, null));
        }
        PushMetricDataRequest request = new PushMetricDataRequest(metric.getId(), tooMany);

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));

        assertThatThrownBy(() -> metricsService.pushMetricData(request, TEAM_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum batch size");
    }

    @Test
    void testPushSingleValue_autoRegistersAndSaves() {
        Metric metric = createMetric("cpu_usage", MetricType.GAUGE, "host-agent");
        MetricResponse response = createResponse(metric);

        when(metricRepository.findByTeamIdAndNameAndServiceName(TEAM_ID, "cpu_usage", "host-agent"))
                .thenReturn(Optional.of(metric));
        when(metricMapper.toResponse(metric)).thenReturn(response);
        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));

        metricsService.pushSingleValue("cpu_usage", "GAUGE", "host-agent", 75.5, TEAM_ID);

        verify(metricSeriesRepository).save(any(MetricSeries.class));
    }

    // ==================== Time-Series Tests ====================

    @Test
    void testGetTimeSeries_returnsOrderedByTimestamp() {
        Metric metric = createMetric("latency", MetricType.TIMER, "api-service");
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        Instant end = Instant.parse("2026-02-20T11:00:00Z");

        MetricSeries s1 = new MetricSeries();
        s1.setTimestamp(start.plusSeconds(60));
        s1.setValue(100.0);
        s1.setTags(null);

        MetricSeries s2 = new MetricSeries();
        s2.setTimestamp(start.plusSeconds(120));
        s2.setValue(200.0);
        s2.setTags(null);

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));
        when(metricSeriesRepository.findByMetricIdAndTimestampBetweenOrderByTimestampAsc(
                metric.getId(), start, end)).thenReturn(List.of(s1, s2));

        MetricTimeSeriesResponse result = metricsService.getTimeSeries(metric.getId(), start, end);

        assertThat(result.dataPoints()).hasSize(2);
        assertThat(result.dataPoints().get(0).value()).isCloseTo(100.0, within(0.001));
        assertThat(result.dataPoints().get(1).value()).isCloseTo(200.0, within(0.001));
        assertThat(result.metricName()).isEqualTo("latency");
        assertThat(result.resolution()).isNull();
    }

    @Test
    void testGetTimeSeriesAggregated_bucketsCorrectly() {
        Metric metric = createMetric("latency", MetricType.TIMER, "api-service");
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        Instant end = Instant.parse("2026-02-20T11:00:00Z");

        MetricSeries s1 = new MetricSeries();
        s1.setTimestamp(start.plusSeconds(10));
        s1.setValue(50.0);

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));
        when(metricSeriesRepository.findByMetricIdAndTimestampBetweenOrderByTimestampAsc(
                metric.getId(), start, end)).thenReturn(List.of(s1));
        when(aggregationService.aggregateByResolution(any(), eq(start), eq(end), eq(60)))
                .thenReturn(List.of(new MetricTimeSeriesResponse.DataPoint(start, 50.0, null)));

        MetricTimeSeriesResponse result = metricsService.getTimeSeriesAggregated(
                metric.getId(), start, end, 60);

        assertThat(result.dataPoints()).hasSize(1);
        assertThat(result.resolution()).isEqualTo(60);
    }

    @Test
    void testGetAggregation_returnsFullStats() {
        Metric metric = createMetric("latency", MetricType.TIMER, "api-service");
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        Instant end = Instant.parse("2026-02-20T11:00:00Z");

        MetricSeries s1 = new MetricSeries();
        s1.setTimestamp(start.plusSeconds(60));
        s1.setValue(100.0);

        MetricSeries s2 = new MetricSeries();
        s2.setTimestamp(start.plusSeconds(120));
        s2.setValue(200.0);

        when(metricRepository.findById(metric.getId())).thenReturn(Optional.of(metric));
        when(metricSeriesRepository.findByMetricIdAndTimestampBetweenOrderByTimestampAsc(
                metric.getId(), start, end)).thenReturn(List.of(s1, s2));
        when(aggregationService.aggregate(List.of(100.0, 200.0)))
                .thenReturn(new MetricAggregationService.AggregationResult(
                        2, 300.0, 150.0, 100.0, 200.0, 100.0, 200.0, 200.0, 50.0));

        MetricAggregationResponse result = metricsService.getAggregation(metric.getId(), start, end);

        assertThat(result.dataPointCount()).isEqualTo(2);
        assertThat(result.sum()).isCloseTo(300.0, within(0.001));
        assertThat(result.avg()).isCloseTo(150.0, within(0.001));
        assertThat(result.p50()).isCloseTo(100.0, within(0.001));
    }

    // ==================== Latest Value Tests ====================

    @Test
    void testGetLatestValue_returnsNewest() {
        Metric metric = createMetric("cpu", MetricType.GAUGE, "host");
        MetricSeries latest = new MetricSeries();
        latest.setId(UUID.randomUUID());
        latest.setMetric(metric);
        latest.setTimestamp(Instant.parse("2026-02-20T10:30:00Z"));
        latest.setValue(85.0);
        latest.setResolution(60);

        MetricDataPointResponse dpResponse = new MetricDataPointResponse(
                latest.getId(), metric.getId(), latest.getTimestamp(), 85.0, null, 60);

        when(metricRepository.existsById(metric.getId())).thenReturn(true);
        when(metricSeriesRepository.findByMetricId(eq(metric.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(latest)));
        when(metricMapper.toDataPointResponse(latest)).thenReturn(dpResponse);

        Optional<MetricDataPointResponse> result = metricsService.getLatestValue(metric.getId());

        assertThat(result).isPresent();
        assertThat(result.get().value()).isCloseTo(85.0, within(0.001));
    }

    @Test
    void testGetLatestValue_noData_returnsEmpty() {
        UUID metricId = UUID.randomUUID();

        when(metricRepository.existsById(metricId)).thenReturn(true);
        when(metricSeriesRepository.findByMetricId(eq(metricId), any(Pageable.class)))
                .thenReturn(Page.empty());

        Optional<MetricDataPointResponse> result = metricsService.getLatestValue(metricId);

        assertThat(result).isEmpty();
    }

    // ==================== Purge Tests ====================

    @Test
    void testPurgeOldData_deletesBeforeCutoff() {
        Instant cutoff = Instant.parse("2026-02-10T00:00:00Z");

        when(metricSeriesRepository.count()).thenReturn(100L).thenReturn(80L);

        long deleted = metricsService.purgeOldData(cutoff);

        assertThat(deleted).isEqualTo(20);
        verify(metricSeriesRepository).deleteByTimestampBefore(cutoff);
    }
}
