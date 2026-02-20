package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for acknowledging or resolving a fired alert.
 */
public record UpdateAlertStatusRequest(
        @NotBlank(message = "Status is required")
        String status
) {}
