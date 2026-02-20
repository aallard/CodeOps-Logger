package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateRetentionPolicyRequest;
import com.codeops.logger.dto.response.RetentionPolicyResponse;
import com.codeops.logger.entity.RetentionPolicy;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.entity.enums.RetentionAction;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link RetentionPolicy} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RetentionPolicyMapper {

    RetentionPolicyResponse toResponse(RetentionPolicy entity);

    List<RetentionPolicyResponse> toResponseList(List<RetentionPolicy> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "lastExecutedAt", ignore = true)
    RetentionPolicy toEntity(CreateRetentionPolicyRequest request);

    /**
     * Maps an action string to the RetentionAction enum.
     */
    default RetentionAction mapAction(String action) {
        if (action == null) return null;
        return RetentionAction.valueOf(action.toUpperCase());
    }

    /**
     * Maps a log level string to the LogLevel enum.
     */
    default LogLevel mapLogLevel(String logLevel) {
        if (logLevel == null) return null;
        return LogLevel.valueOf(logLevel.toUpperCase());
    }
}
