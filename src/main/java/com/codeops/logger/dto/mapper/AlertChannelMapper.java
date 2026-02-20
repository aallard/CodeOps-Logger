package com.codeops.logger.dto.mapper;

import com.codeops.logger.dto.request.CreateAlertChannelRequest;
import com.codeops.logger.dto.response.AlertChannelResponse;
import com.codeops.logger.entity.AlertChannel;
import com.codeops.logger.entity.enums.AlertChannelType;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link AlertChannel} entities and DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AlertChannelMapper {

    AlertChannelResponse toResponse(AlertChannel entity);

    List<AlertChannelResponse> toResponseList(List<AlertChannel> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    AlertChannel toEntity(CreateAlertChannelRequest request);

    /**
     * Maps a channel type string to the AlertChannelType enum.
     */
    default AlertChannelType mapChannelType(String channelType) {
        if (channelType == null) return null;
        return AlertChannelType.valueOf(channelType.toUpperCase());
    }
}
