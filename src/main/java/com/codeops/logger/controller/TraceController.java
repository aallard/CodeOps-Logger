package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateTraceSpanRequest;
import com.codeops.logger.dto.response.RootCauseAnalysisResponse;
import com.codeops.logger.dto.response.TraceFlowResponse;
import com.codeops.logger.dto.response.TraceListResponse;
import com.codeops.logger.dto.response.TraceSpanResponse;
import com.codeops.logger.dto.response.TraceWaterfallResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for distributed tracing operations.
 * Supports span ingestion, trace flow visualization, waterfall views,
 * root cause analysis, and trace listing.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/traces")
@RequiredArgsConstructor
@Tag(name = "Traces", description = "Distributed tracing: span ingestion, flow visualization, and analysis")
public class TraceController extends BaseController {

    private final TraceService traceService;

    /**
     * Creates a single trace span.
     *
     * @param request     the span data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created span
     */
    @PostMapping("/spans")
    @Operation(summary = "Create a trace span")
    public ResponseEntity<TraceSpanResponse> createSpan(@Valid @RequestBody CreateTraceSpanRequest request,
                                                        HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        TraceSpanResponse response = traceService.createSpan(request, teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a batch of trace spans.
     *
     * @param requests    the list of span data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the count of created spans
     */
    @PostMapping("/spans/batch")
    @Operation(summary = "Create a batch of trace spans")
    public ResponseEntity<Map<String, Object>> createSpanBatch(
            @Valid @RequestBody List<CreateTraceSpanRequest> requests,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        List<TraceSpanResponse> created = traceService.createSpanBatch(requests, teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "created", created.size(),
                "total", requests.size()
        ));
    }

    /**
     * Retrieves a single span by its ID.
     *
     * @param spanId the span UUID
     * @return the span details
     */
    @GetMapping("/spans/{spanId}")
    @Operation(summary = "Get a span by ID")
    public ResponseEntity<TraceSpanResponse> getSpan(@PathVariable UUID spanId) {
        return ResponseEntity.ok(traceService.getSpan(spanId));
    }

    /**
     * Retrieves the trace flow for a given correlation ID.
     *
     * @param correlationId the correlation ID
     * @return the trace flow with all spans
     */
    @GetMapping("/flow/{correlationId}")
    @Operation(summary = "Get trace flow by correlation ID")
    public ResponseEntity<TraceFlowResponse> getTraceFlow(@PathVariable String correlationId) {
        return ResponseEntity.ok(traceService.getTraceFlow(correlationId));
    }

    /**
     * Retrieves the trace flow for a given trace ID.
     *
     * @param traceId the trace ID
     * @return the trace flow with all spans
     */
    @GetMapping("/flow/by-trace-id/{traceId}")
    @Operation(summary = "Get trace flow by trace ID")
    public ResponseEntity<TraceFlowResponse> getTraceFlowByTraceId(@PathVariable String traceId) {
        return ResponseEntity.ok(traceService.getTraceFlowByTraceId(traceId));
    }

    /**
     * Retrieves the waterfall visualization for a given correlation ID.
     *
     * @param correlationId the correlation ID
     * @return the waterfall view with timing offsets and depths
     */
    @GetMapping("/waterfall/{correlationId}")
    @Operation(summary = "Get trace waterfall by correlation ID")
    public ResponseEntity<TraceWaterfallResponse> getWaterfall(@PathVariable String correlationId) {
        return ResponseEntity.ok(traceService.getWaterfall(correlationId));
    }

    /**
     * Performs root cause analysis for a given correlation ID.
     * Returns 204 if no errors are found in the trace.
     *
     * @param correlationId the correlation ID
     * @return the root cause analysis or 204 No Content
     */
    @GetMapping("/rca/{correlationId}")
    @Operation(summary = "Get root cause analysis by correlation ID")
    public ResponseEntity<RootCauseAnalysisResponse> getRootCauseAnalysis(@PathVariable String correlationId) {
        Optional<RootCauseAnalysisResponse> rca = traceService.getRootCauseAnalysis(correlationId);
        return rca.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Lists recent traces for a team with pagination.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param page        zero-based page index
     * @param size        page size
     * @return the paginated list of recent traces
     */
    @GetMapping
    @Operation(summary = "List recent traces for a team")
    public ResponseEntity<PageResponse<TraceListResponse>> listRecentTraces(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(traceService.listRecentTraces(teamId, page, size));
    }

    /**
     * Lists traces for a specific service within a team with pagination.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param serviceName the service name to filter by
     * @param page        zero-based page index
     * @param size        page size
     * @return the paginated list of traces for the specified service
     */
    @GetMapping("/service/{serviceName}")
    @Operation(summary = "List traces by service name")
    public ResponseEntity<PageResponse<TraceListResponse>> listTracesByService(
            HttpServletRequest httpRequest,
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(traceService.listTracesByService(teamId, serviceName, page, size));
    }

    /**
     * Lists traces containing errors for a team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param limit       the maximum number of error traces to return
     * @return the list of error traces
     */
    @GetMapping("/errors")
    @Operation(summary = "List error traces for a team")
    public ResponseEntity<List<TraceListResponse>> listErrorTraces(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "20") int limit) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(traceService.listErrorTraces(teamId, limit));
    }

    /**
     * Retrieves log entry IDs related to a given correlation ID.
     *
     * @param correlationId the correlation ID
     * @return the list of related log entry UUIDs
     */
    @GetMapping("/{correlationId}/logs")
    @Operation(summary = "Get related log entries for a trace")
    public ResponseEntity<List<UUID>> getRelatedLogEntries(@PathVariable String correlationId) {
        return ResponseEntity.ok(traceService.getRelatedLogEntries(correlationId));
    }
}
