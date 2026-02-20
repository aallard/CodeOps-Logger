package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateSavedQueryRequest;
import com.codeops.logger.dto.response.SavedQueryResponse;
import com.codeops.logger.entity.SavedQuery;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link SavedQuery} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SavedQueryMapper {

    SavedQueryResponse toResponse(SavedQuery entity);

    List<SavedQueryResponse> toResponseList(List<SavedQuery> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastExecutedAt", ignore = true)
    @Mapping(target = "executionCount", constant = "0L")
    SavedQuery toEntity(CreateSavedQueryRequest request);
}
