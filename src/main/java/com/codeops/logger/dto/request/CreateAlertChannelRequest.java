package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating an alert notification channel.
 */
public record CreateAlertChannelRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotBlank(message = "Channel type is required")
        String channelType,

        @NotBlank(message = "Configuration is required")
        String configuration
) {}
