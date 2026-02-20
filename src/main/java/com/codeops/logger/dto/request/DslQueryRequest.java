package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for executing a DSL (SQL-like) log query.
 */
public record DslQueryRequest(
        @NotBlank(message = "Query is required")
        @Size(max = 2000, message = "Query must not exceed 2000 characters")
        String query,

        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size
) {}
