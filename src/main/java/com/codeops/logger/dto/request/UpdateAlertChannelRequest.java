package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an alert channel.
 */
public record UpdateAlertChannelRequest(
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        String configuration,

        Boolean isActive
) {}
