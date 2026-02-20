package com.codeops.logger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a log trap with conditions.
 */
public record CreateLogTrapRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        String description,

        @NotBlank(message = "Trap type is required")
        String trapType,

        @NotEmpty(message = "At least one condition is required")
        @Size(max = 10, message = "Maximum 10 conditions per trap")
        List<@Valid CreateTrapConditionRequest> conditions
) {}
