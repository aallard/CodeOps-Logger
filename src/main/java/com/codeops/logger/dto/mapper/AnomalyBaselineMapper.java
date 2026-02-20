package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.response.AnomalyBaselineResponse;
import com.codeops.logger.entity.AnomalyBaseline;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link AnomalyBaseline} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AnomalyBaselineMapper {

    AnomalyBaselineResponse toResponse(AnomalyBaseline entity);

    List<AnomalyBaselineResponse> toResponseList(List<AnomalyBaseline> entities);
}
