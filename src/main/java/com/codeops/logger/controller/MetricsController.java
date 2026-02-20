package com.codeops.logger.controller;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.MetricQueryRequest;
import com.codeops.logger.dto.request.PushMetricDataRequest;
import com.codeops.logger.dto.request.RegisterMetricRequest;
import com.codeops.logger.dto.request.UpdateMetricRequest;
import com.codeops.logger.dto.response.MetricAggregationResponse;
import com.codeops.logger.dto.response.MetricDataPointResponse;
import com.codeops.logger.dto.response.MetricResponse;
import com.codeops.logger.dto.response.MetricTimeSeriesResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.ServiceMetricsSummaryResponse;
import com.codeops.logger.service.MetricsService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for application metrics management.
 * Supports metric registration, data push, time-series queries, and aggregations.
 */
@RestController
@RequestMapping(AppConstants.API_PREFIX + "/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Application metrics registration, data ingestion, and querying")
public class MetricsController extends BaseController {

    private final MetricsService metricsService;

    /**
     * Registers a new metric definition.
     *
     * @param request     the metric registration data
     * @param httpRequest the HTTP request for team ID extraction
     * @return the registered metric
     */
    @PostMapping
    @Operation(summary = "Register a new metric")
    public ResponseEntity<MetricResponse> registerMetric(@Valid @RequestBody RegisterMetricRequest request,
                                                         HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        MetricResponse response = metricsService.registerMetric(request, teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all metrics for the specified team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @return the list of metrics
     */
    @GetMapping
    @Operation(summary = "List all metrics for a team")
    public ResponseEntity<List<MetricResponse>> getMetricsByTeam(HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(metricsService.getMetricsByTeam(teamId));
    }

    /**
     * Lists metrics for the specified team with pagination.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param page        zero-based page index
     * @param size        page size
     * @return the paginated metric list
     */
    @GetMapping("/paged")
    @Operation(summary = "List metrics with pagination")
    public ResponseEntity<PageResponse<MetricResponse>> getMetricsByTeamPaged(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(metricsService.getMetricsByTeamPaged(teamId, page, size));
    }

    /**
     * Lists metrics for a specific service within a team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param serviceName the service name to filter by
     * @return the list of metrics for the specified service
     */
    @GetMapping("/service/{serviceName}")
    @Operation(summary = "List metrics by service name")
    public ResponseEntity<List<MetricResponse>> getMetricsByService(
            HttpServletRequest httpRequest,
            @PathVariable String serviceName) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(metricsService.getMetricsByService(teamId, serviceName));
    }

    /**
     * Returns a summary of metrics for a specific service within a team.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param serviceName the service name to summarize
     * @return the metrics summary for the specified service
     */
    @GetMapping("/service/{serviceName}/summary")
    @Operation(summary = "Get metrics summary for a service")
    public ResponseEntity<ServiceMetricsSummaryResponse> getServiceMetricsSummary(
            HttpServletRequest httpRequest,
            @PathVariable String serviceName) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(metricsService.getServiceMetricsSummary(teamId, serviceName));
    }

    /**
     * Retrieves a single metric by its ID.
     *
     * @param metricId the metric UUID
     * @return the metric details
     */
    @GetMapping("/{metricId}")
    @Operation(summary = "Get a metric by ID")
    public ResponseEntity<MetricResponse> getMetric(@PathVariable UUID metricId) {
        return ResponseEntity.ok(metricsService.getMetric(metricId));
    }

    /**
     * Updates a metric's mutable fields (description, unit, tags).
     *
     * @param metricId the metric UUID
     * @param request  the update data
     * @return the updated metric
     */
    @PutMapping("/{metricId}")
    @Operation(summary = "Update a metric")
    public ResponseEntity<MetricResponse> updateMetric(@PathVariable UUID metricId,
                                                       @Valid @RequestBody UpdateMetricRequest request) {
        return ResponseEntity.ok(metricsService.updateMetric(metricId, request));
    }

    /**
     * Deletes a metric and all its associated data points.
     *
     * @param metricId the metric UUID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{metricId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a metric")
    public ResponseEntity<Void> deleteMetric(@PathVariable UUID metricId) {
        metricsService.deleteMetric(metricId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Pushes metric data points for recording.
     *
     * @param request     the data points to push
     * @param httpRequest the HTTP request for team ID extraction
     * @return the count of ingested data points
     */
    @PostMapping("/data")
    @Operation(summary = "Push metric data points")
    public ResponseEntity<Map<String, Object>> pushMetricData(
            @Valid @RequestBody PushMetricDataRequest request,
            HttpServletRequest httpRequest) {
        UUID teamId = extractTeamId(httpRequest);
        int count = metricsService.pushMetricData(request, teamId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ingested", count,
                "total", request.dataPoints().size()
        ));
    }

    /**
     * Queries raw time-series data for a metric within a time range.
     *
     * @param metricId  the metric UUID
     * @param startTime the range start (inclusive)
     * @param endTime   the range end (inclusive)
     * @return the raw time-series data
     */
    @GetMapping("/{metricId}/timeseries")
    @Operation(summary = "Get raw time-series data for a metric")
    public ResponseEntity<MetricTimeSeriesResponse> getTimeSeries(
            @PathVariable UUID metricId,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {
        return ResponseEntity.ok(metricsService.getTimeSeries(metricId, startTime, endTime));
    }

    /**
     * Queries aggregated time-series data for a metric within a time range.
     *
     * @param metricId   the metric UUID
     * @param startTime  the range start (inclusive)
     * @param endTime    the range end (inclusive)
     * @param resolution the aggregation window in seconds
     * @return the aggregated time-series data
     */
    @GetMapping("/{metricId}/timeseries/aggregated")
    @Operation(summary = "Get aggregated time-series data for a metric")
    public ResponseEntity<MetricTimeSeriesResponse> getTimeSeriesAggregated(
            @PathVariable UUID metricId,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime,
            @RequestParam(defaultValue = "60") int resolution) {
        return ResponseEntity.ok(metricsService.getTimeSeriesAggregated(metricId, startTime, endTime, resolution));
    }

    /**
     * Returns statistical aggregation (sum, avg, min, max, percentiles) for a metric
     * within a time range.
     *
     * @param metricId  the metric UUID
     * @param startTime the range start (inclusive)
     * @param endTime   the range end (inclusive)
     * @return the aggregated statistics
     */
    @GetMapping("/{metricId}/aggregation")
    @Operation(summary = "Get aggregated statistics for a metric")
    public ResponseEntity<MetricAggregationResponse> getAggregation(
            @PathVariable UUID metricId,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime) {
        return ResponseEntity.ok(metricsService.getAggregation(metricId, startTime, endTime));
    }

    /**
     * Returns the latest data point value for a metric, or 204 if no data exists.
     *
     * @param metricId the metric UUID
     * @return the latest data point or 204 No Content
     */
    @GetMapping("/{metricId}/latest")
    @Operation(summary = "Get the latest value for a metric")
    public ResponseEntity<MetricDataPointResponse> getLatestValue(@PathVariable UUID metricId) {
        Optional<MetricDataPointResponse> latest = metricsService.getLatestValue(metricId);
        return latest.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Returns the latest metric values for all metrics within a specific service.
     *
     * @param httpRequest the HTTP request for team ID extraction
     * @param serviceName the service name
     * @return a map of metric name to latest value
     */
    @GetMapping("/service/{serviceName}/latest")
    @Operation(summary = "Get latest values for all metrics of a service")
    public ResponseEntity<Map<String, Double>> getLatestValuesByService(
            HttpServletRequest httpRequest,
            @PathVariable String serviceName) {
        UUID teamId = extractTeamId(httpRequest);
        return ResponseEntity.ok(metricsService.getLatestValuesByService(teamId, serviceName));
    }
}
