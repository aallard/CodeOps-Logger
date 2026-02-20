package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.MetricMapper;
import com.codeops.logger.dto.request.PushMetricDataRequest;
import com.codeops.logger.dto.request.RegisterMetricRequest;
import com.codeops.logger.dto.request.UpdateMetricRequest;
import com.codeops.logger.dto.response.*;
import com.codeops.logger.entity.Metric;
import com.codeops.logger.entity.MetricSeries;
import com.codeops.logger.entity.enums.MetricType;
import com.codeops.logger.exception.AuthorizationException;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.MetricRepository;
import com.codeops.logger.repository.MetricSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages metric definitions, time-series data storage, and metric queries.
 * Supports registration, data push, time-series retrieval, and aggregation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final MetricRepository metricRepository;
    private final MetricSeriesRepository metricSeriesRepository;
    private final MetricMapper metricMapper;
    private final MetricAggregationService aggregationService;

    // ==================== Metric Registration ====================

    /**
     * Registers a new metric definition. If a metric with the same name, service, and team
     * already exists, returns the existing one (idempotent registration).
     *
     * @param request the metric definition
     * @param teamId  the team scope
     * @return the registered (or existing) metric response
     * @throws ValidationException if the metric type is invalid
     */
    public MetricResponse registerMetric(RegisterMetricRequest request, UUID teamId) {
        Optional<Metric> existing = metricRepository.findByTeamIdAndNameAndServiceName(
                teamId, request.name(), request.serviceName());
        if (existing.isPresent()) {
            log.debug("Metric '{}' already exists for service '{}', returning existing",
                    request.name(), request.serviceName());
            return metricMapper.toResponse(existing.get());
        }

        MetricType metricType = parseMetricType(request.metricType());

        Metric entity = metricMapper.toEntity(request);
        entity.setMetricType(metricType);
        entity.setTeamId(teamId);

        Metric saved = metricRepository.save(entity);
        log.info("Registered metric '{}' (type={}, service={}) for team {}",
                saved.getName(), metricType, saved.getServiceName(), teamId);
        return metricMapper.toResponse(saved);
    }

    /**
     * Returns all metrics for a team.
     *
     * @param teamId the team scope
     * @return list of metric responses
     */
    public List<MetricResponse> getMetricsByTeam(UUID teamId) {
        List<Metric> metrics = metricRepository.findByTeamId(teamId);
        return metricMapper.toResponseList(metrics);
    }

    /**
     * Returns paginated metrics for a team.
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated metric responses
     */
    public PageResponse<MetricResponse> getMetricsByTeamPaged(UUID teamId, int page, int size) {
        Page<Metric> springPage = metricRepository.findByTeamId(teamId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<MetricResponse> content = metricMapper.toResponseList(springPage.getContent());
        return new PageResponse<>(
                content,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }

    /**
     * Returns all metrics for a specific service within a team.
     *
     * @param teamId      the team scope
     * @param serviceName the service name
     * @return list of metric responses
     */
    public List<MetricResponse> getMetricsByService(UUID teamId, String serviceName) {
        List<Metric> metrics = metricRepository.findByTeamIdAndServiceName(teamId, serviceName);
        return metricMapper.toResponseList(metrics);
    }

    /**
     * Returns a metric summary for a service, including counts grouped by type.
     *
     * @param teamId      the team scope
     * @param serviceName the service name
     * @return summary response with metrics grouped by type
     */
    public ServiceMetricsSummaryResponse getServiceMetricsSummary(UUID teamId, String serviceName) {
        List<Metric> metrics = metricRepository.findByTeamIdAndServiceName(teamId, serviceName);
        Map<String, Long> metricsByType = metrics.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getMetricType().name(),
                        Collectors.counting()
                ));
        List<MetricResponse> responses = metricMapper.toResponseList(metrics);
        return new ServiceMetricsSummaryResponse(serviceName, metrics.size(), metricsByType, responses);
    }

    /**
     * Returns a single metric by ID.
     *
     * @param metricId the metric ID
     * @return the metric response
     * @throws NotFoundException if not found
     */
    public MetricResponse getMetric(UUID metricId) {
        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Metric not found: " + metricId));
        return metricMapper.toResponse(metric);
    }

    /**
     * Updates a metric definition (description, unit, tags only — name and type are immutable).
     *
     * @param metricId the metric ID
     * @param request  the update request
     * @return the updated metric response
     * @throws NotFoundException if not found
     */
    public MetricResponse updateMetric(UUID metricId, UpdateMetricRequest request) {
        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Metric not found: " + metricId));

        if (request.description() != null) {
            metric.setDescription(request.description());
        }
        if (request.unit() != null) {
            metric.setUnit(request.unit());
        }
        if (request.tags() != null) {
            metric.setTags(request.tags());
        }

        Metric saved = metricRepository.save(metric);
        return metricMapper.toResponse(saved);
    }

    /**
     * Deletes a metric and all its time-series data.
     *
     * @param metricId the metric ID
     * @throws NotFoundException if not found
     */
    @Transactional
    public void deleteMetric(UUID metricId) {
        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Metric not found: " + metricId));
        metricSeriesRepository.deleteByMetricId(metricId);
        metricRepository.delete(metric);
        log.info("Deleted metric '{}' ({}) and all series data", metric.getName(), metricId);
    }

    // ==================== Data Push ====================

    /**
     * Pushes metric data points for a registered metric.
     * For COUNTER metrics, logs a warning if values decrease (but does not reject).
     *
     * @param request the data points to push
     * @param teamId  the team scope (for validation)
     * @return count of successfully stored data points
     * @throws NotFoundException      if the metric is not found
     * @throws AuthorizationException if the metric does not belong to the team
     * @throws ValidationException    if batch size exceeds the maximum
     */
    @Transactional
    public int pushMetricData(PushMetricDataRequest request, UUID teamId) {
        Metric metric = metricRepository.findById(request.metricId())
                .orElseThrow(() -> new NotFoundException("Metric not found: " + request.metricId()));

        if (!metric.getTeamId().equals(teamId)) {
            throw new AuthorizationException(
                    "Metric does not belong to this team: " + request.metricId());
        }

        if (request.dataPoints().size() > AppConstants.MAX_BATCH_SIZE) {
            throw new ValidationException(
                    "Data point count exceeds maximum batch size (" + AppConstants.MAX_BATCH_SIZE + ")");
        }

        List<MetricSeries> entities = new ArrayList<>();
        Double previousValue = null;

        for (PushMetricDataRequest.MetricDataPoint dp : request.dataPoints()) {
            if (metric.getMetricType() == MetricType.COUNTER && previousValue != null
                    && dp.value() < previousValue) {
                log.warn("Counter metric '{}' received decreasing value: {} < {}",
                        metric.getName(), dp.value(), previousValue);
            }
            previousValue = dp.value();

            MetricSeries series = new MetricSeries();
            series.setMetric(metric);
            series.setTimestamp(dp.timestamp());
            series.setValue(dp.value());
            series.setTags(dp.tags());
            series.setResolution(AppConstants.DEFAULT_METRIC_RESOLUTION_SECONDS);
            entities.add(series);
        }

        metricSeriesRepository.saveAll(entities);
        log.debug("Pushed {} data points for metric '{}' ({})",
                entities.size(), metric.getName(), metric.getId());
        return entities.size();
    }

    /**
     * Auto-registers a metric and pushes a single data point in one call.
     * Convenience method for services that don't want to pre-register metrics.
     *
     * @param metricName  the metric name
     * @param metricType  the metric type
     * @param serviceName the source service
     * @param value       the metric value
     * @param teamId      the team scope
     */
    @Transactional
    public void pushSingleValue(String metricName, String metricType,
                                 String serviceName, double value, UUID teamId) {
        RegisterMetricRequest registerRequest = new RegisterMetricRequest(
                metricName, metricType, null, null, serviceName, null);
        MetricResponse registered = registerMetric(registerRequest, teamId);

        Metric metric = metricRepository.findById(registered.id())
                .orElseThrow(() -> new NotFoundException("Metric not found after registration"));

        MetricSeries series = new MetricSeries();
        series.setMetric(metric);
        series.setTimestamp(Instant.now());
        series.setValue(value);
        series.setResolution(AppConstants.DEFAULT_METRIC_RESOLUTION_SECONDS);
        metricSeriesRepository.save(series);

        log.debug("Pushed single value {} for metric '{}' (service={})",
                value, metricName, serviceName);
    }

    // ==================== Time-Series Queries ====================

    /**
     * Retrieves raw time-series data points for a metric within a time range.
     *
     * @param metricId  the metric to query
     * @param startTime range start
     * @param endTime   range end
     * @return time-series response with raw data points
     * @throws NotFoundException if the metric is not found
     */
    public MetricTimeSeriesResponse getTimeSeries(UUID metricId,
                                                    Instant startTime, Instant endTime) {
        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Metric not found: " + metricId));

        List<MetricSeries> series = metricSeriesRepository
                .findByMetricIdAndTimestampBetweenOrderByTimestampAsc(metricId, startTime, endTime);

        List<MetricTimeSeriesResponse.DataPoint> dataPoints = series.stream()
                .map(s -> new MetricTimeSeriesResponse.DataPoint(s.getTimestamp(), s.getValue(), s.getTags()))
                .toList();

        return new MetricTimeSeriesResponse(
                metricId, metric.getName(), metric.getServiceName(),
                metric.getMetricType().name(), metric.getUnit(),
                startTime, endTime, null, dataPoints);
    }

    /**
     * Retrieves time-series data aggregated at a specified resolution.
     *
     * @param metricId          the metric to query
     * @param startTime         range start
     * @param endTime           range end
     * @param resolutionSeconds aggregation window size
     * @return time-series response with aggregated data points
     * @throws NotFoundException   if the metric is not found
     * @throws ValidationException if the resolution is out of bounds
     */
    public MetricTimeSeriesResponse getTimeSeriesAggregated(UUID metricId,
                                                              Instant startTime, Instant endTime, int resolutionSeconds) {
        if (resolutionSeconds < AppConstants.MIN_METRIC_RESOLUTION_SECONDS
                || resolutionSeconds > AppConstants.MAX_METRIC_RESOLUTION_SECONDS) {
            throw new ValidationException(
                    "Resolution must be between " + AppConstants.MIN_METRIC_RESOLUTION_SECONDS
                            + " and " + AppConstants.MAX_METRIC_RESOLUTION_SECONDS + " seconds");
        }

        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Metric not found: " + metricId));

        List<MetricSeries> series = metricSeriesRepository
                .findByMetricIdAndTimestampBetweenOrderByTimestampAsc(metricId, startTime, endTime);

        List<MetricTimeSeriesResponse.DataPoint> aggregated =
                aggregationService.aggregateByResolution(series, startTime, endTime, resolutionSeconds);

        return new MetricTimeSeriesResponse(
                metricId, metric.getName(), metric.getServiceName(),
                metric.getMetricType().name(), metric.getUnit(),
                startTime, endTime, resolutionSeconds, aggregated);
    }

    /**
     * Returns full aggregation statistics for a metric over a time range.
     *
     * @param metricId  the metric
     * @param startTime range start
     * @param endTime   range end
     * @return aggregation including sum, avg, min, max, percentiles, stddev
     * @throws NotFoundException if the metric is not found
     */
    public MetricAggregationResponse getAggregation(UUID metricId,
                                                      Instant startTime, Instant endTime) {
        Metric metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new NotFoundException("Metric not found: " + metricId));

        List<MetricSeries> series = metricSeriesRepository
                .findByMetricIdAndTimestampBetweenOrderByTimestampAsc(metricId, startTime, endTime);

        List<Double> values = series.stream()
                .map(MetricSeries::getValue)
                .toList();

        MetricAggregationService.AggregationResult result = aggregationService.aggregate(values);

        return new MetricAggregationResponse(
                metricId, metric.getName(), metric.getServiceName(),
                startTime, endTime, result.count(),
                result.sum(), result.avg(), result.min(), result.max(),
                result.p50(), result.p95(), result.p99(), result.stddev());
    }

    // ==================== Latest Values ====================

    /**
     * Returns the latest value for a metric (most recent data point).
     *
     * @param metricId the metric
     * @return the most recent data point, or empty if no data
     * @throws NotFoundException if the metric is not found
     */
    public Optional<MetricDataPointResponse> getLatestValue(UUID metricId) {
        if (!metricRepository.existsById(metricId)) {
            throw new NotFoundException("Metric not found: " + metricId);
        }
        Page<MetricSeries> page = metricSeriesRepository.findByMetricId(
                metricId, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timestamp")));
        if (page.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(metricMapper.toDataPointResponse(page.getContent().getFirst()));
    }

    /**
     * Returns the latest values for all metrics of a service.
     * Useful for dashboard "current status" displays.
     *
     * @param teamId      the team scope
     * @param serviceName the service
     * @return map of metricName to latest value
     */
    public Map<String, Double> getLatestValuesByService(UUID teamId, String serviceName) {
        List<Metric> metrics = metricRepository.findByTeamIdAndServiceName(teamId, serviceName);
        Map<String, Double> latest = new LinkedHashMap<>();

        for (Metric metric : metrics) {
            Page<MetricSeries> page = metricSeriesRepository.findByMetricId(
                    metric.getId(), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timestamp")));
            if (!page.isEmpty()) {
                latest.put(metric.getName(), page.getContent().getFirst().getValue());
            }
        }

        return latest;
    }

    // ==================== Cleanup ====================

    /**
     * Deletes metric series data older than the specified cutoff.
     * Used by RetentionService for data lifecycle management.
     *
     * @param cutoff delete data points before this time
     * @return count of deleted records
     */
    @Transactional
    public long purgeOldData(Instant cutoff) {
        long countBefore = metricSeriesRepository.count();
        metricSeriesRepository.deleteByTimestampBefore(cutoff);
        long countAfter = metricSeriesRepository.count();
        long deleted = countBefore - countAfter;
        log.info("Purged {} metric series data points older than {}", deleted, cutoff);
        return deleted;
    }

    /**
     * Parses a metric type string to the MetricType enum.
     *
     * @param metricType the string to parse
     * @return the MetricType enum value
     * @throws ValidationException if the string is not a valid metric type
     */
    private MetricType parseMetricType(String metricType) {
        try {
            return MetricType.valueOf(metricType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid metric type: " + metricType);
        }
    }
}
