package com.codeops.logger.controller;

import com.codeops.logger.dto.response.RetentionPolicyResponse;
import com.codeops.logger.dto.response.StorageUsageResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.RetentionExecutor;
import com.codeops.logger.service.RetentionService;
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
 * Controller tests for {@link RetentionController}.
 */
@WebMvcTest(RetentionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RetentionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RetentionService retentionService;

    @MockBean
    private RetentionExecutor retentionExecutor;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID POLICY_ID = UUID.randomUUID();

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

    private RetentionPolicyResponse samplePolicy() {
        return new RetentionPolicyResponse(POLICY_ID, "30-day cleanup", "my-service", "INFO",
                30, "DELETE", null, true, TEAM_ID, USER_ID, null, Instant.now(), Instant.now());
    }

    @Test
    void testCreatePolicy_success() throws Exception {
        when(retentionService.createPolicy(any(), eq(TEAM_ID), eq(USER_ID))).thenReturn(samplePolicy());

        mockMvc.perform(post("/api/v1/logger/retention/policies")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "30-day cleanup",
                                "retentionDays", 30,
                                "action", "DELETE"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("30-day cleanup"));
    }

    @Test
    void testCreatePolicy_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/retention/policies")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "retentionDays", 30,
                                "action", "DELETE"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetPoliciesByTeam_success() throws Exception {
        when(retentionService.getPoliciesByTeam(TEAM_ID)).thenReturn(List.of(samplePolicy()));

        mockMvc.perform(get("/api/v1/logger/retention/policies")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("30-day cleanup"));
    }

    @Test
    void testGetPolicy_success() throws Exception {
        when(retentionService.getPolicy(POLICY_ID, TEAM_ID)).thenReturn(samplePolicy());

        mockMvc.perform(get("/api/v1/logger/retention/policies/{policyId}", POLICY_ID)
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(POLICY_ID.toString()));
    }

    @Test
    void testGetPolicy_notFound_returns404() throws Exception {
        when(retentionService.getPolicy(POLICY_ID, TEAM_ID)).thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/logger/retention/policies/{policyId}", POLICY_ID)
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdatePolicy_success() throws Exception {
        RetentionPolicyResponse updated = new RetentionPolicyResponse(POLICY_ID, "Updated", null, null,
                60, "ARCHIVE", "/archive", true, TEAM_ID, USER_ID, null, Instant.now(), Instant.now());
        when(retentionService.updatePolicy(eq(POLICY_ID), any(), eq(TEAM_ID))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/retention/policies/{policyId}", POLICY_ID)
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void testDeletePolicy_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/retention/policies/{policyId}", POLICY_ID)
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isNoContent());

        verify(retentionService).deletePolicy(POLICY_ID, TEAM_ID);
    }

    @Test
    void testTogglePolicy_success() throws Exception {
        RetentionPolicyResponse toggled = new RetentionPolicyResponse(POLICY_ID, "Policy", null, null,
                30, "DELETE", null, false, TEAM_ID, USER_ID, null, Instant.now(), Instant.now());
        when(retentionService.togglePolicyActive(POLICY_ID, TEAM_ID, false)).thenReturn(toggled);

        mockMvc.perform(put("/api/v1/logger/retention/policies/{policyId}/toggle", POLICY_ID)
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void testExecutePolicy_success() throws Exception {
        mockMvc.perform(post("/api/v1/logger/retention/policies/{policyId}/execute", POLICY_ID)
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("executed"));

        verify(retentionExecutor).manualExecute(POLICY_ID, TEAM_ID);
    }

    @Test
    void testGetStorageUsage_success() throws Exception {
        StorageUsageResponse usage = new StorageUsageResponse(10000L, 5000L, 2000L,
                Map.of("my-service", 8000L), Map.of("INFO", 6000L, "ERROR", 1000L),
                3, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"));
        when(retentionService.getStorageUsage()).thenReturn(usage);

        mockMvc.perform(get("/api/v1/logger/retention/storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogEntries").value(10000));
    }

    @Test
    void testCreatePolicy_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/retention/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Policy",
                                "retentionDays", 30,
                                "action", "DELETE"
                        ))))
                .andExpect(status().isBadRequest());
    }
}
