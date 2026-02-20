package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateLogSourceRequest;
import com.codeops.logger.dto.request.UpdateLogSourceRequest;
import com.codeops.logger.dto.response.LogSourceResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.service.LogSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing log sources (registered services/applications).
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/sources")
@RequiredArgsConstructor
@Tag(name = "Log Sources", description = "Manage log sources (services)")
public class LogSourceController extends BaseController {

    private final LogSourceService logSourceService;

    /**
     * Registers a new log source.
     *
     * @param request     the source details
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created source
     */
    @PostMapping
    @Operation(summary = "Register a new log source")
    public ResponseEntity<LogSourceResponse> createSource(@Valid @RequestBody CreateLogSourceRequest request,
                                                           HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(logSourceService.createSource(request, teamId));
    }

    /**
     * Lists all log sources for the team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return list of sources
     */
    @GetMapping
    @Operation(summary = "List log sources for the team")
    public ResponseEntity<List<LogSourceResponse>> getSources(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(logSourceService.getSourcesByTeam(teamId));
    }

    /**
     * Lists log sources with pagination.
     *
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated sources
     */
    @GetMapping("/paged")
    @Operation(summary = "List log sources with pagination")
    public ResponseEntity<PageResponse<LogSourceResponse>> getSourcesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(logSourceService.getSourcesByTeamPaged(teamId, page, size));
    }

    /**
     * Gets a log source by ID.
     *
     * @param sourceId the source ID
     * @return the source
     */
    @GetMapping("/{sourceId}")
    @Operation(summary = "Get a log source by ID")
    public ResponseEntity<LogSourceResponse> getSource(@PathVariable UUID sourceId) {
        return ResponseEntity.ok(logSourceService.getSource(sourceId));
    }

    /**
     * Updates a log source.
     *
     * @param sourceId the source ID
     * @param request  the update data
     * @return the updated source
     */
    @PutMapping("/{sourceId}")
    @Operation(summary = "Update a log source")
    public ResponseEntity<LogSourceResponse> updateSource(@PathVariable UUID sourceId,
                                                           @Valid @RequestBody UpdateLogSourceRequest request) {
        return ResponseEntity.ok(logSourceService.updateSource(sourceId, request));
    }

    /**
     * Deletes a log source.
     *
     * @param sourceId the source ID
     * @return 204 No Content
     */
    @DeleteMapping("/{sourceId}")
    @Operation(summary = "Delete a log source")
    public ResponseEntity<Void> deleteSource(@PathVariable UUID sourceId) {
        logSourceService.deleteSource(sourceId);
        return ResponseEntity.noContent().build();
    }
}
