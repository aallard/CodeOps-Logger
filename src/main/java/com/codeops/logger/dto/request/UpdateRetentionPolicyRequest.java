package com.codeops.logger.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a retention policy.
 */
public record UpdateRetentionPolicyRequest(
        @Size(max = 200) String name,
        @Size(max = 200) String sourceName,
        String logLevel,
        @Min(1) @Max(365) Integer retentionDays,
        String action,
        @Size(max = 500) String archiveDestination,
        Boolean isActive
) {}
