package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a saved query.
 */
public record UpdateSavedQueryRequest(
        @Size(max = 200) String name,
        @Size(max = 5000) String description,
        String queryJson,
        String queryDsl,
        Boolean isShared
) {}
