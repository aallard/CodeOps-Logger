package com.codeops.logger.controller;

import com.codeops.logger.dto.request.CreateSavedQueryRequest;
import com.codeops.logger.dto.request.DslQueryRequest;
import com.codeops.logger.dto.request.LogQueryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.QueryHistoryResponse;
import com.codeops.logger.dto.response.SavedQueryResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.LogQueryService;
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
 * Controller tests for {@link LogQueryController}.
 */
@WebMvcTest(LogQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
class LogQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogQueryService logQueryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID LOG_ID = UUID.randomUUID();
    private static final UUID QUERY_ID = UUID.randomUUID();

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
    void testQuery_success() throws Exception {
        PageResponse<LogEntryResponse> response = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(logQueryService.query(any(LogQueryRequest.class), eq(TEAM_ID), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(post("/api/v1/logger/logs/query")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("serviceName", "my-service"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testQuery_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/logs/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("serviceName", "svc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearch_success() throws Exception {
        PageResponse<LogEntryResponse> response = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(logQueryService.search(eq("error"), eq(TEAM_ID), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/logger/logs/search")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .param("q", "error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testSearch_missingQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/logger/logs/search")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExecuteDsl_success() throws Exception {
        PageResponse<LogEntryResponse> response = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(logQueryService.executeDsl(eq("level = ERROR"), eq(TEAM_ID), eq(USER_ID), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/logger/logs/dsl")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("query", "level = ERROR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testExecuteDsl_missingQuery_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/logs/dsl")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetLogEntry_success() throws Exception {
        LogEntryResponse response = new LogEntryResponse(LOG_ID, null, null, "INFO", "msg",
                Instant.now(), "svc", null, null, null, null, null, null, null, null, null, null, null, TEAM_ID, Instant.now());
        when(logQueryService.getLogEntry(LOG_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/logger/logs/{id}", LOG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOG_ID.toString()));
    }

    @Test
    void testGetLogEntry_notFound_returns404() throws Exception {
        when(logQueryService.getLogEntry(LOG_ID)).thenThrow(new NotFoundException("Log entry not found: " + LOG_ID));

        mockMvc.perform(get("/api/v1/logger/logs/{id}", LOG_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSaveQuery_success() throws Exception {
        SavedQueryResponse response = new SavedQueryResponse(QUERY_ID, "My Query", null, "{}", null,
                TEAM_ID, USER_ID, false, null, 0L, Instant.now(), Instant.now());
        when(logQueryService.saveQuery(any(CreateSavedQueryRequest.class), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/logger/logs/queries/saved")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "My Query",
                                "queryJson", "{}"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Query"));
    }

    @Test
    void testGetSavedQueries_success() throws Exception {
        SavedQueryResponse response = new SavedQueryResponse(QUERY_ID, "Q1", null, "{}", null,
                TEAM_ID, USER_ID, false, null, 0L, Instant.now(), Instant.now());
        when(logQueryService.getSavedQueries(TEAM_ID, USER_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/logger/logs/queries/saved")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Q1"));
    }

    @Test
    void testGetSavedQuery_success() throws Exception {
        SavedQueryResponse response = new SavedQueryResponse(QUERY_ID, "Q1", null, "{}", null,
                TEAM_ID, USER_ID, false, null, 0L, Instant.now(), Instant.now());
        when(logQueryService.getSavedQuery(QUERY_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/logger/logs/queries/saved/{queryId}", QUERY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUERY_ID.toString()));
    }

    @Test
    void testUpdateSavedQuery_success() throws Exception {
        SavedQueryResponse response = new SavedQueryResponse(QUERY_ID, "Updated", null, "{}", null,
                TEAM_ID, USER_ID, false, null, 0L, Instant.now(), Instant.now());
        when(logQueryService.updateSavedQuery(eq(QUERY_ID), any(), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(put("/api/v1/logger/logs/queries/saved/{queryId}", QUERY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void testDeleteSavedQuery_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/logs/queries/saved/{queryId}", QUERY_ID))
                .andExpect(status().isNoContent());

        verify(logQueryService).deleteSavedQuery(QUERY_ID, USER_ID);
    }

    @Test
    void testExecuteSavedQuery_success() throws Exception {
        PageResponse<LogEntryResponse> response = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(logQueryService.executeSavedQuery(eq(QUERY_ID), eq(TEAM_ID), eq(USER_ID), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/logger/logs/queries/saved/{queryId}/execute", QUERY_ID)
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetQueryHistory_success() throws Exception {
        PageResponse<QueryHistoryResponse> response = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(logQueryService.getQueryHistory(eq(USER_ID), eq(0), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/v1/logger/logs/queries/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
