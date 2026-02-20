package com.codeops.logger.controller;

import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.LogIngestionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link LogIngestionController}.
 */
@WebMvcTest(LogIngestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class LogIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogIngestionService logIngestionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID LOG_ID = UUID.randomUUID();

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

    @Test
    void testIngest_success() throws Exception {
        LogEntryResponse response = new LogEntryResponse(LOG_ID, null, null, "INFO", "Test message",
                Instant.now(), "my-service", null, null, null, null, null, null, null, null, null, null, null, TEAM_ID, Instant.now());

        when(logIngestionService.ingest(any(IngestLogEntryRequest.class), eq(TEAM_ID))).thenReturn(response);

        mockMvc.perform(post("/api/v1/logger/logs")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "level", "INFO",
                                "message", "Test message",
                                "serviceName", "my-service"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(LOG_ID.toString()))
                .andExpect(jsonPath("$.level").value("INFO"));
    }

    @Test
    void testIngest_missingTeamIdHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "level", "INFO",
                                "message", "Test",
                                "serviceName", "svc"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIngest_invalidTeamIdHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/logs")
                        .header("X-Team-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "level", "INFO",
                                "message", "Test",
                                "serviceName", "svc"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIngest_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/logs")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "level", "INFO"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIngestBatch_success() throws Exception {
        when(logIngestionService.ingestBatch(any(), eq(TEAM_ID))).thenReturn(2);

        mockMvc.perform(post("/api/v1/logger/logs/batch")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("level", "INFO", "message", "msg1", "serviceName", "svc"),
                                        Map.of("level", "ERROR", "message", "msg2", "serviceName", "svc")
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingested").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void testIngestBatch_emptyEntries_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/logs/batch")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "entries", List.of()
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIngestBatch_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/logs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "entries", List.of(
                                        Map.of("level", "INFO", "message", "msg", "serviceName", "svc")
                                )
                        ))))
                .andExpect(status().isBadRequest());
    }
}
