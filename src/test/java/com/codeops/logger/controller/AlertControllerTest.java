package com.codeops.logger.controller;

import com.codeops.logger.dto.response.AlertChannelResponse;
import com.codeops.logger.dto.response.AlertHistoryResponse;
import com.codeops.logger.dto.response.AlertRuleResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.AlertChannelService;
import com.codeops.logger.service.AlertService;
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
 * Controller tests for {@link AlertController}.
 */
@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @MockBean
    private AlertChannelService alertChannelService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID RULE_ID = UUID.randomUUID();
    private static final UUID TRAP_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();

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

    // ==================== Channel Tests ====================

    @Test
    void testCreateChannel_success() throws Exception {
        AlertChannelResponse response = new AlertChannelResponse(CHANNEL_ID, "Slack Channel", "SLACK",
                "{}", true, TEAM_ID, USER_ID, Instant.now(), Instant.now());
        when(alertChannelService.createChannel(any(), eq(TEAM_ID), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(post("/api/v1/logger/alerts/channels")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Slack Channel",
                                "channelType", "SLACK",
                                "configuration", "{\"webhook_url\":\"https://hooks.slack.com/test\"}"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Slack Channel"));
    }

    @Test
    void testCreateChannel_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/alerts/channels")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "channelType", "SLACK",
                                "configuration", "{}"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetChannels_success() throws Exception {
        AlertChannelResponse response = new AlertChannelResponse(CHANNEL_ID, "Ch1", "EMAIL",
                "{}", true, TEAM_ID, USER_ID, Instant.now(), Instant.now());
        when(alertChannelService.getChannelsByTeam(TEAM_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/logger/alerts/channels")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ch1"));
    }

    @Test
    void testGetChannelsPaged_success() throws Exception {
        AlertChannelResponse response = new AlertChannelResponse(CHANNEL_ID, "Ch1", "EMAIL",
                "{}", true, TEAM_ID, USER_ID, Instant.now(), Instant.now());
        PageResponse<AlertChannelResponse> page = new PageResponse<>(List.of(response), 0, 20, 1, 1, true);
        when(alertChannelService.getChannelsByTeamPaged(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/alerts/channels/paged")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetChannel_success() throws Exception {
        AlertChannelResponse response = new AlertChannelResponse(CHANNEL_ID, "Ch1", "EMAIL",
                "{}", true, TEAM_ID, USER_ID, Instant.now(), Instant.now());
        when(alertChannelService.getChannel(CHANNEL_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/logger/alerts/channels/{channelId}", CHANNEL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CHANNEL_ID.toString()));
    }

    @Test
    void testGetChannel_notFound_returns404() throws Exception {
        when(alertChannelService.getChannel(CHANNEL_ID)).thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/logger/alerts/channels/{channelId}", CHANNEL_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateChannel_success() throws Exception {
        AlertChannelResponse updated = new AlertChannelResponse(CHANNEL_ID, "Updated", "EMAIL",
                "{}", true, TEAM_ID, USER_ID, Instant.now(), Instant.now());
        when(alertChannelService.updateChannel(eq(CHANNEL_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/alerts/channels/{channelId}", CHANNEL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void testDeleteChannel_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/alerts/channels/{channelId}", CHANNEL_ID))
                .andExpect(status().isNoContent());

        verify(alertChannelService).deleteChannel(CHANNEL_ID);
    }

    // ==================== Rule Tests ====================

    @Test
    void testCreateRule_success() throws Exception {
        AlertRuleResponse response = new AlertRuleResponse(RULE_ID, "Rule1", TRAP_ID, "TrapName",
                CHANNEL_ID, "ChannelName", "CRITICAL", true, 5, TEAM_ID, Instant.now(), Instant.now());
        when(alertService.createRule(any(), eq(TEAM_ID))).thenReturn(response);

        mockMvc.perform(post("/api/v1/logger/alerts/rules")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Rule1",
                                "trapId", TRAP_ID.toString(),
                                "channelId", CHANNEL_ID.toString(),
                                "severity", "CRITICAL"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rule1"));
    }

    @Test
    void testCreateRule_missingTeamId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/alerts/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Rule1",
                                "trapId", TRAP_ID.toString(),
                                "channelId", CHANNEL_ID.toString(),
                                "severity", "CRITICAL"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRules_success() throws Exception {
        AlertRuleResponse response = new AlertRuleResponse(RULE_ID, "R1", TRAP_ID, "T",
                CHANNEL_ID, "C", "INFO", true, 5, TEAM_ID, Instant.now(), Instant.now());
        when(alertService.getRulesByTeam(TEAM_ID)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/logger/alerts/rules")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("R1"));
    }

    @Test
    void testGetRulesPaged_success() throws Exception {
        AlertRuleResponse response = new AlertRuleResponse(RULE_ID, "R1", TRAP_ID, "T",
                CHANNEL_ID, "C", "INFO", true, 5, TEAM_ID, Instant.now(), Instant.now());
        PageResponse<AlertRuleResponse> page = new PageResponse<>(List.of(response), 0, 20, 1, 1, true);
        when(alertService.getRulesByTeamPaged(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/alerts/rules/paged")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetRule_success() throws Exception {
        AlertRuleResponse response = new AlertRuleResponse(RULE_ID, "R1", TRAP_ID, "T",
                CHANNEL_ID, "C", "INFO", true, 5, TEAM_ID, Instant.now(), Instant.now());
        when(alertService.getRule(RULE_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/logger/alerts/rules/{ruleId}", RULE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RULE_ID.toString()));
    }

    @Test
    void testUpdateRule_success() throws Exception {
        AlertRuleResponse updated = new AlertRuleResponse(RULE_ID, "Updated", TRAP_ID, "T",
                CHANNEL_ID, "C", "WARNING", true, 10, TEAM_ID, Instant.now(), Instant.now());
        when(alertService.updateRule(eq(RULE_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/alerts/rules/{ruleId}", RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void testDeleteRule_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/alerts/rules/{ruleId}", RULE_ID))
                .andExpect(status().isNoContent());

        verify(alertService).deleteRule(RULE_ID);
    }

    // ==================== Alert History Tests ====================

    @Test
    void testGetAlertHistory_success() throws Exception {
        PageResponse<AlertHistoryResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(alertService.getAlertHistory(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/alerts/history")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetAlertHistoryByStatus_success() throws Exception {
        PageResponse<AlertHistoryResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(alertService.getAlertHistoryByStatus(eq(TEAM_ID), any(), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/alerts/history/status/FIRED")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAlertHistoryByStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/logger/alerts/history/status/INVALID")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAlertHistoryBySeverity_success() throws Exception {
        PageResponse<AlertHistoryResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true);
        when(alertService.getAlertHistoryBySeverity(eq(TEAM_ID), any(), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/alerts/history/severity/CRITICAL")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAlertHistoryBySeverity_invalidSeverity_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/logger/alerts/history/severity/INVALID")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateAlertStatus_success() throws Exception {
        AlertHistoryResponse response = new AlertHistoryResponse(ALERT_ID, RULE_ID, "R1",
                TRAP_ID, "T1", CHANNEL_ID, "C1", "CRITICAL", "ACKNOWLEDGED", "msg",
                USER_ID, Instant.now(), null, null, TEAM_ID, Instant.now());
        when(alertService.updateAlertStatus(eq(ALERT_ID), any(), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(put("/api/v1/logger/alerts/history/{alertId}/status", ALERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACKNOWLEDGED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    void testUpdateAlertStatus_missingStatus_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/logger/alerts/history/{alertId}/status", ALERT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetActiveAlertCounts_success() throws Exception {
        Map<String, Long> counts = Map.of("CRITICAL", 5L, "WARNING", 3L, "INFO", 1L);
        when(alertService.getActiveAlertCounts(TEAM_ID)).thenReturn(counts);

        mockMvc.perform(get("/api/v1/logger/alerts/active-counts")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CRITICAL").value(5));
    }
}
