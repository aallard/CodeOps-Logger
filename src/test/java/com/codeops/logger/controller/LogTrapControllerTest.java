package com.codeops.logger.controller;

import com.codeops.logger.dto.response.LogTrapResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.TrapTestResult;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.LogTrapService;
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
 * Controller tests for {@link LogTrapController}.
 */
@WebMvcTest(LogTrapController.class)
@AutoConfigureMockMvc(addFilters = false)
class LogTrapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogTrapService logTrapService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TRAP_ID = UUID.randomUUID();

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

    private LogTrapResponse sampleTrap() {
        return new LogTrapResponse(TRAP_ID, "Error Trap", "catches errors", "PATTERN",
                true, TEAM_ID, USER_ID, null, 0L, List.of(), Instant.now(), Instant.now());
    }

    @Test
    void testCreateTrap_success() throws Exception {
        when(logTrapService.createTrap(any(), eq(TEAM_ID), eq(USER_ID))).thenReturn(sampleTrap());

        mockMvc.perform(post("/api/v1/logger/traps")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Error Trap",
                                "trapType", "PATTERN",
                                "conditions", List.of(Map.of(
                                        "conditionType", "CONTAINS",
                                        "field", "message",
                                        "pattern", "error"
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Error Trap"));
    }

    @Test
    void testCreateTrap_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/traps")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trapType", "PATTERN",
                                "conditions", List.of(Map.of(
                                        "conditionType", "CONTAINS",
                                        "field", "message",
                                        "pattern", "error"
                                ))
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateTrap_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/traps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Trap",
                                "trapType", "PATTERN",
                                "conditions", List.of(Map.of(
                                        "conditionType", "CONTAINS",
                                        "field", "message",
                                        "pattern", "x"
                                ))
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetTraps_success() throws Exception {
        when(logTrapService.getTrapsByTeam(TEAM_ID)).thenReturn(List.of(sampleTrap()));

        mockMvc.perform(get("/api/v1/logger/traps")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Error Trap"));
    }

    @Test
    void testGetTrapsPaged_success() throws Exception {
        PageResponse<LogTrapResponse> page = new PageResponse<>(List.of(sampleTrap()), 0, 20, 1, 1, true);
        when(logTrapService.getTrapsByTeamPaged(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/traps/paged")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetTrap_success() throws Exception {
        when(logTrapService.getTrap(TRAP_ID)).thenReturn(sampleTrap());

        mockMvc.perform(get("/api/v1/logger/traps/{trapId}", TRAP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRAP_ID.toString()));
    }

    @Test
    void testGetTrap_notFound_returns404() throws Exception {
        when(logTrapService.getTrap(TRAP_ID)).thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/logger/traps/{trapId}", TRAP_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateTrap_success() throws Exception {
        LogTrapResponse updated = new LogTrapResponse(TRAP_ID, "Updated", null, "PATTERN",
                true, TEAM_ID, USER_ID, null, 0L, List.of(), Instant.now(), Instant.now());
        when(logTrapService.updateTrap(eq(TRAP_ID), any(), eq(USER_ID))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/traps/{trapId}", TRAP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void testDeleteTrap_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/traps/{trapId}", TRAP_ID))
                .andExpect(status().isNoContent());

        verify(logTrapService).deleteTrap(TRAP_ID);
    }

    @Test
    void testToggleTrap_success() throws Exception {
        LogTrapResponse toggled = new LogTrapResponse(TRAP_ID, "Trap", null, "PATTERN",
                false, TEAM_ID, USER_ID, null, 0L, List.of(), Instant.now(), Instant.now());
        when(logTrapService.toggleTrap(TRAP_ID)).thenReturn(toggled);

        mockMvc.perform(post("/api/v1/logger/traps/{trapId}/toggle", TRAP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void testTestTrap_success() throws Exception {
        TrapTestResult result = TrapTestResult.of(5, 100, List.of(), Instant.now().minusSeconds(3600), Instant.now());
        when(logTrapService.testTrap(eq(TRAP_ID), eq(24))).thenReturn(result);

        mockMvc.perform(post("/api/v1/logger/traps/{trapId}/test", TRAP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("hoursBack", 24))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(5))
                .andExpect(jsonPath("$.totalEvaluated").value(100));
    }

    @Test
    void testTestTrapDefinition_success() throws Exception {
        TrapTestResult result = TrapTestResult.of(3, 50, List.of(), Instant.now().minusSeconds(3600), Instant.now());
        when(logTrapService.testTrapDefinition(any(), eq(TEAM_ID), eq(24))).thenReturn(result);

        mockMvc.perform(post("/api/v1/logger/traps/test")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .param("hoursBack", "24")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Test Trap",
                                "trapType", "PATTERN",
                                "conditions", List.of(Map.of(
                                        "conditionType", "CONTAINS",
                                        "field", "message",
                                        "pattern", "error"
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchCount").value(3));
    }
}
