package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateDashboardRequest;
import com.codeops.logger.dto.request.CreateDashboardWidgetRequest;
import com.codeops.logger.dto.request.CreateFromTemplateRequest;
import com.codeops.logger.dto.request.ReorderWidgetsRequest;
import com.codeops.logger.dto.request.UpdateDashboardRequest;
import com.codeops.logger.dto.request.UpdateDashboardWidgetRequest;
import com.codeops.logger.dto.request.UpdateLayoutRequest;
import com.codeops.logger.dto.response.DashboardResponse;
import com.codeops.logger.dto.response.DashboardWidgetResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for dashboard management.
 * Supports dashboard CRUD, widget management, layouts, templates, and duplication.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Dashboard CRUD, widget management, templates, and layouts")
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    /**
     * Creates a new dashboard.
     *
     * @param request     the dashboard creation data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created dashboard
     */
    @PostMapping
    @Operation(summary = "Create a dashboard")
    public ResponseEntity<DashboardResponse> createDashboard(
            @Valid @RequestBody CreateDashboardRequest request,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        DashboardResponse response = dashboardService.createDashboard(request, teamId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all dashboards for the specified team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return the list of dashboards
     */
    @GetMapping
    @Operation(summary = "List all dashboards for a team")
    public ResponseEntity<List<DashboardResponse>> getDashboardsByTeam(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(dashboardService.getDashboardsByTeam(teamId));
    }

    /**
     * Lists dashboards for the specified team with pagination.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param page        zero-based page index
     * @param size        page size
     * @return the paginated dashboard list
     */
    @GetMapping("/paged")
    @Operation(summary = "List dashboards with pagination")
    public ResponseEntity<PageResponse<DashboardResponse>> getDashboardsByTeamPaged(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(dashboardService.getDashboardsByTeamPaged(teamId, page, size));
    }

    /**
     * Lists shared dashboards for a team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return the list of shared dashboards
     */
    @GetMapping("/shared")
    @Operation(summary = "List shared dashboards for a team")
    public ResponseEntity<List<DashboardResponse>> getSharedDashboards(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(dashboardService.getSharedDashboards(teamId));
    }

    /**
     * Lists dashboards created by the current user.
     *
     * @return the list of user's dashboards
     */
    @GetMapping("/mine")
    @Operation(summary = "List dashboards owned by the current user")
    public ResponseEntity<List<DashboardResponse>> getMyDashboards() {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getDashboardsByUser(userId));
    }

    /**
     * Retrieves a single dashboard by its ID.
     *
     * @param dashboardId the dashboard UUID
     * @return the dashboard details with widgets
     */
    @GetMapping("/{dashboardId}")
    @Operation(summary = "Get a dashboard by ID")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable UUID dashboardId) {
        return ResponseEntity.ok(dashboardService.getDashboard(dashboardId));
    }

    /**
     * Updates a dashboard's mutable fields.
     *
     * @param dashboardId the dashboard UUID
     * @param request     the update data
     * @return the updated dashboard
     */
    @PutMapping("/{dashboardId}")
    @Operation(summary = "Update a dashboard")
    public ResponseEntity<DashboardResponse> updateDashboard(
            @PathVariable UUID dashboardId,
            @Valid @RequestBody UpdateDashboardRequest request) {
        return ResponseEntity.ok(dashboardService.updateDashboard(dashboardId, request));
    }

    /**
     * Deletes a dashboard and all its widgets.
     *
     * @param dashboardId the dashboard UUID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{dashboardId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a dashboard")
    public ResponseEntity<Void> deleteDashboard(@PathVariable UUID dashboardId) {
        dashboardService.deleteDashboard(dashboardId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Widget Endpoints ====================

    /**
     * Adds a widget to a dashboard.
     *
     * @param dashboardId the dashboard UUID
     * @param request     the widget creation data
     * @return the created widget
     */
    @PostMapping("/{dashboardId}/widgets")
    @Operation(summary = "Add a widget to a dashboard")
    public ResponseEntity<DashboardWidgetResponse> addWidget(
            @PathVariable UUID dashboardId,
            @Valid @RequestBody CreateDashboardWidgetRequest request) {
        DashboardWidgetResponse response = dashboardService.addWidget(dashboardId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a widget within a dashboard.
     *
     * @param dashboardId the dashboard UUID
     * @param widgetId    the widget UUID
     * @param request     the update data
     * @return the updated widget
     */
    @PutMapping("/{dashboardId}/widgets/{widgetId}")
    @Operation(summary = "Update a widget")
    public ResponseEntity<DashboardWidgetResponse> updateWidget(
            @PathVariable UUID dashboardId,
            @PathVariable UUID widgetId,
            @Valid @RequestBody UpdateDashboardWidgetRequest request) {
        return ResponseEntity.ok(dashboardService.updateWidget(dashboardId, widgetId, request));
    }

    /**
     * Removes a widget from a dashboard.
     *
     * @param dashboardId the dashboard UUID
     * @param widgetId    the widget UUID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{dashboardId}/widgets/{widgetId}")
    @Operation(summary = "Remove a widget from a dashboard")
    public ResponseEntity<Void> removeWidget(
            @PathVariable UUID dashboardId,
            @PathVariable UUID widgetId) {
        dashboardService.removeWidget(dashboardId, widgetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorders widgets within a dashboard.
     *
     * @param dashboardId the dashboard UUID
     * @param request     the ordered list of widget IDs
     * @return the updated dashboard
     */
    @PutMapping("/{dashboardId}/widgets/reorder")
    @Operation(summary = "Reorder widgets within a dashboard")
    public ResponseEntity<DashboardResponse> reorderWidgets(
            @PathVariable UUID dashboardId,
            @Valid @RequestBody ReorderWidgetsRequest request) {
        return ResponseEntity.ok(dashboardService.reorderWidgets(dashboardId, request.widgetIds()));
    }

    /**
     * Updates the grid layout positions for all widgets in a dashboard.
     *
     * @param dashboardId the dashboard UUID
     * @param request     the widget position updates
     * @return the updated dashboard
     */
    @PutMapping("/{dashboardId}/layout")
    @Operation(summary = "Update widget layout positions")
    public ResponseEntity<DashboardResponse> updateLayout(
            @PathVariable UUID dashboardId,
            @Valid @RequestBody UpdateLayoutRequest request) {
        return ResponseEntity.ok(dashboardService.updateLayout(dashboardId, request.positions()));
    }

    // ==================== Template Endpoints ====================

    /**
     * Marks a dashboard as a template.
     *
     * @param dashboardId the dashboard UUID
     * @return the updated dashboard
     */
    @PostMapping("/{dashboardId}/template")
    @Operation(summary = "Mark a dashboard as a template")
    public ResponseEntity<DashboardResponse> markAsTemplate(@PathVariable UUID dashboardId) {
        return ResponseEntity.ok(dashboardService.markAsTemplate(dashboardId));
    }

    /**
     * Unmarks a dashboard as a template.
     *
     * @param dashboardId the dashboard UUID
     * @return the updated dashboard
     */
    @DeleteMapping("/{dashboardId}/template")
    @Operation(summary = "Unmark a dashboard as a template")
    public ResponseEntity<DashboardResponse> unmarkAsTemplate(@PathVariable UUID dashboardId) {
        return ResponseEntity.ok(dashboardService.unmarkAsTemplate(dashboardId));
    }

    /**
     * Lists all dashboard templates for a team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return the list of template dashboards
     */
    @GetMapping("/templates")
    @Operation(summary = "List dashboard templates for a team")
    public ResponseEntity<List<DashboardResponse>> getTemplates(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(dashboardService.getTemplates(teamId));
    }

    /**
     * Creates a new dashboard from an existing template.
     *
     * @param request     the creation request with template ID and new name
     * @param httpRequest the HTTP request for team ID extraction
     * @return the newly created dashboard
     */
    @PostMapping("/from-template")
    @Operation(summary = "Create a dashboard from a template")
    public ResponseEntity<DashboardResponse> createFromTemplate(
            @Valid @RequestBody CreateFromTemplateRequest request,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        DashboardResponse response = dashboardService.createFromTemplate(
                request.templateId(), request.name(), teamId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Duplicates an existing dashboard with a new name.
     *
     * @param dashboardId the source dashboard UUID
     * @param body        request body containing the new name
     * @param httpRequest the HTTP request for team ID extraction
     * @return the duplicated dashboard
     */
    @PostMapping("/{dashboardId}/duplicate")
    @Operation(summary = "Duplicate an existing dashboard")
    public ResponseEntity<DashboardResponse> duplicateDashboard(
            @PathVariable UUID dashboardId,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        String newName = body.getOrDefault("name", "Copy");
        DashboardResponse response = dashboardService.duplicateDashboard(dashboardId, newName, teamId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
