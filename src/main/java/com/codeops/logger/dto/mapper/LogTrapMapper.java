package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateLogTrapRequest;
import com.codeops.logger.dto.request.CreateTrapConditionRequest;
import com.codeops.logger.dto.response.LogTrapResponse;
import com.codeops.logger.dto.response.TrapConditionResponse;
import com.codeops.logger.entity.LogTrap;
import com.codeops.logger.entity.TrapCondition;
import com.codeops.logger.entity.enums.ConditionType;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.entity.enums.TrapType;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link LogTrap} entities and DTOs.
 * Includes nested mapping for {@link TrapCondition}.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface LogTrapMapper {

    @Mapping(source = "trapType", target = "trapType")
    LogTrapResponse toResponse(LogTrap entity);

    List<LogTrapResponse> toResponseList(List<LogTrap> entities);

    TrapConditionResponse toConditionResponse(TrapCondition condition);

    List<TrapConditionResponse> toConditionResponseList(List<TrapCondition> conditions);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "lastTriggeredAt", ignore = true)
    @Mapping(target = "triggerCount", constant = "0L")
    @Mapping(target = "conditions", ignore = true)
    LogTrap toEntity(CreateLogTrapRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "trap", ignore = true)
    TrapCondition toConditionEntity(CreateTrapConditionRequest request);

    /**
     * Maps a trap type string to the TrapType enum.
     */
    default TrapType mapTrapType(String trapType) {
        if (trapType == null) return null;
        return TrapType.valueOf(trapType.toUpperCase());
    }

    /**
     * Maps a condition type string to the ConditionType enum.
     */
    default ConditionType mapConditionType(String conditionType) {
        if (conditionType == null) return null;
        return ConditionType.valueOf(conditionType.toUpperCase());
    }

    /**
     * Maps a log level string to the LogLevel enum.
     */
    default LogLevel mapLogLevel(String logLevel) {
        if (logLevel == null) return null;
        return LogLevel.valueOf(logLevel.toUpperCase());
    }
}
