package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.CreateBaselineRequest;
import com.codeops.logger.dto.request.UpdateBaselineRequest;
import com.codeops.logger.dto.response.AnomalyBaselineResponse;
import com.codeops.logger.dto.response.AnomalyCheckResponse;
import com.codeops.logger.dto.response.AnomalyReportResponse;
import com.codeops.logger.service.AnomalyDetectionService;
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
 * REST controller for anomaly detection management.
 * Supports baseline CRUD, single-metric anomaly checks, and full team reports.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/anomalies")
@RequiredArgsConstructor
@Tag(name = "Anomaly Detection", description = "Anomaly baselines, checks, and reports")
public class AnomalyController extends BaseController {

    private final AnomalyDetectionService anomalyDetectionService;

    /**
     * Creates or updates an anomaly baseline for a service metric.
     *
     * @param request     the baseline configuration data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the created or updated baseline
     */
    @PostMapping("/baselines")
    @Operation(summary = "Create or update an anomaly baseline")
    public ResponseEntity<AnomalyBaselineResponse> createOrUpdateBaseline(
            @Valid @RequestBody CreateBaselineRequest request,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        AnomalyBaselineResponse response = anomalyDetectionService.createOrUpdateBaseline(request, teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all anomaly baselines for the specified team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return the list of baselines
     */
    @GetMapping("/baselines")
    @Operation(summary = "List baselines for a team")
    public ResponseEntity<List<AnomalyBaselineResponse>> getBaselinesByTeam(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(anomalyDetectionService.getBaselinesByTeam(teamId));
    }

    /**
     * Lists anomaly baselines for a specific service within a team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param serviceName the service name to filter by
     * @return the list of baselines for the specified service
     */
    @GetMapping("/baselines/service/{serviceName}")
    @Operation(summary = "List baselines by service name")
    public ResponseEntity<List<AnomalyBaselineResponse>> getBaselinesByService(
            HttpServletRequest httpRequest,
            @PathVariable String serviceName) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(anomalyDetectionService.getBaselinesByService(teamId, serviceName));
    }

    /**
     * Retrieves a single anomaly baseline by its ID.
     *
     * @param baselineId the baseline UUID
     * @return the baseline details
     */
    @GetMapping("/baselines/{baselineId}")
    @Operation(summary = "Get a baseline by ID")
    public ResponseEntity<AnomalyBaselineResponse> getBaseline(@PathVariable UUID baselineId) {
        return ResponseEntity.ok(anomalyDetectionService.getBaseline(baselineId));
    }

    /**
     * Updates a baseline's mutable fields (window hours, deviation threshold, active status).
     *
     * @param baselineId the baseline UUID
     * @param request    the update data
     * @return the updated baseline
     */
    @PutMapping("/baselines/{baselineId}")
    @Operation(summary = "Update a baseline")
    public ResponseEntity<AnomalyBaselineResponse> updateBaseline(
            @PathVariable UUID baselineId,
            @Valid @RequestBody UpdateBaselineRequest request) {
        return ResponseEntity.ok(anomalyDetectionService.updateBaseline(baselineId, request));
    }

    /**
     * Deletes an anomaly baseline.
     *
     * @param baselineId the baseline UUID
     * @return 204 No Content on success
     */
    @DeleteMapping("/baselines/{baselineId}")
    @Operation(summary = "Delete a baseline")
    public ResponseEntity<Void> deleteBaseline(@PathVariable UUID baselineId) {
        anomalyDetectionService.deleteBaseline(baselineId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Checks a single metric for anomalous behavior against its baseline.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param serviceName the service name
     * @param metricName  the metric name to check
     * @return the anomaly check result
     */
    @GetMapping("/check")
    @Operation(summary = "Check a single metric for anomalies")
    public ResponseEntity<AnomalyCheckResponse> checkAnomaly(
            HttpServletRequest httpRequest,
            @RequestParam String serviceName,
            @RequestParam String metricName) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(anomalyDetectionService.checkAnomaly(teamId, serviceName, metricName));
    }

    /**
     * Runs a full anomaly check across all baselines for a team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return the full anomaly report
     */
    @GetMapping("/report")
    @Operation(summary = "Run a full anomaly check for a team")
    public ResponseEntity<AnomalyReportResponse> runFullCheck(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(anomalyDetectionService.runFullCheck(teamId));
    }
}
