package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for registering a new log source.
 */
public record CreateLogSourceRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        UUID serviceId,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @Size(max = 50, message = "Environment must not exceed 50 characters")
        String environment
) {}
