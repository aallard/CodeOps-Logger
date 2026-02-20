package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a log source.
 */
public record UpdateLogSourceRequest(
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @Size(max = 50, message = "Environment must not exceed 50 characters")
        String environment,

        Boolean isActive
) {}
