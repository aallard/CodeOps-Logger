package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.enums.LogLevel;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link LogEntry} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LogEntryMapper {

    @Mapping(source = "source.id", target = "sourceId")
    @Mapping(source = "source.name", target = "sourceName")
    @Mapping(source = "level", target = "level")
    LogEntryResponse toResponse(LogEntry entity);

    List<LogEntryResponse> toResponseList(List<LogEntry> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    LogEntry toEntity(IngestLogEntryRequest request);

    /**
     * Maps the level string to LogLevel enum. Used by toEntity.
     */
    default LogLevel mapLevel(String level) {
        if (level == null) return null;
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LogLevel.INFO;
        }
    }
}
