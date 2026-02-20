package com.codeops.logger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a dashboard from a template.
 */
public record CreateFromTemplateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200) String name,

        @NotNull(message = "Template ID is required")
        UUID templateId
) {}
