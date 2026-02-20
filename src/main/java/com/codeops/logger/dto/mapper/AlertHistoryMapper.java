package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.response.AlertHistoryResponse;
import com.codeops.logger.entity.AlertHistory;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link AlertHistory} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AlertHistoryMapper {

    @Mapping(source = "rule.id", target = "ruleId")
    @Mapping(source = "rule.name", target = "ruleName")
    @Mapping(source = "trap.id", target = "trapId")
    @Mapping(source = "trap.name", target = "trapName")
    @Mapping(source = "channel.id", target = "channelId")
    @Mapping(source = "channel.name", target = "channelName")
    AlertHistoryResponse toResponse(AlertHistory entity);

    List<AlertHistoryResponse> toResponseList(List<AlertHistory> entities);
}
