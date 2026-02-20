package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateLogSourceRequest;
import com.codeops.logger.dto.response.LogSourceResponse;
import com.codeops.logger.entity.LogSource;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link LogSource} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LogSourceMapper {

    LogSourceResponse toResponse(LogSource entity);

    List<LogSourceResponse> toResponseList(List<LogSource> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "lastLogReceivedAt", ignore = true)
    @Mapping(target = "logCount", constant = "0L")
    LogSource toEntity(CreateLogSourceRequest request);
}
