package com.codeops.logger.controller;

import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.RootCauseAnalysisResponse;
import com.codeops.logger.dto.response.TraceFlowResponse;
import com.codeops.logger.dto.response.TraceListResponse;
import com.codeops.logger.dto.response.TraceSpanResponse;
import com.codeops.logger.dto.response.TraceWaterfallResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.TraceService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link TraceController}.
 */
@WebMvcTest(TraceController.class)
@AutoConfigureMockMvc(addFilters = false)
class TraceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TraceService traceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SPAN_ID = UUID.randomUUID();
    private static final String CORRELATION_ID = "corr-123";
    private static final String TRACE_ID = "trace-456";

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

    private TraceSpanResponse sampleSpan() {
        return new TraceSpanResponse(SPAN_ID, CORRELATION_ID, TRACE_ID, "span-1", null,
                "my-service", "GET /api", Instant.now(), Instant.now(), 100L,
                "OK", null, null, TEAM_ID, Instant.now());
    }

    @Test
    void testCreateSpan_success() throws Exception {
        when(traceService.createSpan(any(), eq(TEAM_ID))).thenReturn(sampleSpan());

        mockMvc.perform(post("/api/v1/logger/traces/spans")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "correlationId", CORRELATION_ID,
                                "traceId", TRACE_ID,
                                "spanId", "span-1",
                                "serviceName", "my-service",
                                "operationName", "GET /api",
                                "startTime", Instant.now().toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationName").value("GET /api"));
    }

    @Test
    void testCreateSpan_missingCorrelationId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/traces/spans")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "traceId", TRACE_ID,
                                "spanId", "span-1",
                                "serviceName", "my-service",
                                "operationName", "GET /api",
                                "startTime", Instant.now().toString()
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSpanBatch_success() throws Exception {
        when(traceService.createSpanBatch(anyList(), eq(TEAM_ID))).thenReturn(List.of(sampleSpan()));

        mockMvc.perform(post("/api/v1/logger/traces/spans/batch")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(
                                Map.of(
                                        "correlationId", CORRELATION_ID,
                                        "traceId", TRACE_ID,
                                        "spanId", "span-1",
                                        "serviceName", "my-service",
                                        "operationName", "GET /api",
                                        "startTime", Instant.now().toString()
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1));
    }

    @Test
    void testGetSpan_success() throws Exception {
        when(traceService.getSpan(SPAN_ID)).thenReturn(sampleSpan());

        mockMvc.perform(get("/api/v1/logger/traces/spans/{spanId}", SPAN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SPAN_ID.toString()));
    }

    @Test
    void testGetSpan_notFound_returns404() throws Exception {
        when(traceService.getSpan(SPAN_ID)).thenThrow(new NotFoundException("Span not found"));

        mockMvc.perform(get("/api/v1/logger/traces/spans/{spanId}", SPAN_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTraceFlow_success() throws Exception {
        TraceFlowResponse flow = new TraceFlowResponse(CORRELATION_ID, TRACE_ID,
                List.of(sampleSpan()), 100L, 1, false);
        when(traceService.getTraceFlow(CORRELATION_ID)).thenReturn(flow);

        mockMvc.perform(get("/api/v1/logger/traces/flow/{correlationId}", CORRELATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    void testGetTraceFlowByTraceId_success() throws Exception {
        TraceFlowResponse flow = new TraceFlowResponse(CORRELATION_ID, TRACE_ID,
                List.of(sampleSpan()), 100L, 1, false);
        when(traceService.getTraceFlowByTraceId(TRACE_ID)).thenReturn(flow);

        mockMvc.perform(get("/api/v1/logger/traces/flow/by-trace-id/{traceId}", TRACE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void testGetWaterfall_success() throws Exception {
        TraceWaterfallResponse waterfall = new TraceWaterfallResponse(CORRELATION_ID, TRACE_ID,
                100L, 1, 1, false, List.of());
        when(traceService.getWaterfall(CORRELATION_ID)).thenReturn(waterfall);

        mockMvc.perform(get("/api/v1/logger/traces/waterfall/{correlationId}", CORRELATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    void testGetRootCauseAnalysis_found() throws Exception {
        RootCauseAnalysisResponse rca = new RootCauseAnalysisResponse(CORRELATION_ID, TRACE_ID,
                sampleSpan(), "my-service", "NullPointerException", List.of(sampleSpan()),
                List.of(), 1, 100L);
        when(traceService.getRootCauseAnalysis(CORRELATION_ID)).thenReturn(Optional.of(rca));

        mockMvc.perform(get("/api/v1/logger/traces/rca/{correlationId}", CORRELATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootCauseService").value("my-service"));
    }

    @Test
    void testGetRootCauseAnalysis_noErrors_returns204() throws Exception {
        when(traceService.getRootCauseAnalysis(CORRELATION_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/logger/traces/rca/{correlationId}", CORRELATION_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void testListRecentTraces_success() throws Exception {
        TraceListResponse trace = new TraceListResponse(CORRELATION_ID, TRACE_ID, "my-service",
                "GET /api", 1, 1, 100L, false, Instant.now(), Instant.now());
        PageResponse<TraceListResponse> page = new PageResponse<>(List.of(trace), 0, 20, 1, 1, true);
        when(traceService.listRecentTraces(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/traces")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testListTracesByService_success() throws Exception {
        TraceListResponse trace = new TraceListResponse(CORRELATION_ID, TRACE_ID, "my-service",
                "GET /api", 1, 1, 100L, false, Instant.now(), Instant.now());
        PageResponse<TraceListResponse> page = new PageResponse<>(List.of(trace), 0, 20, 1, 1, true);
        when(traceService.listTracesByService(TEAM_ID, "my-service", 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/traces/service/{serviceName}", "my-service")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rootService").value("my-service"));
    }

    @Test
    void testListErrorTraces_success() throws Exception {
        TraceListResponse trace = new TraceListResponse(CORRELATION_ID, TRACE_ID, "my-service",
                "GET /api", 1, 1, 100L, true, Instant.now(), Instant.now());
        when(traceService.listErrorTraces(TEAM_ID, 20)).thenReturn(List.of(trace));

        mockMvc.perform(get("/api/v1/logger/traces/errors")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hasErrors").value(true));
    }

    @Test
    void testGetRelatedLogEntries_success() throws Exception {
        UUID logId = UUID.randomUUID();
        when(traceService.getRelatedLogEntries(CORRELATION_ID)).thenReturn(List.of(logId));

        mockMvc.perform(get("/api/v1/logger/traces/{correlationId}/logs", CORRELATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(logId.toString()));
    }
}
