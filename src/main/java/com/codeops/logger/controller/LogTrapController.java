package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateLogTrapRequest;
import com.codeops.logger.dto.request.TestTrapRequest;
import com.codeops.logger.dto.request.UpdateLogTrapRequest;
import com.codeops.logger.dto.response.LogTrapResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.TrapTestResult;
import com.codeops.logger.service.LogTrapService;
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
 * REST controller for managing log traps and testing trap definitions.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/traps")
@RequiredArgsConstructor
@Tag(name = "Log Traps", description = "Manage log traps and test definitions")
public class LogTrapController extends BaseController {

    private final LogTrapService logTrapService;

    /**
     * Creates a new log trap with conditions.
     *
     * @param request     the trap definition
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created trap
     */
    @PostMapping
    @Operation(summary = "Create a log trap")
    public ResponseEntity<LogTrapResponse> createTrap(@Valid @RequestBody CreateLogTrapRequest request,
                                                       HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(logTrapService.createTrap(request, teamId, userId));
    }

    /**
     * Lists all traps for the team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return list of traps
     */
    @GetMapping
    @Operation(summary = "List traps for the team")
    public ResponseEntity<List<LogTrapResponse>> getTraps(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(logTrapService.getTrapsByTeam(teamId));
    }

    /**
     * Lists traps with pagination.
     *
     * @param page        page number
     * @param size        page size
     * @param httpRequest the HTTP request for team ID extraction
     * @return paginated traps
     */
    @GetMapping("/paged")
    @Operation(summary = "List traps with pagination")
    public ResponseEntity<PageResponse<LogTrapResponse>> getTrapsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(logTrapService.getTrapsByTeamPaged(teamId, page, size));
    }

    /**
     * Gets a trap by ID.
     *
     * @param trapId the trap ID
     * @return the trap
     */
    @GetMapping("/{trapId}")
    @Operation(summary = "Get a trap by ID")
    public ResponseEntity<LogTrapResponse> getTrap(@PathVariable UUID trapId) {
        return ResponseEntity.ok(logTrapService.getTrap(trapId));
    }

    /**
     * Updates a trap.
     *
     * @param trapId  the trap ID
     * @param request the update data
     * @return the updated trap
     */
    @PutMapping("/{trapId}")
    @Operation(summary = "Update a trap")
    public ResponseEntity<LogTrapResponse> updateTrap(@PathVariable UUID trapId,
                                                       @Valid @RequestBody UpdateLogTrapRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(logTrapService.updateTrap(trapId, request, userId));
    }

    /**
     * Deletes a trap.
     *
     * @param trapId the trap ID
     * @return 204 No Content
     */
    @DeleteMapping("/{trapId}")
    @Operation(summary = "Delete a trap")
    public ResponseEntity<Void> deleteTrap(@PathVariable UUID trapId) {
        logTrapService.deleteTrap(trapId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggles a trap's active status.
     *
     * @param trapId the trap ID
     * @return the updated trap
     */
    @PostMapping("/{trapId}/toggle")
    @Operation(summary = "Toggle a trap's active status")
    public ResponseEntity<LogTrapResponse> toggleTrap(@PathVariable UUID trapId) {
        return ResponseEntity.ok(logTrapService.toggleTrap(trapId));
    }

    /**
     * Tests a saved trap against historical logs.
     *
     * @param trapId  the trap ID
     * @param request the test parameters
     * @return test results with match count and sample IDs
     */
    @PostMapping("/{trapId}/test")
    @Operation(summary = "Test a trap against historical logs")
    public ResponseEntity<TrapTestResult> testTrap(@PathVariable UUID trapId,
                                                    @Valid @RequestBody TestTrapRequest request) {
        return ResponseEntity.ok(logTrapService.testTrap(trapId, request.hoursBack()));
    }

    /**
     * Tests a trap definition (before saving) against historical logs.
     *
     * @param request     the trap definition to test
     * @param hoursBack   hours of history to scan
     * @param httpRequest the HTTP request for team ID extraction
     * @return test results
     */
    @PostMapping("/test")
    @Operation(summary = "Test a trap definition before saving")
    public ResponseEntity<TrapTestResult> testTrapDefinition(@Valid @RequestBody CreateLogTrapRequest request,
                                                              @RequestParam(defaultValue = "24") int hoursBack,
                                                              HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(logTrapService.testTrapDefinition(request, teamId, hoursBack));
    }
}
