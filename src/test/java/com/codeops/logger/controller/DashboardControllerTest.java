package com.codeops.logger.controller;

import com.codeops.logger.dto.response.DashboardResponse;
import com.codeops.logger.dto.response.DashboardWidgetResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.security.JwtTokenProvider;
import com.codeops.logger.service.DashboardService;
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
 * Controller tests for {@link DashboardController}.
 */
@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DASHBOARD_ID = UUID.randomUUID();
    private static final UUID WIDGET_ID = UUID.randomUUID();

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

    private DashboardResponse sampleDashboard() {
        return new DashboardResponse(DASHBOARD_ID, "My Dashboard", "desc", TEAM_ID, USER_ID,
                false, false, 30, null, List.of(), Instant.now(), Instant.now());
    }

    private DashboardWidgetResponse sampleWidget() {
        return new DashboardWidgetResponse(WIDGET_ID, DASHBOARD_ID, "CPU Chart", "LINE_CHART",
                "{}", "{}", 0, 0, 6, 4, 0, Instant.now(), Instant.now());
    }

    @Test
    void testCreateDashboard_success() throws Exception {
        when(dashboardService.createDashboard(any(), eq(TEAM_ID), eq(USER_ID))).thenReturn(sampleDashboard());

        mockMvc.perform(post("/api/v1/logger/dashboards")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "My Dashboard"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Dashboard"));
    }

    @Test
    void testCreateDashboard_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/logger/dashboards")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "no name"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetDashboardsByTeam_success() throws Exception {
        when(dashboardService.getDashboardsByTeam(TEAM_ID)).thenReturn(List.of(sampleDashboard()));

        mockMvc.perform(get("/api/v1/logger/dashboards")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My Dashboard"));
    }

    @Test
    void testGetDashboardsByTeamPaged_success() throws Exception {
        PageResponse<DashboardResponse> page = new PageResponse<>(List.of(sampleDashboard()), 0, 20, 1, 1, true);
        when(dashboardService.getDashboardsByTeamPaged(TEAM_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/logger/dashboards/paged")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetSharedDashboards_success() throws Exception {
        when(dashboardService.getSharedDashboards(TEAM_ID)).thenReturn(List.of(sampleDashboard()));

        mockMvc.perform(get("/api/v1/logger/dashboards/shared")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My Dashboard"));
    }

    @Test
    void testGetMyDashboards_success() throws Exception {
        when(dashboardService.getDashboardsByUser(USER_ID)).thenReturn(List.of(sampleDashboard()));

        mockMvc.perform(get("/api/v1/logger/dashboards/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdBy").value(USER_ID.toString()));
    }

    @Test
    void testGetDashboard_success() throws Exception {
        when(dashboardService.getDashboard(DASHBOARD_ID)).thenReturn(sampleDashboard());

        mockMvc.perform(get("/api/v1/logger/dashboards/{dashboardId}", DASHBOARD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DASHBOARD_ID.toString()));
    }

    @Test
    void testGetDashboard_notFound_returns404() throws Exception {
        when(dashboardService.getDashboard(DASHBOARD_ID)).thenThrow(new NotFoundException("Not found"));

        mockMvc.perform(get("/api/v1/logger/dashboards/{dashboardId}", DASHBOARD_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateDashboard_success() throws Exception {
        DashboardResponse updated = new DashboardResponse(DASHBOARD_ID, "Updated", null, TEAM_ID, USER_ID,
                false, false, 30, null, List.of(), Instant.now(), Instant.now());
        when(dashboardService.updateDashboard(eq(DASHBOARD_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/dashboards/{dashboardId}", DASHBOARD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void testDeleteDashboard_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/dashboards/{dashboardId}", DASHBOARD_ID))
                .andExpect(status().isNoContent());

        verify(dashboardService).deleteDashboard(DASHBOARD_ID);
    }

    @Test
    void testAddWidget_success() throws Exception {
        when(dashboardService.addWidget(eq(DASHBOARD_ID), any())).thenReturn(sampleWidget());

        mockMvc.perform(post("/api/v1/logger/dashboards/{dashboardId}/widgets", DASHBOARD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "CPU Chart",
                                "widgetType", "LINE_CHART"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("CPU Chart"));
    }

    @Test
    void testUpdateWidget_success() throws Exception {
        DashboardWidgetResponse updated = new DashboardWidgetResponse(WIDGET_ID, DASHBOARD_ID, "Updated",
                "LINE_CHART", "{}", "{}", 0, 0, 6, 4, 0, Instant.now(), Instant.now());
        when(dashboardService.updateWidget(eq(DASHBOARD_ID), eq(WIDGET_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/logger/dashboards/{dashboardId}/widgets/{widgetId}", DASHBOARD_ID, WIDGET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void testRemoveWidget_success() throws Exception {
        mockMvc.perform(delete("/api/v1/logger/dashboards/{dashboardId}/widgets/{widgetId}", DASHBOARD_ID, WIDGET_ID))
                .andExpect(status().isNoContent());

        verify(dashboardService).removeWidget(DASHBOARD_ID, WIDGET_ID);
    }

    @Test
    void testReorderWidgets_success() throws Exception {
        UUID w1 = UUID.randomUUID();
        UUID w2 = UUID.randomUUID();
        when(dashboardService.reorderWidgets(eq(DASHBOARD_ID), anyList())).thenReturn(sampleDashboard());

        mockMvc.perform(put("/api/v1/logger/dashboards/{dashboardId}/widgets/reorder", DASHBOARD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "widgetIds", List.of(w1.toString(), w2.toString())
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateLayout_success() throws Exception {
        when(dashboardService.updateLayout(eq(DASHBOARD_ID), anyList())).thenReturn(sampleDashboard());

        mockMvc.perform(put("/api/v1/logger/dashboards/{dashboardId}/layout", DASHBOARD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "positions", List.of(Map.of(
                                        "widgetId", WIDGET_ID.toString(),
                                        "gridX", 0,
                                        "gridY", 0,
                                        "gridWidth", 6,
                                        "gridHeight", 4
                                ))
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    void testMarkAsTemplate_success() throws Exception {
        DashboardResponse template = new DashboardResponse(DASHBOARD_ID, "Template", null, TEAM_ID, USER_ID,
                false, true, 30, null, List.of(), Instant.now(), Instant.now());
        when(dashboardService.markAsTemplate(DASHBOARD_ID)).thenReturn(template);

        mockMvc.perform(post("/api/v1/logger/dashboards/{dashboardId}/template", DASHBOARD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isTemplate").value(true));
    }

    @Test
    void testUnmarkAsTemplate_success() throws Exception {
        when(dashboardService.unmarkAsTemplate(DASHBOARD_ID)).thenReturn(sampleDashboard());

        mockMvc.perform(delete("/api/v1/logger/dashboards/{dashboardId}/template", DASHBOARD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isTemplate").value(false));
    }

    @Test
    void testGetTemplates_success() throws Exception {
        DashboardResponse template = new DashboardResponse(DASHBOARD_ID, "Template", null, TEAM_ID, USER_ID,
                false, true, 30, null, List.of(), Instant.now(), Instant.now());
        when(dashboardService.getTemplates(TEAM_ID)).thenReturn(List.of(template));

        mockMvc.perform(get("/api/v1/logger/dashboards/templates")
                        .header("X-Team-Id", TEAM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isTemplate").value(true));
    }

    @Test
    void testCreateFromTemplate_success() throws Exception {
        UUID templateId = UUID.randomUUID();
        DashboardResponse created = new DashboardResponse(UUID.randomUUID(), "From Template", null, TEAM_ID,
                USER_ID, false, false, 30, null, List.of(), Instant.now(), Instant.now());
        when(dashboardService.createFromTemplate(eq(templateId), eq("From Template"), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/logger/dashboards/from-template")
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "From Template",
                                "templateId", templateId.toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("From Template"));
    }

    @Test
    void testDuplicateDashboard_success() throws Exception {
        DashboardResponse dup = new DashboardResponse(UUID.randomUUID(), "Copy", null, TEAM_ID, USER_ID,
                false, false, 30, null, List.of(), Instant.now(), Instant.now());
        when(dashboardService.duplicateDashboard(eq(DASHBOARD_ID), eq("Copy"), eq(TEAM_ID), eq(USER_ID)))
                .thenReturn(dup);

        mockMvc.perform(post("/api/v1/logger/dashboards/{dashboardId}/duplicate", DASHBOARD_ID)
                        .header("X-Team-Id", TEAM_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Copy"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Copy"));
    }
}
