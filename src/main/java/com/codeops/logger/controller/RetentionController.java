package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateRetentionPolicyRequest;
import com.codeops.logger.dto.request.UpdateRetentionPolicyRequest;
import com.codeops.logger.dto.response.RetentionPolicyResponse;
import com.codeops.logger.dto.response.StorageUsageResponse;
import com.codeops.logger.service.RetentionExecutor;
import com.codeops.logger.service.RetentionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for data retention policy management.
 * Supports CRUD operations on retention policies, manual execution, and storage usage.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/retention")
@RequiredArgsConstructor
@Tag(name = "Retention", description = "Data retention policy management and storage monitoring")
public class RetentionController extends BaseController {

    private final RetentionService retentionService;
    private final RetentionExecutor retentionExecutor;

    /**
     * Creates a new retention policy.
     *
     * @param request     the policy creation data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created retention policy
     */
    @PostMapping("/policies")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a retention policy")
    public ResponseEntity<RetentionPolicyResponse> createPolicy(
            @Valid @RequestBody CreateRetentionPolicyRequest request,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        UUID userId = getCurrentUserId();
        RetentionPolicyResponse response = retentionService.createPolicy(request, teamId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all retention policies for the specified team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return the list of retention policies
     */
    @GetMapping("/policies")
    @Operation(summary = "List retention policies for a team")
    public ResponseEntity<List<RetentionPolicyResponse>> getPoliciesByTeam(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(retentionService.getPoliciesByTeam(teamId));
    }

    /**
     * Retrieves a single retention policy by its ID.
     *
     * @param policyId    the policy UUID
     * @param httpRequest the HTTP request for team ID extraction
     * @return the retention policy details
     */
    @GetMapping("/policies/{policyId}")
    @Operation(summary = "Get a retention policy by ID")
    public ResponseEntity<RetentionPolicyResponse> getPolicy(
            @PathVariable UUID policyId,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(retentionService.getPolicy(policyId, teamId));
    }

    /**
     * Updates a retention policy's mutable fields.
     *
     * @param policyId    the policy UUID
     * @param request     the update data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the updated retention policy
     */
    @PutMapping("/policies/{policyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a retention policy")
    public ResponseEntity<RetentionPolicyResponse> updatePolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody UpdateRetentionPolicyRequest request,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(retentionService.updatePolicy(policyId, request, teamId));
    }

    /**
     * Deletes a retention policy.
     *
     * @param policyId    the policy UUID
     * @param httpRequest the HTTP request for team ID extraction
     * @return 204 No Content on success
     */
    @DeleteMapping("/policies/{policyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a retention policy")
    public ResponseEntity<Void> deletePolicy(
            @PathVariable UUID policyId,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        retentionService.deletePolicy(policyId, teamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggles a retention policy's active status.
     *
     * @param policyId    the policy UUID
     * @param body        request body containing the {@code active} boolean
     * @param httpRequest the HTTP request for team ID extraction
     * @return the updated retention policy
     */
    @PutMapping("/policies/{policyId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle a retention policy active/inactive")
    public ResponseEntity<RetentionPolicyResponse> togglePolicy(
            @PathVariable UUID policyId,
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(retentionService.togglePolicyActive(policyId, teamId, active));
    }

    /**
     * Manually executes a specific retention policy.
     *
     * @param policyId    the policy UUID
     * @param httpRequest the HTTP request for team ID extraction
     * @return confirmation of execution
     */
    @PostMapping("/policies/{policyId}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually execute a retention policy")
    public ResponseEntity<Map<String, String>> executePolicy(
            @PathVariable UUID policyId,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        retentionExecutor.manualExecute(policyId, teamId);
        return ResponseEntity.ok(Map.of("status", "executed", "policyId", policyId.toString()));
    }

    /**
     * Returns storage usage statistics across all data stores.
     *
     * @return the storage usage report
     */
    @GetMapping("/storage")
    @Operation(summary = "Get storage usage statistics")
    public ResponseEntity<StorageUsageResponse> getStorageUsage() {
        return ResponseEntity.ok(retentionService.getStorageUsage());
    }
}
