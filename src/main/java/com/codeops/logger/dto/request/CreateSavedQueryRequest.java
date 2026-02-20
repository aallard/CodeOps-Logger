package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for saving a log query.
 */
public record CreateSavedQueryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200) String name,

        @Size(max = 5000) String description,

        @NotBlank(message = "Query JSON is required")
        String queryJson,

        String queryDsl,

        Boolean isShared
) {}
