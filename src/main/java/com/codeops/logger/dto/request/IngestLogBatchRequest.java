package com.codeops.logger.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for batch ingestion of multiple log entries.
 */
public record IngestLogBatchRequest(
        @NotEmpty(message = "Entries list must not be empty")
        @Size(max = 1000, message = "Batch must not exceed 1000 entries")
        List<@Valid IngestLogEntryRequest> entries
) {}
