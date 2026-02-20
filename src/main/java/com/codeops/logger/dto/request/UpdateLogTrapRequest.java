package com.codeops.logger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating a log trap.
 */
public record UpdateLogTrapRequest(
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        String description,

        String trapType,

        Boolean isActive,

        @Size(max = 10, message = "Maximum 10 conditions per trap")
        List<@Valid CreateTrapConditionRequest> conditions
) {}
