package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateTraceSpanRequest;
import com.codeops.logger.dto.response.TraceSpanResponse;
import com.codeops.logger.entity.TraceSpan;
import com.codeops.logger.entity.enums.SpanStatus;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link TraceSpan} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TraceSpanMapper {

    TraceSpanResponse toResponse(TraceSpan entity);

    List<TraceSpanResponse> toResponseList(List<TraceSpan> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    TraceSpan toEntity(CreateTraceSpanRequest request);

    /**
     * Maps a span status string to the SpanStatus enum, defaulting to OK.
     */
    default SpanStatus mapSpanStatus(String status) {
        if (status == null) return SpanStatus.OK;
        try {
            return SpanStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SpanStatus.OK;
        }
    }
}
