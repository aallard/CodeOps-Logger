package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.DashboardMapper;
import com.codeops.logger.dto.request.*;
import com.codeops.logger.dto.response.DashboardResponse;
import com.codeops.logger.dto.response.DashboardWidgetResponse;
import com.codeops.logger.entity.Dashboard;
import com.codeops.logger.entity.DashboardWidget;
import com.codeops.logger.entity.enums.WidgetType;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.DashboardRepository;
import com.codeops.logger.repository.DashboardWidgetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DashboardService}.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DashboardWidgetRepository dashboardWidgetRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardService dashboardService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private Dashboard createDashboard(String name) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(UUID.randomUUID());
        dashboard.setName(name);
        dashboard.setTeamId(TEAM_ID);
        dashboard.setCreatedBy(USER_ID);
        dashboard.setIsShared(true);
        dashboard.setIsTemplate(false);
        dashboard.setRefreshIntervalSeconds(AppConstants.DEFAULT_REFRESH_INTERVAL_SECONDS);
        dashboard.setWidgets(new ArrayList<>());
        return dashboard;
    }

    private DashboardWidget createWidget(Dashboard dashboard, String title,
                                           WidgetType type, int sortOrder) {
        DashboardWidget widget = new DashboardWidget();
        widget.setId(UUID.randomUUID());
        widget.setDashboard(dashboard);
        widget.setTitle(title);
        widget.setWidgetType(type);
        widget.setGridX(0);
        widget.setGridY(sortOrder * 3);
        widget.setGridWidth(4);
        widget.setGridHeight(3);
        widget.setSortOrder(sortOrder);
        return widget;
    }

    private DashboardResponse mockDashboardResponse(Dashboard d) {
        return new DashboardResponse(
                d.getId(), d.getName(), d.getDescription(), d.getTeamId(),
                d.getCreatedBy(), d.getIsShared(), d.getIsTemplate(),
                d.getRefreshIntervalSeconds(), d.getLayoutJson(),
                List.of(), Instant.now(), Instant.now());
    }

    private DashboardWidgetResponse mockWidgetResponse(DashboardWidget w) {
        return new DashboardWidgetResponse(
                w.getId(), w.getDashboard().getId(), w.getTitle(),
                w.getWidgetType().name(), w.getQueryJson(), w.getConfigJson(),
                w.getGridX(), w.getGridY(), w.getGridWidth(), w.getGridHeight(),
                w.getSortOrder(), Instant.now(), Instant.now());
    }

    // ==================== Dashboard CRUD Tests ====================

    @Test
    void testCreateDashboard_valid_succeeds() {
        CreateDashboardRequest request = new CreateDashboardRequest(
                "My Dashboard", "Description", true, 60, null);

        Dashboard entity = createDashboard("My Dashboard");
        DashboardResponse response = mockDashboardResponse(entity);

        when(dashboardRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(dashboardMapper.toEntity(request)).thenReturn(entity);
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(entity);
        when(dashboardMapper.toResponse(entity)).thenReturn(response);

        DashboardResponse result = dashboardService.createDashboard(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("My Dashboard");
        verify(dashboardRepository).save(any(Dashboard.class));
    }

    @Test
    void testCreateDashboard_exceedsMax_throwsValidation() {
        when(dashboardRepository.countByTeamId(TEAM_ID))
                .thenReturn((long) AppConstants.MAX_DASHBOARDS_PER_TEAM);

        CreateDashboardRequest request = new CreateDashboardRequest(
                "Too Many", null, null, null, null);

        assertThatThrownBy(() -> dashboardService.createDashboard(request, TEAM_ID, USER_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum dashboard limit");
    }

    @Test
    void testCreateDashboard_defaultValues_applied() {
        CreateDashboardRequest request = new CreateDashboardRequest(
                "Default Dashboard", null, null, null, null);

        Dashboard entity = createDashboard("Default Dashboard");

        when(dashboardRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(dashboardMapper.toEntity(request)).thenReturn(entity);
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(entity);
        when(dashboardMapper.toResponse(entity)).thenReturn(mockDashboardResponse(entity));

        dashboardService.createDashboard(request, TEAM_ID, USER_ID);

        ArgumentCaptor<Dashboard> captor = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardRepository).save(captor.capture());
        assertThat(captor.getValue().getIsShared()).isTrue();
        assertThat(captor.getValue().getRefreshIntervalSeconds())
                .isEqualTo(AppConstants.DEFAULT_REFRESH_INTERVAL_SECONDS);
    }

    @Test
    void testGetDashboardsByTeam_returnsList() {
        Dashboard d1 = createDashboard("D1");
        Dashboard d2 = createDashboard("D2");

        when(dashboardRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(d1, d2));
        when(dashboardMapper.toResponseList(any())).thenReturn(List.of(
                mock(DashboardResponse.class), mock(DashboardResponse.class)));

        List<DashboardResponse> result = dashboardService.getDashboardsByTeam(TEAM_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void testGetSharedDashboards_filtersCorrectly() {
        Dashboard shared = createDashboard("Shared");
        shared.setIsShared(true);

        when(dashboardRepository.findByTeamIdAndIsSharedTrue(TEAM_ID)).thenReturn(List.of(shared));
        when(dashboardMapper.toResponseList(any())).thenReturn(List.of(mock(DashboardResponse.class)));

        List<DashboardResponse> result = dashboardService.getSharedDashboards(TEAM_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void testGetDashboard_found_includesWidgets() {
        Dashboard dashboard = createDashboard("Full");
        DashboardWidget widget = createWidget(dashboard, "Chart", WidgetType.TIME_SERIES_CHART, 0);
        dashboard.getWidgets().add(widget);

        DashboardResponse response = new DashboardResponse(
                dashboard.getId(), "Full", null, TEAM_ID, USER_ID, true, false,
                30, null, List.of(mockWidgetResponse(widget)), Instant.now(), Instant.now());

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardMapper.toResponse(dashboard)).thenReturn(response);

        DashboardResponse result = dashboardService.getDashboard(dashboard.getId());

        assertThat(result.widgets()).hasSize(1);
    }

    @Test
    void testGetDashboard_notFound_throwsNotFound() {
        UUID fakeId = UUID.randomUUID();
        when(dashboardRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getDashboard(fakeId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Dashboard not found");
    }

    @Test
    void testUpdateDashboard_updatesFields() {
        Dashboard dashboard = createDashboard("Old Name");
        UpdateDashboardRequest request = new UpdateDashboardRequest(
                "New Name", "New Desc", false, null, 120, null);

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toResponse(dashboard)).thenReturn(mockDashboardResponse(dashboard));

        dashboardService.updateDashboard(dashboard.getId(), request);

        assertThat(dashboard.getName()).isEqualTo("New Name");
        assertThat(dashboard.getDescription()).isEqualTo("New Desc");
        assertThat(dashboard.getIsShared()).isFalse();
        assertThat(dashboard.getRefreshIntervalSeconds()).isEqualTo(120);
    }

    @Test
    void testDeleteDashboard_cascadesWidgets() {
        Dashboard dashboard = createDashboard("Delete Me");
        DashboardWidget w = createWidget(dashboard, "Widget", WidgetType.COUNTER, 0);
        dashboard.getWidgets().add(w);

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));

        dashboardService.deleteDashboard(dashboard.getId());

        verify(dashboardRepository).delete(dashboard);
    }

    // ==================== Widget CRUD Tests ====================

    @Test
    void testAddWidget_valid_addsToDashboard() {
        Dashboard dashboard = createDashboard("Dashboard");
        CreateDashboardWidgetRequest request = new CreateDashboardWidgetRequest(
                "My Chart", "TIME_SERIES_CHART", "{}", "{}", 0, 0, 6, 4, 0);

        DashboardWidget widgetEntity = createWidget(dashboard, "My Chart",
                WidgetType.TIME_SERIES_CHART, 0);

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardMapper.toWidgetEntity(request)).thenReturn(widgetEntity);
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toWidgetResponse(any(DashboardWidget.class)))
                .thenReturn(mockWidgetResponse(widgetEntity));

        DashboardWidgetResponse result = dashboardService.addWidget(dashboard.getId(), request);

        assertThat(result.title()).isEqualTo("My Chart");
    }

    @Test
    void testAddWidget_exceedsMax_throwsValidation() {
        Dashboard dashboard = createDashboard("Full");
        for (int i = 0; i < AppConstants.MAX_WIDGETS_PER_DASHBOARD; i++) {
            dashboard.getWidgets().add(createWidget(dashboard, "W" + i, WidgetType.COUNTER, i));
        }

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));

        CreateDashboardWidgetRequest request = new CreateDashboardWidgetRequest(
                "One Too Many", "COUNTER", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> dashboardService.addWidget(dashboard.getId(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("maximum widget limit");
    }

    @Test
    void testAddWidget_invalidType_throwsValidation() {
        Dashboard dashboard = createDashboard("Dashboard");

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));

        CreateDashboardWidgetRequest request = new CreateDashboardWidgetRequest(
                "Bad Widget", "HOLOGRAM", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> dashboardService.addWidget(dashboard.getId(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid widget type");
    }

    @Test
    void testAddWidget_defaultGridPosition() {
        Dashboard dashboard = createDashboard("Dashboard");
        DashboardWidget existing = createWidget(dashboard, "Existing", WidgetType.COUNTER, 0);
        existing.setGridY(0);
        existing.setGridHeight(3);
        dashboard.getWidgets().add(existing);

        CreateDashboardWidgetRequest request = new CreateDashboardWidgetRequest(
                "New Widget", "GAUGE", null, null, null, null, null, null, null);

        DashboardWidget newWidget = new DashboardWidget();
        newWidget.setTitle("New Widget");
        newWidget.setWidgetType(WidgetType.GAUGE);

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardMapper.toWidgetEntity(request)).thenReturn(newWidget);
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toWidgetResponse(any(DashboardWidget.class)))
                .thenReturn(mock(DashboardWidgetResponse.class));

        dashboardService.addWidget(dashboard.getId(), request);

        // Verify new widget was given position after existing widget
        assertThat(newWidget.getGridX()).isZero();
        assertThat(newWidget.getGridY()).isEqualTo(3); // next row after existing (0 + 3)
        assertThat(newWidget.getGridWidth()).isEqualTo(4);
        assertThat(newWidget.getGridHeight()).isEqualTo(3);
        assertThat(newWidget.getSortOrder()).isEqualTo(1); // second widget
    }

    @Test
    void testUpdateWidget_updatesFields() {
        Dashboard dashboard = createDashboard("Dashboard");
        DashboardWidget widget = createWidget(dashboard, "Old Title", WidgetType.COUNTER, 0);
        dashboard.getWidgets().add(widget);

        UpdateDashboardWidgetRequest request = new UpdateDashboardWidgetRequest(
                "New Title", "GAUGE", null, null, 2, 4, 6, 3, null);

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toWidgetResponse(widget)).thenReturn(mockWidgetResponse(widget));

        dashboardService.updateWidget(dashboard.getId(), widget.getId(), request);

        assertThat(widget.getTitle()).isEqualTo("New Title");
        assertThat(widget.getWidgetType()).isEqualTo(WidgetType.GAUGE);
        assertThat(widget.getGridX()).isEqualTo(2);
        assertThat(widget.getGridY()).isEqualTo(4);
    }

    @Test
    void testUpdateWidget_widgetNotFound_throwsNotFound() {
        Dashboard dashboard = createDashboard("Dashboard");

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));

        UUID fakeWidgetId = UUID.randomUUID();
        UpdateDashboardWidgetRequest request = new UpdateDashboardWidgetRequest(
                "Title", null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> dashboardService.updateWidget(
                dashboard.getId(), fakeWidgetId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Widget not found");
    }

    @Test
    void testRemoveWidget_removesAndReorders() {
        Dashboard dashboard = createDashboard("Dashboard");
        DashboardWidget w0 = createWidget(dashboard, "First", WidgetType.COUNTER, 0);
        DashboardWidget w1 = createWidget(dashboard, "Second", WidgetType.GAUGE, 1);
        DashboardWidget w2 = createWidget(dashboard, "Third", WidgetType.TABLE, 2);
        dashboard.getWidgets().addAll(List.of(w0, w1, w2));

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);

        dashboardService.removeWidget(dashboard.getId(), w1.getId());

        assertThat(dashboard.getWidgets()).hasSize(2);
        assertThat(dashboard.getWidgets().get(0).getSortOrder()).isZero();
        assertThat(dashboard.getWidgets().get(1).getSortOrder()).isEqualTo(1);
    }

    // ==================== Grid Layout Tests ====================

    @Test
    void testReorderWidgets_updatesSort() {
        Dashboard dashboard = createDashboard("Dashboard");
        DashboardWidget w0 = createWidget(dashboard, "A", WidgetType.COUNTER, 0);
        DashboardWidget w1 = createWidget(dashboard, "B", WidgetType.GAUGE, 1);
        dashboard.getWidgets().addAll(List.of(w0, w1));

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toResponse(dashboard)).thenReturn(mockDashboardResponse(dashboard));

        // Reverse order
        dashboardService.reorderWidgets(dashboard.getId(), List.of(w1.getId(), w0.getId()));

        assertThat(w1.getSortOrder()).isZero();
        assertThat(w0.getSortOrder()).isEqualTo(1);
    }

    @Test
    void testReorderWidgets_missingWidget_throwsValidation() {
        Dashboard dashboard = createDashboard("Dashboard");
        DashboardWidget w0 = createWidget(dashboard, "A", WidgetType.COUNTER, 0);
        dashboard.getWidgets().add(w0);

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));

        assertThatThrownBy(() -> dashboardService.reorderWidgets(
                dashboard.getId(), List.of(UUID.randomUUID())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Widget IDs must match");
    }

    @Test
    void testUpdateLayout_updatesPositions() {
        Dashboard dashboard = createDashboard("Dashboard");
        DashboardWidget w0 = createWidget(dashboard, "Chart", WidgetType.TIME_SERIES_CHART, 0);
        dashboard.getWidgets().add(w0);

        WidgetPositionUpdate pos = new WidgetPositionUpdate(w0.getId(), 2, 4, 8, 6);

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toResponse(dashboard)).thenReturn(mockDashboardResponse(dashboard));

        dashboardService.updateLayout(dashboard.getId(), List.of(pos));

        assertThat(w0.getGridX()).isEqualTo(2);
        assertThat(w0.getGridY()).isEqualTo(4);
        assertThat(w0.getGridWidth()).isEqualTo(8);
        assertThat(w0.getGridHeight()).isEqualTo(6);
    }

    // ==================== Template System Tests ====================

    @Test
    void testMarkAsTemplate_setsFlag() {
        Dashboard dashboard = createDashboard("Dashboard");
        assertThat(dashboard.getIsTemplate()).isFalse();

        when(dashboardRepository.findById(dashboard.getId())).thenReturn(Optional.of(dashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(dashboard);
        when(dashboardMapper.toResponse(dashboard)).thenReturn(mockDashboardResponse(dashboard));

        dashboardService.markAsTemplate(dashboard.getId());

        assertThat(dashboard.getIsTemplate()).isTrue();
    }

    @Test
    void testGetTemplates_returnsOnlyTemplates() {
        Dashboard template = createDashboard("Template");
        template.setIsTemplate(true);

        when(dashboardRepository.findByTeamIdAndIsTemplateTrue(TEAM_ID)).thenReturn(List.of(template));
        when(dashboardMapper.toResponseList(any())).thenReturn(List.of(mock(DashboardResponse.class)));

        List<DashboardResponse> result = dashboardService.getTemplates(TEAM_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void testCreateFromTemplate_deepClones() {
        Dashboard template = createDashboard("Template");
        template.setIsTemplate(true);
        template.setDescription("Template desc");
        template.setLayoutJson("{\"cols\": 12}");
        DashboardWidget tw = createWidget(template, "Chart", WidgetType.TIME_SERIES_CHART, 0);
        tw.setQueryJson("{\"metric\": \"cpu\"}");
        tw.setConfigJson("{\"color\": \"blue\"}");
        template.getWidgets().add(tw);

        when(dashboardRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(dashboardRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dashboardMapper.toResponse(any(Dashboard.class)))
                .thenReturn(mock(DashboardResponse.class));

        dashboardService.createFromTemplate(template.getId(), "My Copy", TEAM_ID, USER_ID);

        ArgumentCaptor<Dashboard> captor = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardRepository).save(captor.capture());
        Dashboard clone = captor.getValue();

        assertThat(clone.getName()).isEqualTo("My Copy");
        assertThat(clone.getDescription()).isEqualTo("Template desc");
        assertThat(clone.getLayoutJson()).isEqualTo("{\"cols\": 12}");
        assertThat(clone.getWidgets()).hasSize(1);
        assertThat(clone.getWidgets().getFirst().getTitle()).isEqualTo("Chart");
        assertThat(clone.getWidgets().getFirst().getQueryJson()).isEqualTo("{\"metric\": \"cpu\"}");
    }

    @Test
    void testCreateFromTemplate_widgetsHaveNewIds() {
        Dashboard template = createDashboard("Template");
        DashboardWidget tw = createWidget(template, "Widget", WidgetType.COUNTER, 0);
        template.getWidgets().add(tw);

        when(dashboardRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(dashboardRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dashboardMapper.toResponse(any(Dashboard.class)))
                .thenReturn(mock(DashboardResponse.class));

        dashboardService.createFromTemplate(template.getId(), "Clone", TEAM_ID, USER_ID);

        ArgumentCaptor<Dashboard> captor = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardRepository).save(captor.capture());
        Dashboard clone = captor.getValue();

        // Clone widget has no ID yet (new entity, not persisted yet)
        assertThat(clone.getWidgets().getFirst().getId()).isNull();
        // Clone itself also has no ID
        assertThat(clone.getId()).isNull();
    }

    @Test
    void testCreateFromTemplate_cloneIsNotTemplate() {
        Dashboard template = createDashboard("Template");
        template.setIsTemplate(true);

        when(dashboardRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(dashboardRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dashboardMapper.toResponse(any(Dashboard.class)))
                .thenReturn(mock(DashboardResponse.class));

        dashboardService.createFromTemplate(template.getId(), "Clone", TEAM_ID, USER_ID);

        ArgumentCaptor<Dashboard> captor = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardRepository).save(captor.capture());
        assertThat(captor.getValue().getIsTemplate()).isFalse();
    }

    @Test
    void testDuplicateDashboard_sameAsTemplateClone() {
        Dashboard original = createDashboard("Original");
        DashboardWidget w = createWidget(original, "Widget", WidgetType.GAUGE, 0);
        original.getWidgets().add(w);

        when(dashboardRepository.countByTeamId(TEAM_ID)).thenReturn(0L);
        when(dashboardRepository.findById(original.getId())).thenReturn(Optional.of(original));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(dashboardMapper.toResponse(any(Dashboard.class)))
                .thenReturn(mock(DashboardResponse.class));

        dashboardService.duplicateDashboard(original.getId(), "Copy", TEAM_ID, USER_ID);

        ArgumentCaptor<Dashboard> captor = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardRepository).save(captor.capture());
        Dashboard clone = captor.getValue();

        assertThat(clone.getName()).isEqualTo("Copy");
        assertThat(clone.getWidgets()).hasSize(1);
        assertThat(clone.getIsTemplate()).isFalse();
    }
}
