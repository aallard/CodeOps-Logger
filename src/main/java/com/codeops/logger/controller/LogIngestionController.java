package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.IngestLogBatchRequest;
import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.service.LogIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for log entry ingestion via HTTP push.
 * Supports single and batch ingestion of log entries.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Log Ingestion", description = "Ingest log entries via HTTP push")
public class LogIngestionController extends BaseController {

    private final LogIngestionService logIngestionService;

    /**
     * Ingests a single log entry.
     *
     * @param request     the log entry data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the persisted log entry
     */
    @PostMapping
    @Operation(summary = "Ingest a single log entry")
    public ResponseEntity<LogEntryResponse> ingest(@Valid @RequestBody IngestLogEntryRequest request,
                                                    HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        LogEntryResponse response = logIngestionService.ingest(request, teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Ingests a batch of log entries.
     *
     * @param request     the batch of log entries
     * @param httpRequest the HTTP request for team ID extraction
     * @return the count of successfully ingested entries
     */
    @PostMapping("/batch")
    @Operation(summary = "Ingest a batch of log entries")
    public ResponseEntity<Map<String, Object>> ingestBatch(@Valid @RequestBody IngestLogBatchRequest request,
                                                            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        int count = logIngestionService.ingestBatch(request.entries(), teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ingested", count,
                "total", request.entries().size()
        ));
    }
}
