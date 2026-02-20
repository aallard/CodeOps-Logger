package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.DashboardMapper;
import com.codeops.logger.dto.request.*;
import com.codeops.logger.dto.response.DashboardResponse;
import com.codeops.logger.dto.response.DashboardWidgetResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.entity.Dashboard;
import com.codeops.logger.entity.DashboardWidget;
import com.codeops.logger.entity.enums.WidgetType;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.DashboardRepository;
import com.codeops.logger.repository.DashboardWidgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages dashboard lifecycle including CRUD operations, widget management,
 * grid layout positioning, and the template system for cloning dashboards.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;
    private final DashboardMapper dashboardMapper;

    // ==================== Dashboard CRUD ====================

    /**
     * Creates a new dashboard.
     *
     * @param request the dashboard configuration
     * @param teamId  the team scope
     * @param userId  the creating user
     * @return the created dashboard response (no widgets yet)
     * @throws ValidationException if team has reached MAX_DASHBOARDS_PER_TEAM
     */
    @Transactional
    public DashboardResponse createDashboard(CreateDashboardRequest request,
                                               UUID teamId, UUID userId) {
        long currentCount = dashboardRepository.countByTeamId(teamId);
        if (currentCount >= AppConstants.MAX_DASHBOARDS_PER_TEAM) {
            throw new ValidationException(
                    "Team has reached maximum dashboard limit (" + AppConstants.MAX_DASHBOARDS_PER_TEAM + ")");
        }

        Dashboard entity = dashboardMapper.toEntity(request);
        entity.setTeamId(teamId);
        entity.setCreatedBy(userId);
        entity.setIsShared(request.isShared() != null ? request.isShared() : true);
        entity.setRefreshIntervalSeconds(
                request.refreshIntervalSeconds() != null
                        ? request.refreshIntervalSeconds()
                        : AppConstants.DEFAULT_REFRESH_INTERVAL_SECONDS);

        Dashboard saved = dashboardRepository.save(entity);
        log.info("Created dashboard '{}' for team {}", saved.getName(), teamId);
        return dashboardMapper.toResponse(saved);
    }

    /**
     * Returns all dashboards for a team.
     *
     * @param teamId the team scope
     * @return list of dashboard responses with widgets
     */
    public List<DashboardResponse> getDashboardsByTeam(UUID teamId) {
        List<Dashboard> dashboards = dashboardRepository.findByTeamId(teamId);
        return dashboardMapper.toResponseList(dashboards);
    }

    /**
     * Returns paginated dashboards for a team.
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated dashboard responses
     */
    public PageResponse<DashboardResponse> getDashboardsByTeamPaged(UUID teamId, int page, int size) {
        Page<Dashboard> springPage = dashboardRepository.findByTeamId(teamId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<DashboardResponse> content = dashboardMapper.toResponseList(springPage.getContent());
        return new PageResponse<>(
                content,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }

    /**
     * Returns shared dashboards for a team.
     *
     * @param teamId the team scope
     * @return list of shared dashboard responses
     */
    public List<DashboardResponse> getSharedDashboards(UUID teamId) {
        List<Dashboard> dashboards = dashboardRepository.findByTeamIdAndIsSharedTrue(teamId);
        return dashboardMapper.toResponseList(dashboards);
    }

    /**
     * Returns dashboards created by a specific user.
     *
     * @param userId the user ID
     * @return list of dashboard responses
     */
    public List<DashboardResponse> getDashboardsByUser(UUID userId) {
        List<Dashboard> dashboards = dashboardRepository.findByCreatedBy(userId);
        return dashboardMapper.toResponseList(dashboards);
    }

    /**
     * Returns a single dashboard with all its widgets.
     *
     * @param dashboardId the dashboard ID
     * @return the dashboard response with widgets
     * @throws NotFoundException if not found
     */
    public DashboardResponse getDashboard(UUID dashboardId) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);
        return dashboardMapper.toResponse(dashboard);
    }

    /**
     * Updates a dashboard's metadata (not widgets).
     *
     * @param dashboardId the dashboard ID
     * @param request     the update request
     * @return the updated dashboard response
     * @throws NotFoundException if not found
     */
    @Transactional
    public DashboardResponse updateDashboard(UUID dashboardId, UpdateDashboardRequest request) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);

        if (request.name() != null) {
            dashboard.setName(request.name());
        }
        if (request.description() != null) {
            dashboard.setDescription(request.description());
        }
        if (request.isShared() != null) {
            dashboard.setIsShared(request.isShared());
        }
        if (request.isTemplate() != null) {
            dashboard.setIsTemplate(request.isTemplate());
        }
        if (request.refreshIntervalSeconds() != null) {
            dashboard.setRefreshIntervalSeconds(request.refreshIntervalSeconds());
        }
        if (request.layoutJson() != null) {
            dashboard.setLayoutJson(request.layoutJson());
        }

        Dashboard saved = dashboardRepository.save(dashboard);
        return dashboardMapper.toResponse(saved);
    }

    /**
     * Deletes a dashboard and all its widgets (cascade).
     *
     * @param dashboardId the dashboard ID
     * @throws NotFoundException if not found
     */
    @Transactional
    public void deleteDashboard(UUID dashboardId) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);
        dashboardRepository.delete(dashboard);
        log.info("Deleted dashboard '{}' ({})", dashboard.getName(), dashboardId);
    }

    // ==================== Widget CRUD ====================

    /**
     * Adds a widget to a dashboard.
     *
     * @param dashboardId the target dashboard
     * @param request     the widget configuration
     * @return the created widget response
     * @throws ValidationException if dashboard has reached MAX_WIDGETS_PER_DASHBOARD
     * @throws ValidationException if widget type is invalid
     * @throws NotFoundException   if dashboard not found
     */
    @Transactional
    public DashboardWidgetResponse addWidget(UUID dashboardId,
                                               CreateDashboardWidgetRequest request) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);

        if (dashboard.getWidgets().size() >= AppConstants.MAX_WIDGETS_PER_DASHBOARD) {
            throw new ValidationException(
                    "Dashboard has reached maximum widget limit (" + AppConstants.MAX_WIDGETS_PER_DASHBOARD + ")");
        }

        WidgetType widgetType = parseWidgetType(request.widgetType());

        DashboardWidget widget = dashboardMapper.toWidgetEntity(request);
        widget.setDashboard(dashboard);
        widget.setWidgetType(widgetType);

        if (request.gridX() == null) {
            widget.setGridX(0);
        }
        if (request.gridY() == null) {
            int nextRow = dashboard.getWidgets().stream()
                    .mapToInt(w -> w.getGridY() + w.getGridHeight())
                    .max()
                    .orElse(0);
            widget.setGridY(nextRow);
        }
        if (request.gridWidth() == null) {
            widget.setGridWidth(4);
        }
        if (request.gridHeight() == null) {
            widget.setGridHeight(3);
        }
        if (request.sortOrder() == null) {
            widget.setSortOrder(dashboard.getWidgets().size());
        }

        dashboard.getWidgets().add(widget);
        Dashboard saved = dashboardRepository.save(dashboard);

        DashboardWidget savedWidget = saved.getWidgets().stream()
                .filter(w -> w.getTitle().equals(request.title()) && w.getWidgetType() == widgetType)
                .reduce((first, second) -> second)
                .orElse(saved.getWidgets().getLast());

        log.info("Added widget '{}' (type={}) to dashboard '{}'",
                request.title(), widgetType, dashboard.getName());
        return dashboardMapper.toWidgetResponse(savedWidget);
    }

    /**
     * Updates a widget within a dashboard.
     *
     * @param dashboardId the dashboard ID
     * @param widgetId    the widget ID
     * @param request     the update request
     * @return the updated widget response
     * @throws NotFoundException   if dashboard or widget not found
     * @throws ValidationException if widget type is invalid
     */
    @Transactional
    public DashboardWidgetResponse updateWidget(UUID dashboardId, UUID widgetId,
                                                  UpdateDashboardWidgetRequest request) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);
        DashboardWidget widget = findWidgetInDashboard(dashboard, widgetId);

        if (request.title() != null) {
            widget.setTitle(request.title());
        }
        if (request.widgetType() != null) {
            widget.setWidgetType(parseWidgetType(request.widgetType()));
        }
        if (request.queryJson() != null) {
            widget.setQueryJson(request.queryJson());
        }
        if (request.configJson() != null) {
            widget.setConfigJson(request.configJson());
        }
        if (request.gridX() != null) {
            widget.setGridX(request.gridX());
        }
        if (request.gridY() != null) {
            widget.setGridY(request.gridY());
        }
        if (request.gridWidth() != null) {
            widget.setGridWidth(request.gridWidth());
        }
        if (request.gridHeight() != null) {
            widget.setGridHeight(request.gridHeight());
        }
        if (request.sortOrder() != null) {
            widget.setSortOrder(request.sortOrder());
        }

        dashboardRepository.save(dashboard);
        return dashboardMapper.toWidgetResponse(widget);
    }

    /**
     * Removes a widget from a dashboard and re-numbers remaining widget sort orders.
     *
     * @param dashboardId the dashboard ID
     * @param widgetId    the widget ID
     * @throws NotFoundException if dashboard or widget not found
     */
    @Transactional
    public void removeWidget(UUID dashboardId, UUID widgetId) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);
        DashboardWidget widget = findWidgetInDashboard(dashboard, widgetId);

        dashboard.getWidgets().remove(widget);

        for (int i = 0; i < dashboard.getWidgets().size(); i++) {
            dashboard.getWidgets().get(i).setSortOrder(i);
        }

        dashboardRepository.save(dashboard);
        log.info("Removed widget '{}' from dashboard '{}'", widget.getTitle(), dashboard.getName());
    }

    /**
     * Reorders widgets within a dashboard.
     *
     * @param dashboardId the dashboard
     * @param widgetIds   ordered list of widget IDs (new sort order)
     * @return the updated dashboard response
     * @throws ValidationException if widgetIds do not match existing widget IDs
     * @throws NotFoundException   if dashboard not found
     */
    @Transactional
    public DashboardResponse reorderWidgets(UUID dashboardId, List<UUID> widgetIds) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);

        Set<UUID> existingIds = dashboard.getWidgets().stream()
                .map(DashboardWidget::getId)
                .collect(Collectors.toSet());
        Set<UUID> providedIds = new HashSet<>(widgetIds);

        if (!existingIds.equals(providedIds)) {
            throw new ValidationException(
                    "Widget IDs must match exactly the widgets in the dashboard");
        }

        Map<UUID, DashboardWidget> widgetMap = dashboard.getWidgets().stream()
                .collect(Collectors.toMap(DashboardWidget::getId, w -> w));

        for (int i = 0; i < widgetIds.size(); i++) {
            widgetMap.get(widgetIds.get(i)).setSortOrder(i);
        }

        Dashboard saved = dashboardRepository.save(dashboard);
        return dashboardMapper.toResponse(saved);
    }

    // ==================== Grid Layout ====================

    /**
     * Updates the grid layout for all widgets in a dashboard.
     *
     * @param dashboardId the dashboard
     * @param positions   list of widget position updates
     * @return the updated dashboard
     * @throws NotFoundException if dashboard or any widget not found
     */
    @Transactional
    public DashboardResponse updateLayout(UUID dashboardId,
                                            List<WidgetPositionUpdate> positions) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);

        Map<UUID, DashboardWidget> widgetMap = dashboard.getWidgets().stream()
                .collect(Collectors.toMap(DashboardWidget::getId, w -> w));

        for (WidgetPositionUpdate pos : positions) {
            DashboardWidget widget = widgetMap.get(pos.widgetId());
            if (widget == null) {
                throw new NotFoundException("Widget not found in dashboard: " + pos.widgetId());
            }
            widget.setGridX(pos.gridX());
            widget.setGridY(pos.gridY());
            widget.setGridWidth(pos.gridWidth());
            widget.setGridHeight(pos.gridHeight());
        }

        Dashboard saved = dashboardRepository.save(dashboard);
        return dashboardMapper.toResponse(saved);
    }

    // ==================== Template System ====================

    /**
     * Marks a dashboard as a template.
     *
     * @param dashboardId the dashboard ID
     * @return the updated dashboard response
     * @throws NotFoundException if not found
     */
    @Transactional
    public DashboardResponse markAsTemplate(UUID dashboardId) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);
        dashboard.setIsTemplate(true);
        Dashboard saved = dashboardRepository.save(dashboard);
        log.info("Marked dashboard '{}' as template", dashboard.getName());
        return dashboardMapper.toResponse(saved);
    }

    /**
     * Unmarks a dashboard as a template.
     *
     * @param dashboardId the dashboard ID
     * @return the updated dashboard response
     * @throws NotFoundException if not found
     */
    @Transactional
    public DashboardResponse unmarkAsTemplate(UUID dashboardId) {
        Dashboard dashboard = findDashboardOrThrow(dashboardId);
        dashboard.setIsTemplate(false);
        Dashboard saved = dashboardRepository.save(dashboard);
        return dashboardMapper.toResponse(saved);
    }

    /**
     * Returns all templates available to a team.
     *
     * @param teamId the team scope
     * @return list of template dashboard responses
     */
    public List<DashboardResponse> getTemplates(UUID teamId) {
        List<Dashboard> templates = dashboardRepository.findByTeamIdAndIsTemplateTrue(teamId);
        return dashboardMapper.toResponseList(templates);
    }

    /**
     * Creates a new dashboard by cloning a template.
     * Deep-copies all widgets with new UUIDs. The new dashboard is not a template.
     *
     * @param templateId the template dashboard to clone
     * @param name       the name for the new dashboard
     * @param teamId     the team scope
     * @param userId     the creating user
     * @return the new dashboard with cloned widgets
     * @throws NotFoundException   if template not found
     * @throws ValidationException if team has reached MAX_DASHBOARDS_PER_TEAM
     */
    @Transactional
    public DashboardResponse createFromTemplate(UUID templateId, String name,
                                                  UUID teamId, UUID userId) {
        long currentCount = dashboardRepository.countByTeamId(teamId);
        if (currentCount >= AppConstants.MAX_DASHBOARDS_PER_TEAM) {
            throw new ValidationException(
                    "Team has reached maximum dashboard limit (" + AppConstants.MAX_DASHBOARDS_PER_TEAM + ")");
        }

        Dashboard template = findDashboardOrThrow(templateId);
        return cloneDashboard(template, name, teamId, userId);
    }

    /**
     * Duplicates an existing dashboard with a new name.
     *
     * @param dashboardId the dashboard to duplicate
     * @param newName     the name for the copy
     * @param teamId      the team scope
     * @param userId      the creating user
     * @return the duplicated dashboard with cloned widgets
     * @throws NotFoundException   if dashboard not found
     * @throws ValidationException if team has reached MAX_DASHBOARDS_PER_TEAM
     */
    @Transactional
    public DashboardResponse duplicateDashboard(UUID dashboardId, String newName,
                                                  UUID teamId, UUID userId) {
        long currentCount = dashboardRepository.countByTeamId(teamId);
        if (currentCount >= AppConstants.MAX_DASHBOARDS_PER_TEAM) {
            throw new ValidationException(
                    "Team has reached maximum dashboard limit (" + AppConstants.MAX_DASHBOARDS_PER_TEAM + ")");
        }

        Dashboard original = findDashboardOrThrow(dashboardId);
        return cloneDashboard(original, newName, teamId, userId);
    }

    // ==================== Private Helpers ====================

    /**
     * Deep-clones a dashboard and all its widgets.
     */
    private DashboardResponse cloneDashboard(Dashboard source, String name,
                                               UUID teamId, UUID userId) {
        Dashboard clone = new Dashboard();
        clone.setName(name);
        clone.setDescription(source.getDescription());
        clone.setTeamId(teamId);
        clone.setCreatedBy(userId);
        clone.setIsShared(source.getIsShared());
        clone.setIsTemplate(false);
        clone.setRefreshIntervalSeconds(source.getRefreshIntervalSeconds());
        clone.setLayoutJson(source.getLayoutJson());

        List<DashboardWidget> clonedWidgets = new ArrayList<>();
        for (DashboardWidget srcWidget : source.getWidgets()) {
            DashboardWidget clonedWidget = new DashboardWidget();
            clonedWidget.setDashboard(clone);
            clonedWidget.setTitle(srcWidget.getTitle());
            clonedWidget.setWidgetType(srcWidget.getWidgetType());
            clonedWidget.setQueryJson(srcWidget.getQueryJson());
            clonedWidget.setConfigJson(srcWidget.getConfigJson());
            clonedWidget.setGridX(srcWidget.getGridX());
            clonedWidget.setGridY(srcWidget.getGridY());
            clonedWidget.setGridWidth(srcWidget.getGridWidth());
            clonedWidget.setGridHeight(srcWidget.getGridHeight());
            clonedWidget.setSortOrder(srcWidget.getSortOrder());
            clonedWidgets.add(clonedWidget);
        }
        clone.setWidgets(clonedWidgets);

        Dashboard saved = dashboardRepository.save(clone);
        log.info("Cloned dashboard '{}' from '{}' for team {}",
                name, source.getName(), teamId);
        return dashboardMapper.toResponse(saved);
    }

    /**
     * Finds a dashboard by ID or throws NotFoundException.
     */
    private Dashboard findDashboardOrThrow(UUID dashboardId) {
        return dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new NotFoundException("Dashboard not found: " + dashboardId));
    }

    /**
     * Finds a widget within a dashboard by widget ID or throws NotFoundException.
     */
    private DashboardWidget findWidgetInDashboard(Dashboard dashboard, UUID widgetId) {
        return dashboard.getWidgets().stream()
                .filter(w -> w.getId().equals(widgetId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Widget not found in dashboard: " + widgetId));
    }

    /**
     * Parses a widget type string to the WidgetType enum.
     */
    private WidgetType parseWidgetType(String widgetType) {
        try {
            return WidgetType.valueOf(widgetType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid widget type: " + widgetType);
        }
    }
}
