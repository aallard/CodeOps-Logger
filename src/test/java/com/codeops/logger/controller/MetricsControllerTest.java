package com.codeops.logger.controller;

import com.codeops.logger.dto.response.MetricAggregationResponse;
import com.codeops.logger.dto.response.MetricDataPointResponse;
import com.codeops.logger.dto.response.MetricResponse;
import com.codeops.logger.dto.response.MetricTimeSeriesResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.ServiceMetricsSummaryResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link MetricsController}.
 */
@WebMvcTest(MetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID METRIC_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, "test@test.com",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MetricResponse sampleMetric() {
        return new MetricResponse(METRIC_ID, "cpu.usage", "GAUGE", "CPU usage", "%",
                "my-service", null, TEAM_ID, Instant.now(), Instant.now());
    }

    @Test
    void testRegisterMetric_success() throws Exception {
        when(metricsService.registerMetric(any(), eq(TEAM_ID))).thenReturn(sampleMetric());

        mockMvc.perform(post("/api/v1/logger/metrics")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "cpu.usage",
                                "metricType", "GAUGE",
                                "serviceName", "my-service"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("cpu.usage"));
    }

    @Test
    void testRegisterMetric_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/metrics")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "metricType", "GAUGE",
                                "serviceName", "svc"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetMetricsByTeam_success() throws Exception {
        when(metricsService.getMetricsByTeam(TEAM_ID)).thenReturn(List.of(sampleMetric()));

        mockMvc.perform(get("/api/v1/logger/metrics")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("cpu.usage"));
    }

    @Test
    void testGetMetricsByTeamPaged_success() throws Exception {
        PageResponse<MetricResponse> page = new PageResponse<>(List.of(sampleMetric()), 0, 20, 1, 1, true);
        when(metricsService.getMetricsByTeamPaged(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/metrics/paged")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetMetricsByService_success() throws Exception {
        when(metricsService.getMetricsByService(TEAM_ID, "my-service")).thenReturn(List.of(sampleMetric()));

        mockMvc.perform(get("/api/v1/logger/metrics/service/{serviceName}", "my-service")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("my-service"));
    }

    @Test
    void testGetServiceMetricsSummary_success() throws Exception {
        ServiceMetricsSummaryResponse summary = new ServiceMetricsSummaryResponse(
                "my-service", 1, Map.of("GAUGE", 1L), List.of(sampleMetric()));
        when(metricsService.getServiceMetricsSummary(TEAM_ID, "my-service")).thenReturn(summary);

        mockMvc.perform(get("/api/v1/logger/metrics/service/{serviceName}/summary", "my-service")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricCount").value(1));
    }

    @Test
    void testGetMetric_success() throws Exception {
        when(metricsService.getMetric(METRIC_ID)).thenReturn(sampleMetric());

        mockMvc.perform(get("/api/v1/logger/metrics/{metricId}", METRIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(METRIC_ID.toString()));
    }

    @Test
    void testGetMetric_notFound_returns404() throws Exception {
        when(metricsService.getMetric(METRIC_ID)).thenThrow(new NotFoundException("Metric not found"));

        mockMvc.perform(get("/api/v1/logger/metrics/{metricId}", METRIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateMetric_success() throws Exception {
        MetricResponse updated = new MetricResponse(METRIC_ID, "cpu.usage", "GAUGE", "Updated", "%",
                "my-service", null, TEAM_ID, Instant.now(), Instant.now());
        when(metricsService.updateMetric(eq(METRIC_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/metrics/{metricId}", METRIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void testDeleteMetric_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/metrics/{metricId}", METRIC_ID))
                .andExpect(status().isNoContent());

        verify(metricsService).deleteMetric(METRIC_ID);
    }

    @Test
    void testPushMetricData_success() throws Exception {
        when(metricsService.pushMetricData(any(), eq(TEAM_ID))).thenReturn(2);

        mockMvc.perform(post("/api/v1/logger/metrics/data")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "metricId", METRIC_ID.toString(),
                                "dataPoints", List.of(
                                        Map.of("timestamp", Instant.now().toString(), "value", 42.5),
                                        Map.of("timestamp", Instant.now().toString(), "value", 43.0)
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingested").value(2));
    }

    @Test
    void testGetTimeSeries_success() throws Exception {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-02T00:00:00Z");
        MetricTimeSeriesResponse ts = new MetricTimeSeriesResponse(METRIC_ID, "cpu.usage",
                "my-service", "GAUGE", "%", start, end, null, List.of());
        when(metricsService.getTimeSeries(METRIC_ID, start, end)).thenReturn(ts);

        mockMvc.perform(get("/api/v1/logger/metrics/{metricId}/timeseries", METRIC_ID)
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricName").value("cpu.usage"));
    }

    @Test
    void testGetTimeSeriesAggregated_success() throws Exception {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-02T00:00:00Z");
        MetricTimeSeriesResponse ts = new MetricTimeSeriesResponse(METRIC_ID, "cpu.usage",
                "my-service", "GAUGE", "%", start, end, 60, List.of());
        when(metricsService.getTimeSeriesAggregated(METRIC_ID, start, end, 60)).thenReturn(ts);

        mockMvc.perform(get("/api/v1/logger/metrics/{metricId}/timeseries/aggregated", METRIC_ID)
                        .param("startTime", start.toString())
                        .param("endTime", end.toString())
                        .param("resolution", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolution").value(60));
    }

    @Test
    void testGetAggregation_success() throws Exception {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-02T00:00:00Z");
        MetricAggregationResponse agg = new MetricAggregationResponse(METRIC_ID, "cpu.usage",
                "my-service", start, end, 100, 4200.0, 42.0, 10.0, 95.0, 40.0, 90.0, 94.0, 5.0);
        when(metricsService.getAggregation(METRIC_ID, start, end)).thenReturn(agg);

        mockMvc.perform(get("/api/v1/logger/metrics/{metricId}/aggregation", METRIC_ID)
                        .param("startTime", start.toString())
                        .param("endTime", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avg").value(42.0));
    }

    @Test
    void testGetLatestValue_found() throws Exception {
        MetricDataPointResponse dp = new MetricDataPointResponse(UUID.randomUUID(), METRIC_ID,
                Instant.now(), 42.5, null, null);
        when(metricsService.getLatestValue(METRIC_ID)).thenReturn(Optional.of(dp));

        mockMvc.perform(get("/api/v1/logger/metrics/{metricId}/latest", METRIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(42.5));
    }

    @Test
    void testGetLatestValue_empty_returns204() throws Exception {
        when(metricsService.getLatestValue(METRIC_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/logger/metrics/{metricId}/latest", METRIC_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetLatestValuesByService_success() throws Exception {
        when(metricsService.getLatestValuesByService(TEAM_ID, "my-service"))
                .thenReturn(Map.of("cpu.usage", 42.5));

        mockMvc.perform(get("/api/v1/logger/metrics/service/{serviceName}/latest", "my-service")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['cpu.usage']").value(42.5));
    }

    @Test
    void testRegisterMetric_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "cpu.usage",
                                "metricType", "GAUGE",
                                "serviceName", "svc"
                        ))))
                .andExpect(status().isBadRequest());
    }
}
