package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.RegisterMetricRequest;
import com.codeops.logger.dto.response.MetricDataPointResponse;
import com.codeops.logger.dto.response.MetricResponse;
import com.codeops.logger.entity.Metric;
import com.codeops.logger.entity.MetricSeries;
import com.codeops.logger.entity.enums.MetricType;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Metric} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MetricMapper {

    MetricResponse toResponse(Metric entity);

    List<MetricResponse> toResponseList(List<Metric> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    Metric toEntity(RegisterMetricRequest request);

    @Mapping(source = "metric.id", target = "metricId")
    MetricDataPointResponse toDataPointResponse(MetricSeries entity);

    List<MetricDataPointResponse> toDataPointResponseList(List<MetricSeries> entities);

    /**
     * Maps a metric type string to the MetricType enum.
     */
    default MetricType mapMetricType(String metricType) {
        if (metricType == null) return null;
        return MetricType.valueOf(metricType.toUpperCase());
    }
}
