package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateSavedQueryRequest;
import com.codeops.logger.dto.request.DslQueryRequest;
import com.codeops.logger.dto.request.LogQueryRequest;
import com.codeops.logger.dto.request.UpdateSavedQueryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.QueryHistoryResponse;
import com.codeops.logger.dto.response.SavedQueryResponse;
import com.codeops.logger.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for log querying, saved queries, and query history.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Log Query", description = "Query, search, and manage saved queries")
public class LogQueryController extends BaseController {

    private final LogQueryService logQueryService;

    /**
     * Executes a structured log query with field-level filters.
     *
     * @param request     the query parameters
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated log entry results
     */
    @PostMapping("/query")
    @Operation(summary = "Execute a structured log query")
    public ResponseEntity<PageResponse<LogEntryResponse>> query(@Valid @RequestBody LogQueryRequest request,
                                                                 HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(logQueryService.query(request, teamId, userId));
    }

    /**
     * Performs full-text search across log entries.
     *
     * @param q           the search term
     * @param startTime   optional time range start
     * @param endTime     optional time range end
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated results
     */
    @GetMapping("/search")
    @Operation(summary = "Full-text search across log entries")
    public ResponseEntity<PageResponse<LogEntryResponse>> search(
            @RequestParam String q,
            @RequestParam(required = false) Instant startTime,
            @RequestParam(required = false) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(logQueryService.search(q, teamId, startTime, endTime, page, size));
    }

    /**
     * Executes a DSL (SQL-like) log query.
     *
     * @param request     the DSL query
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated results
     */
    @PostMapping("/dsl")
    @Operation(summary = "Execute a DSL query")
    public ResponseEntity<PageResponse<LogEntryResponse>> executeDsl(@Valid @RequestBody DslQueryRequest request,
                                                                      HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : AppConstants.DEFAULT_PAGE_SIZE;
        return ResponseEntity.ok(logQueryService.executeDsl(request.query(), teamId, userId, page, size));
    }

    /**
     * Retrieves a single log entry by ID.
     *
     * @param id the log entry ID
     * @return the log entry
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a single log entry by ID")
    public ResponseEntity<LogEntryResponse> getLogEntry(@PathVariable UUID id) {
        return ResponseEntity.ok(logQueryService.getLogEntry(id));
    }

    // ==================== Saved Queries ====================

    /**
     * Saves a query for later reuse.
     *
     * @param request     the saved query definition
     * @param httpRequest the HTTP request for team ID extraction
     * @return the saved query
     */
    @PostMapping("/queries/saved")
    @Operation(summary = "Save a query")
    public ResponseEntity<SavedQueryResponse> saveQuery(@Valid @RequestBody CreateSavedQueryRequest request,
                                                         HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(logQueryService.saveQuery(request, teamId, userId));
    }

    /**
     * Lists all saved queries visible to the user (own + shared team queries).
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return list of saved queries
     */
    @GetMapping("/queries/saved")
    @Operation(summary = "List saved queries")
    public ResponseEntity<List<SavedQueryResponse>> getSavedQueries(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(logQueryService.getSavedQueries(teamId, userId));
    }

    /**
     * Retrieves a single saved query by ID.
     *
     * @param queryId the saved query ID
     * @return the saved query
     */
    @GetMapping("/queries/saved/{queryId}")
    @Operation(summary = "Get a saved query")
    public ResponseEntity<SavedQueryResponse> getSavedQuery(@PathVariable UUID queryId) {
        return ResponseEntity.ok(logQueryService.getSavedQuery(queryId));
    }

    /**
     * Updates a saved query.
     *
     * @param queryId the saved query ID
     * @param request the update data
     * @return the updated saved query
     */
    @PutMapping("/queries/saved/{queryId}")
    @Operation(summary = "Update a saved query")
    public ResponseEntity<SavedQueryResponse> updateSavedQuery(@PathVariable UUID queryId,
                                                                @Valid @RequestBody UpdateSavedQueryRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(logQueryService.updateSavedQuery(queryId, request, userId));
    }

    /**
     * Deletes a saved query.
     *
     * @param queryId the saved query ID
     * @return 204 No Content
     */
    @DeleteMapping("/queries/saved/{queryId}")
    @Operation(summary = "Delete a saved query")
    public ResponseEntity<Void> deleteSavedQuery(@PathVariable UUID queryId) {
        UUID userId = getCurrentUserId();
        logQueryService.deleteSavedQuery(queryId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Executes a previously saved query.
     *
     * @param queryId     the saved query ID
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated results
     */
    @PostMapping("/queries/saved/{queryId}/execute")
    @Operation(summary = "Execute a saved query")
    public ResponseEntity<PageResponse<LogEntryResponse>> executeSavedQuery(
            @PathVariable UUID queryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(logQueryService.executeSavedQuery(queryId, teamId, userId, page, size));
    }

    /**
     * Returns the current user's query history.
     *
     * @param page page number
     * @param size page size
     * @return paginated query history
     */
    @GetMapping("/queries/history")
    @Operation(summary = "Get query history")
    public ResponseEntity<PageResponse<QueryHistoryResponse>> getQueryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(logQueryService.getQueryHistory(userId, page, size));
    }
}
