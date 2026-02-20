package com.codeops.logger.controller;

import com.codeops.logger.dto.response.AnomalyBaselineResponse;
import com.codeops.logger.dto.response.AnomalyCheckResponse;
import com.codeops.logger.dto.response.AnomalyReportResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.AnomalyDetectionService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link AnomalyController}.
 */
@WebMvcTest(AnomalyController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnomalyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnomalyDetectionService anomalyDetectionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BASELINE_ID = UUID.randomUUID();

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

    private AnomalyBaselineResponse sampleBaseline() {
        return new AnomalyBaselineResponse(BASELINE_ID, "my-service", "log_volume",
                100.0, 15.0, 168L, Instant.now().minusSeconds(604800), Instant.now(),
                2.0, true, TEAM_ID, Instant.now(), Instant.now(), Instant.now());
    }

    @Test
    void testCreateOrUpdateBaseline_success() throws Exception {
        when(anomalyDetectionService.createOrUpdateBaseline(any(), eq(TEAM_ID))).thenReturn(sampleBaseline());

        mockMvc.perform(post("/api/v1/logger/anomalies/baselines")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceName", "my-service",
                                "metricName", "log_volume"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceName").value("my-service"));
    }

    @Test
    void testCreateOrUpdateBaseline_missingServiceName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/anomalies/baselines")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "metricName", "log_volume"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetBaselinesByTeam_success() throws Exception {
        when(anomalyDetectionService.getBaselinesByTeam(TEAM_ID)).thenReturn(List.of(sampleBaseline()));

        mockMvc.perform(get("/api/v1/logger/anomalies/baselines")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricName").value("log_volume"));
    }

    @Test
    void testGetBaselinesByService_success() throws Exception {
        when(anomalyDetectionService.getBaselinesByService(TEAM_ID, "my-service"))
                .thenReturn(List.of(sampleBaseline()));

        mockMvc.perform(get("/api/v1/logger/anomalies/baselines/service/{serviceName}", "my-service")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("my-service"));
    }

    @Test
    void testGetBaseline_success() throws Exception {
        when(anomalyDetectionService.getBaseline(BASELINE_ID)).thenReturn(sampleBaseline());

        mockMvc.perform(get("/api/v1/logger/anomalies/baselines/{baselineId}", BASELINE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BASELINE_ID.toString()));
    }

    @Test
    void testGetBaseline_notFound_returns404() throws Exception {
        when(anomalyDetectionService.getBaseline(BASELINE_ID)).thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/logger/anomalies/baselines/{baselineId}", BASELINE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBaseline_success() throws Exception {
        AnomalyBaselineResponse updated = new AnomalyBaselineResponse(BASELINE_ID, "my-service", "log_volume",
                100.0, 15.0, 168L, Instant.now().minusSeconds(604800), Instant.now(),
                3.0, true, TEAM_ID, Instant.now(), Instant.now(), Instant.now());
        when(anomalyDetectionService.updateBaseline(eq(BASELINE_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/anomalies/baselines/{baselineId}", BASELINE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("deviationThreshold", 3.0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviationThreshold").value(3.0));
    }

    @Test
    void testDeleteBaseline_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/anomalies/baselines/{baselineId}", BASELINE_ID))
                .andExpect(status().isNoContent());

        verify(anomalyDetectionService).deleteBaseline(BASELINE_ID);
    }

    @Test
    void testCheckAnomaly_success() throws Exception {
        AnomalyCheckResponse check = new AnomalyCheckResponse("my-service", "log_volume",
                150.0, 100.0, 15.0, 2.0, 3.33, true, "ABOVE", Instant.now());
        when(anomalyDetectionService.checkAnomaly(TEAM_ID, "my-service", "log_volume")).thenReturn(check);

        mockMvc.perform(get("/api/v1/logger/anomalies/check")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .param("serviceName", "my-service")
                        .param("metricName", "log_volume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAnomaly").value(true))
                .andExpect(jsonPath("$.direction").value("ABOVE"));
    }

    @Test
    void testRunFullCheck_success() throws Exception {
        AnomalyReportResponse report = new AnomalyReportResponse(TEAM_ID, Instant.now(),
                5, 1, List.of(), List.of());
        when(anomalyDetectionService.runFullCheck(TEAM_ID)).thenReturn(report);

        mockMvc.perform(get("/api/v1/logger/anomalies/report")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBaselines").value(5))
                .andExpect(jsonPath("$.anomaliesDetected").value(1));
    }

    @Test
    void testCreateOrUpdateBaseline_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/anomalies/baselines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceName", "my-service",
                                "metricName", "log_volume"
                        ))))
                .andExpect(status().isBadRequest());
    }
}
