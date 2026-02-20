package com.codeops.logger.controller;

import com.codeops.logger.dto.response.LogSourceResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.LogSourceService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link LogSourceController}.
 */
@WebMvcTest(LogSourceController.class)
@AutoConfigureMockMvc(addFilters = false)
class LogSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogSourceService logSourceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SOURCE_ID = UUID.randomUUID();

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

    private LogSourceResponse sampleResponse() {
        return new LogSourceResponse(SOURCE_ID, "my-service", null, "Test service",
                "production", true, TEAM_ID, null, 0L, Instant.now(), Instant.now());
    }

    @Test
    void testCreateSource_success() throws Exception {
        when(logSourceService.createSource(any(), eq(TEAM_ID))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/logger/sources")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "my-service",
                                "description", "Test service",
                                "environment", "production"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("my-service"));
    }

    @Test
    void testCreateSource_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/sources")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "test"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSource_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "svc"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSources_success() throws Exception {
        when(logSourceService.getSourcesByTeam(TEAM_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/logger/sources")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("my-service"));
    }

    @Test
    void testGetSourcesPaged_success() throws Exception {
        PageResponse<LogSourceResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1, true);
        when(logSourceService.getSourcesByTeamPaged(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/sources/paged")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetSource_success() throws Exception {
        when(logSourceService.getSource(SOURCE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/logger/sources/{sourceId}", SOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SOURCE_ID.toString()));
    }

    @Test
    void testGetSource_notFound_returns404() throws Exception {
        when(logSourceService.getSource(SOURCE_ID)).thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/logger/sources/{sourceId}", SOURCE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateSource_success() throws Exception {
        LogSourceResponse updated = new LogSourceResponse(SOURCE_ID, "updated-name", null, null,
                null, true, TEAM_ID, null, 0L, Instant.now(), Instant.now());
        when(logSourceService.updateSource(eq(SOURCE_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/sources/{sourceId}", SOURCE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "updated-name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated-name"));
    }

    @Test
    void testDeleteSource_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/sources/{sourceId}", SOURCE_ID))
                .andExpect(status().isNoContent());

        verify(logSourceService).deleteSource(SOURCE_ID);
    }

    @Test
    void testDeleteSource_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new NotFoundException("Not found")).when(logSourceService).deleteSource(SOURCE_ID);

        mockMvc.perform(delete("/api/v1/logger/sources/{sourceId}", SOURCE_ID))
                .andExpect(status().isNotFound());
    }
}
