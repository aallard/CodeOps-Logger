package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.AnomalyBaselineMapper;
import com.codeops.logger.dto.request.CreateBaselineRequest;
import com.codeops.logger.dto.request.UpdateBaselineRequest;
import com.codeops.logger.dto.response.AnomalyBaselineResponse;
import com.codeops.logger.dto.response.AnomalyCheckResponse;
import com.codeops.logger.dto.response.AnomalyReportResponse;
import com.codeops.logger.entity.AnomalyBaseline;
import com.codeops.logger.entity.Metric;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.AnomalyBaselineRepository;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.MetricRepository;
import com.codeops.logger.repository.MetricSeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages anomaly detection baselines, scheduled recalculation, and anomaly checking.
 * Learns "normal" patterns from historical log volume, error rate, and metric data,
 * then detects statistically significant deviations using z-score analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnomalyDetectionService {

    private final AnomalyBaselineRepository baselineRepository;
    private final LogEntryRepository logEntryRepository;
    private final MetricSeriesRepository metricSeriesRepository;
    private final MetricRepository metricRepository;
    private final AnomalyBaselineMapper baselineMapper;
    private final AnomalyBaselineCalculator calculator;

    // ==================== Baseline CRUD ====================

    /**
     * Creates or recalculates a baseline for a service+metric combination.
     * If a baseline already exists for the same team/service/metric, it is updated
     * with freshly computed statistics.
     *
     * @param request the baseline configuration
     * @param teamId  the team scope
     * @return the computed baseline response
     * @throws ValidationException if insufficient historical data exists (minimum 24 hours)
     */
    public AnomalyBaselineResponse createOrUpdateBaseline(CreateBaselineRequest request, UUID teamId) {
        Optional<AnomalyBaseline> existing = baselineRepository
                .findByTeamIdAndServiceNameAndMetricName(teamId, request.serviceName(), request.metricName());

        int windowHours = request.windowHours() != null
                ? request.windowHours() : AppConstants.DEFAULT_BASELINE_WINDOW_HOURS;
        double threshold = request.deviationThreshold() != null
                ? request.deviationThreshold() : AppConstants.DEFAULT_DEVIATION_THRESHOLD;

        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(windowHours, ChronoUnit.HOURS);

        List<Double> hourlyData = collectHourlyData(
                teamId, request.serviceName(), request.metricName(), windowStart, windowEnd);

        Optional<AnomalyBaselineCalculator.BaselineStats> stats = calculator.computeBaseline(hourlyData);
        if (stats.isEmpty()) {
            throw new ValidationException(
                    "Insufficient data for baseline computation. At least 24 hourly data points are required. Found: "
                            + hourlyData.size());
        }

        AnomalyBaseline baseline;
        if (existing.isPresent()) {
            baseline = existing.get();
        } else {
            baseline = new AnomalyBaseline();
            baseline.setServiceName(request.serviceName());
            baseline.setMetricName(request.metricName());
            baseline.setTeamId(teamId);
            baseline.setIsActive(true);
        }

        baseline.setBaselineValue(stats.get().mean());
        baseline.setStandardDeviation(stats.get().stddev());
        baseline.setSampleCount(stats.get().sampleCount());
        baseline.setWindowStartTime(windowStart);
        baseline.setWindowEndTime(windowEnd);
        baseline.setDeviationThreshold(threshold);
        baseline.setLastComputedAt(Instant.now());

        AnomalyBaseline saved = baselineRepository.save(baseline);
        log.info("Computed baseline for {}:{} (team {}): mean={}, stddev={}, samples={}",
                request.serviceName(), request.metricName(), teamId,
                stats.get().mean(), stats.get().stddev(), stats.get().sampleCount());

        return baselineMapper.toResponse(saved);
    }

    /**
     * Returns all baselines for a team.
     *
     * @param teamId the team ID
     * @return list of baseline responses
     */
    @Transactional(readOnly = true)
    public List<AnomalyBaselineResponse> getBaselinesByTeam(UUID teamId) {
        return baselineMapper.toResponseList(baselineRepository.findByTeamId(teamId));
    }

    /**
     * Returns baselines for a specific service within a team.
     *
     * @param teamId      the team ID
     * @param serviceName the service name to filter by
     * @return list of baseline responses for the service
     */
    @Transactional(readOnly = true)
    public List<AnomalyBaselineResponse> getBaselinesByService(UUID teamId, String serviceName) {
        return baselineMapper.toResponseList(
                baselineRepository.findByTeamIdAndServiceName(teamId, serviceName));
    }

    /**
     * Returns a single baseline by ID.
     *
     * @param baselineId the baseline ID
     * @return the baseline response
     * @throws NotFoundException if the baseline does not exist
     */
    @Transactional(readOnly = true)
    public AnomalyBaselineResponse getBaseline(UUID baselineId) {
        AnomalyBaseline baseline = baselineRepository.findById(baselineId)
                .orElseThrow(() -> new NotFoundException("Baseline not found: " + baselineId));
        return baselineMapper.toResponse(baseline);
    }

    /**
     * Updates baseline configuration (threshold, window, active status).
     * If windowHours is changed, triggers a full recalculation of the baseline.
     *
     * @param baselineId the baseline ID to update
     * @param request    the update request with optional fields
     * @return the updated baseline response
     * @throws NotFoundException if the baseline does not exist
     */
    public AnomalyBaselineResponse updateBaseline(UUID baselineId, UpdateBaselineRequest request) {
        AnomalyBaseline baseline = baselineRepository.findById(baselineId)
                .orElseThrow(() -> new NotFoundException("Baseline not found: " + baselineId));

        if (request.deviationThreshold() != null) {
            baseline.setDeviationThreshold(request.deviationThreshold());
        }
        if (request.isActive() != null) {
            baseline.setIsActive(request.isActive());
        }
        if (request.windowHours() != null) {
            Instant windowEnd = Instant.now();
            Instant windowStart = windowEnd.minus(request.windowHours(), ChronoUnit.HOURS);

            List<Double> hourlyData = collectHourlyData(
                    baseline.getTeamId(), baseline.getServiceName(),
                    baseline.getMetricName(), windowStart, windowEnd);

            Optional<AnomalyBaselineCalculator.BaselineStats> stats = calculator.computeBaseline(hourlyData);
            if (stats.isPresent()) {
                baseline.setBaselineValue(stats.get().mean());
                baseline.setStandardDeviation(stats.get().stddev());
                baseline.setSampleCount(stats.get().sampleCount());
                baseline.setWindowStartTime(windowStart);
                baseline.setWindowEndTime(windowEnd);
                baseline.setLastComputedAt(Instant.now());
            }
        }

        AnomalyBaseline saved = baselineRepository.save(baseline);
        log.info("Updated baseline {} ({}:{})", baselineId,
                baseline.getServiceName(), baseline.getMetricName());
        return baselineMapper.toResponse(saved);
    }

    /**
     * Deletes a baseline by ID.
     *
     * @param baselineId the baseline ID to delete
     * @throws NotFoundException if the baseline does not exist
     */
    public void deleteBaseline(UUID baselineId) {
        AnomalyBaseline baseline = baselineRepository.findById(baselineId)
                .orElseThrow(() -> new NotFoundException("Baseline not found: " + baselineId));
        baselineRepository.delete(baseline);
        log.info("Deleted baseline {} ({}:{})", baselineId,
                baseline.getServiceName(), baseline.getMetricName());
    }

    // ==================== Anomaly Checking ====================

    /**
     * Checks the current value of a metric against its baseline using z-score analysis.
     *
     * @param teamId      the team scope
     * @param serviceName the service to check
     * @param metricName  the metric to check (e.g., "log_volume", "error_rate")
     * @return the anomaly check result including z-score, direction, and anomaly flag
     * @throws NotFoundException if no baseline exists for the service/metric combination
     */
    @Transactional(readOnly = true)
    public AnomalyCheckResponse checkAnomaly(UUID teamId, String serviceName, String metricName) {
        AnomalyBaseline baseline = baselineRepository
                .findByTeamIdAndServiceNameAndMetricName(teamId, serviceName, metricName)
                .orElseThrow(() -> new NotFoundException(
                        "No baseline found for " + serviceName + ":" + metricName));

        double currentValue = getCurrentValue(teamId, serviceName, metricName);
        double zScore = calculator.calculateZScore(
                currentValue, baseline.getBaselineValue(), baseline.getStandardDeviation());
        boolean isAnomaly = calculator.isAnomaly(
                currentValue, baseline.getBaselineValue(),
                baseline.getStandardDeviation(), baseline.getDeviationThreshold());
        String direction = calculator.getDirection(currentValue, baseline.getBaselineValue());

        return new AnomalyCheckResponse(
                serviceName, metricName, currentValue,
                baseline.getBaselineValue(), baseline.getStandardDeviation(),
                baseline.getDeviationThreshold(), zScore, isAnomaly,
                direction, Instant.now()
        );
    }

    /**
     * Runs anomaly detection across all active baselines for a team.
     * Individual baseline check failures are logged but do not stop the report.
     *
     * @param teamId the team scope
     * @return comprehensive anomaly report with all checks and detected anomalies
     */
    @Transactional(readOnly = true)
    public AnomalyReportResponse runFullCheck(UUID teamId) {
        List<AnomalyBaseline> activeBaselines = baselineRepository.findByTeamIdAndIsActiveTrue(teamId);
        List<AnomalyCheckResponse> allChecks = new ArrayList<>();
        List<AnomalyCheckResponse> anomalies = new ArrayList<>();

        for (AnomalyBaseline baseline : activeBaselines) {
            try {
                AnomalyCheckResponse check = checkAnomaly(
                        teamId, baseline.getServiceName(), baseline.getMetricName());
                allChecks.add(check);
                if (check.isAnomaly()) {
                    anomalies.add(check);
                }
            } catch (Exception e) {
                log.warn("Failed to check anomaly for {}:{}: {}",
                        baseline.getServiceName(), baseline.getMetricName(), e.getMessage());
            }
        }

        return new AnomalyReportResponse(
                teamId, Instant.now(),
                activeBaselines.size(), anomalies.size(),
                anomalies, allChecks
        );
    }

    // ==================== Scheduled Baseline Recalculation ====================

    /**
     * Scheduled task to recalculate all active baselines from current historical data.
     * Runs daily at 3:00 AM by default (configurable via property).
     * Individual baseline failures are logged but do not stop other recalculations.
     */
    @Scheduled(cron = "${codeops.anomaly.recalculation-cron:0 0 3 * * *}")
    @Transactional
    public void recalculateAllBaselines() {
        log.info("Starting scheduled baseline recalculation");
        List<AnomalyBaseline> activeBaselines = baselineRepository.findByIsActiveTrue();
        int success = 0;
        int failed = 0;

        for (AnomalyBaseline baseline : activeBaselines) {
            try {
                recalculateBaseline(baseline);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to recalculate baseline '{}:{}' for team {}: {}",
                        baseline.getServiceName(), baseline.getMetricName(),
                        baseline.getTeamId(), e.getMessage());
            }
        }

        log.info("Baseline recalculation complete: {} succeeded, {} failed", success, failed);
    }

    /**
     * Recalculates a single baseline from current historical data using the
     * same window duration as the previous computation.
     *
     * @param baseline the baseline entity to recalculate
     */
    void recalculateBaseline(AnomalyBaseline baseline) {
        long windowHours = Duration.between(
                baseline.getWindowStartTime(), baseline.getWindowEndTime()).toHours();
        if (windowHours <= 0) {
            windowHours = AppConstants.DEFAULT_BASELINE_WINDOW_HOURS;
        }

        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(windowHours, ChronoUnit.HOURS);

        List<Double> hourlyData = collectHourlyData(
                baseline.getTeamId(), baseline.getServiceName(),
                baseline.getMetricName(), windowStart, windowEnd);

        Optional<AnomalyBaselineCalculator.BaselineStats> stats = calculator.computeBaseline(hourlyData);
        if (stats.isEmpty()) {
            log.warn("Insufficient data to recalculate baseline {}:{} for team {}",
                    baseline.getServiceName(), baseline.getMetricName(), baseline.getTeamId());
            return;
        }

        baseline.setBaselineValue(stats.get().mean());
        baseline.setStandardDeviation(stats.get().stddev());
        baseline.setSampleCount(stats.get().sampleCount());
        baseline.setWindowStartTime(windowStart);
        baseline.setWindowEndTime(windowEnd);
        baseline.setLastComputedAt(Instant.now());

        baselineRepository.save(baseline);
        log.debug("Recalculated baseline {}:{} for team {}",
                baseline.getServiceName(), baseline.getMetricName(), baseline.getTeamId());
    }

    // ==================== Data Collection Helpers ====================

    /**
     * Routes hourly data collection to the appropriate method based on metric name.
     *
     * @param teamId      the team scope
     * @param serviceName the service name
     * @param metricName  the metric name
     * @param start       window start time
     * @param end         window end time
     * @return list of hourly data values
     */
    private List<Double> collectHourlyData(UUID teamId, String serviceName,
            String metricName, Instant start, Instant end) {
        return switch (metricName) {
            case "log_volume" -> collectHourlyLogVolume(teamId, serviceName, start, end);
            case "error_rate" -> collectHourlyErrorRate(teamId, serviceName, start, end);
            default -> collectHourlyCustomMetric(teamId, serviceName, metricName, start, end);
        };
    }

    /**
     * Collects hourly log volume data for a service.
     * Returns one value per hour: the count of log entries in that hour.
     *
     * @param teamId      the team scope
     * @param serviceName the service
     * @param windowStart start of the collection window
     * @param windowEnd   end of the collection window
     * @return list of hourly log counts as doubles
     */
    List<Double> collectHourlyLogVolume(UUID teamId, String serviceName,
            Instant windowStart, Instant windowEnd) {
        List<Double> values = new ArrayList<>();
        Instant hourStart = windowStart;
        while (hourStart.isBefore(windowEnd)) {
            Instant hourEnd = hourStart.plus(1, ChronoUnit.HOURS);
            long count = logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                    teamId, serviceName, hourStart, hourEnd);
            values.add((double) count);
            hourStart = hourEnd;
        }
        return values;
    }

    /**
     * Collects hourly error rate data for a service.
     * Returns one value per hour: (ERROR + FATAL count / total count) * 100.
     * Zero total logs in an hour yields 0% error rate.
     *
     * @param teamId      the team scope
     * @param serviceName the service
     * @param windowStart start of the collection window
     * @param windowEnd   end of the collection window
     * @return list of hourly error rate percentages (0-100)
     */
    List<Double> collectHourlyErrorRate(UUID teamId, String serviceName,
            Instant windowStart, Instant windowEnd) {
        List<Double> values = new ArrayList<>();
        Instant hourStart = windowStart;
        while (hourStart.isBefore(windowEnd)) {
            Instant hourEnd = hourStart.plus(1, ChronoUnit.HOURS);
            long total = logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                    teamId, serviceName, hourStart, hourEnd);
            if (total == 0) {
                values.add(0.0);
            } else {
                long errors = logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                        teamId, serviceName, LogLevel.ERROR, hourStart, hourEnd);
                long fatals = logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                        teamId, serviceName, LogLevel.FATAL, hourStart, hourEnd);
                values.add((errors + fatals) * 100.0 / total);
            }
            hourStart = hourEnd;
        }
        return values;
    }

    /**
     * Collects hourly data for a custom metric by querying the MetricSeries table.
     * Uses the average value per hour for the matching metric definition.
     *
     * @param teamId      the team scope
     * @param serviceName the service name
     * @param metricName  the custom metric name
     * @param windowStart start of the collection window
     * @param windowEnd   end of the collection window
     * @return list of hourly average values, or empty list if metric not found
     */
    private List<Double> collectHourlyCustomMetric(UUID teamId, String serviceName,
            String metricName, Instant windowStart, Instant windowEnd) {
        Optional<Metric> metricOpt = metricRepository.findByTeamIdAndNameAndServiceName(
                teamId, metricName, serviceName);
        if (metricOpt.isEmpty()) {
            return List.of();
        }

        UUID metricId = metricOpt.get().getId();
        List<Double> values = new ArrayList<>();
        Instant hourStart = windowStart;
        while (hourStart.isBefore(windowEnd)) {
            Instant hourEnd = hourStart.plus(1, ChronoUnit.HOURS);
            Optional<Double> avg = metricSeriesRepository
                    .findAverageValueByMetricIdAndTimestampBetween(metricId, hourStart, hourEnd);
            values.add(avg.orElse(0.0));
            hourStart = hourEnd;
        }
        return values;
    }

    /**
     * Gets the current value for a metric in the last hour.
     *
     * @param teamId      the team scope
     * @param serviceName the service
     * @param metricName  the metric name
     * @return the current hour's metric value
     */
    double getCurrentValue(UUID teamId, String serviceName, String metricName) {
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant now = Instant.now();
        return switch (metricName) {
            case "log_volume" -> (double) logEntryRepository
                    .countByTeamIdAndServiceNameAndTimestampBetween(teamId, serviceName, hourAgo, now);
            case "error_rate" -> calculateCurrentErrorRate(teamId, serviceName, hourAgo, now);
            default -> getCustomMetricCurrentValue(teamId, serviceName, metricName, hourAgo, now);
        };
    }

    /**
     * Calculates the current error rate for a service in the given time window.
     *
     * @param teamId      the team scope
     * @param serviceName the service
     * @param since       start of the window
     * @param until       end of the window
     * @return error rate as a percentage (0-100), or 0 if no logs exist
     */
    private double calculateCurrentErrorRate(UUID teamId, String serviceName,
            Instant since, Instant until) {
        long total = logEntryRepository.countByTeamIdAndServiceNameAndTimestampBetween(
                teamId, serviceName, since, until);
        if (total == 0) return 0.0;
        long errors = logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                teamId, serviceName, LogLevel.ERROR, since, until);
        long fatals = logEntryRepository.countByTeamIdAndServiceNameAndLevelAndTimestampBetween(
                teamId, serviceName, LogLevel.FATAL, since, until);
        return (errors + fatals) * 100.0 / total;
    }

    /**
     * Gets the current value for a custom metric by averaging recent data points.
     *
     * @param teamId      the team scope
     * @param serviceName the service
     * @param metricName  the metric name
     * @param since       start of the window
     * @param until       end of the window
     * @return the average metric value, or 0.0 if no data found
     */
    private double getCustomMetricCurrentValue(UUID teamId, String serviceName,
            String metricName, Instant since, Instant until) {
        Optional<Metric> metricOpt = metricRepository.findByTeamIdAndNameAndServiceName(
                teamId, metricName, serviceName);
        if (metricOpt.isEmpty()) return 0.0;
        return metricSeriesRepository
                .findAverageValueByMetricIdAndTimestampBetween(metricOpt.get().getId(), since, until)
                .orElse(0.0);
    }
}
