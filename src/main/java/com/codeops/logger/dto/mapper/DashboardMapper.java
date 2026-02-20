package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateDashboardRequest;
import com.codeops.logger.dto.request.CreateDashboardWidgetRequest;
import com.codeops.logger.dto.response.DashboardResponse;
import com.codeops.logger.dto.response.DashboardWidgetResponse;
import com.codeops.logger.entity.Dashboard;
import com.codeops.logger.entity.DashboardWidget;
import com.codeops.logger.entity.enums.WidgetType;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Dashboard} entities and DTOs.
 * Includes nested mapping for {@link DashboardWidget}.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DashboardMapper {

    DashboardResponse toResponse(Dashboard entity);

    List<DashboardResponse> toResponseList(List<Dashboard> entities);

    @Mapping(source = "dashboard.id", target = "dashboardId")
    DashboardWidgetResponse toWidgetResponse(DashboardWidget widget);

    List<DashboardWidgetResponse> toWidgetResponseList(List<DashboardWidget> widgets);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "widgets", ignore = true)
    @Mapping(target = "isTemplate", constant = "false")
    Dashboard toEntity(CreateDashboardRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "dashboard", ignore = true)
    DashboardWidget toWidgetEntity(CreateDashboardWidgetRequest request);

    /**
     * Maps a widget type string to the WidgetType enum.
     */
    default WidgetType mapWidgetType(String widgetType) {
        if (widgetType == null) return null;
        return WidgetType.valueOf(widgetType.toUpperCase());
    }
}
