package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.response.QueryHistoryResponse;
import com.codeops.logger.entity.QueryHistory;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link QueryHistory} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface QueryHistoryMapper {

    QueryHistoryResponse toResponse(QueryHistory entity);

    List<QueryHistoryResponse> toResponseList(List<QueryHistory> entities);
}
