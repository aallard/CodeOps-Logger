package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateAlertRuleRequest;
import com.codeops.logger.dto.response.AlertRuleResponse;
import com.codeops.logger.entity.AlertRule;
import com.codeops.logger.entity.enums.AlertSeverity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link AlertRule} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AlertRuleMapper {

    @Mapping(source = "trap.id", target = "trapId")
    @Mapping(source = "trap.name", target = "trapName")
    @Mapping(source = "channel.id", target = "channelId")
    @Mapping(source = "channel.name", target = "channelName")
    AlertRuleResponse toResponse(AlertRule entity);

    List<AlertRuleResponse> toResponseList(List<AlertRule> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "trap", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    AlertRule toEntity(CreateAlertRuleRequest request);

    /**
     * Maps a severity string to the AlertSeverity enum.
     */
    default AlertSeverity mapSeverity(String severity) {
        if (severity == null) return null;
        return AlertSeverity.valueOf(severity.toUpperCase());
    }
}
