package com.codeops.logger.service;

import com.codeops.logger.dto.response.MetricTimeSeriesResponse;
import com.codeops.logger.entity.MetricSeries;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Performs statistical aggregation calculations on metric time-series data.
 * Supports sum, average, min, max, standard deviation, and percentile calculations (p50, p95, p99).
 */
@Service
@Slf4j
public class MetricAggregationService {

    /**
     * Calculates comprehensive aggregations over a list of data point values.
     *
     * @param values the raw metric values
     * @return aggregation results including sum, avg, min, max, percentiles, stddev
     */
    public AggregationResult aggregate(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new AggregationResult(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        long count = values.size();
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / count;
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        double p50 = calculatePercentile(sorted, 50);
        double p95 = calculatePercentile(sorted, 95);
        double p99 = calculatePercentile(sorted, 99);
        double stddev = calculateStdDev(values, avg);

        return new AggregationResult(count, sum, avg, min, max, p50, p95, p99, stddev);
    }

    /**
     * Calculates a percentile value from a sorted list using the nearest-rank method.
     *
     * @param sortedValues the values sorted in ascending order
     * @param percentile   the desired percentile (0-100)
     * @return the percentile value
     */
    double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        if (percentile <= 0) {
            return sortedValues.getFirst();
        }
        if (percentile >= 100) {
            return sortedValues.getLast();
        }

        int rank = (int) Math.ceil(percentile / 100.0 * sortedValues.size());
        return sortedValues.get(rank - 1);
    }

    /**
     * Calculates standard deviation of values.
     *
     * @param values the values
     * @param mean   the pre-calculated mean
     * @return the standard deviation
     */
    double calculateStdDev(List<Double> values, double mean) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sumSquaredDiffs = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiffs / values.size());
    }

    /**
     * Aggregates data points into time-bucketed windows.
     * Each bucket covers {@code resolutionSeconds} and contains the average of points within it.
     * Empty buckets are omitted.
     *
     * @param dataPoints        raw data points (timestamp + value)
     * @param startTime         window start
     * @param endTime           window end
     * @param resolutionSeconds bucket size in seconds
     * @return list of aggregated data points, one per non-empty bucket
     */
    public List<MetricTimeSeriesResponse.DataPoint> aggregateByResolution(
            List<MetricSeries> dataPoints, Instant startTime, Instant endTime,
            int resolutionSeconds) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return List.of();
        }

        long startEpoch = startTime.getEpochSecond();
        long endEpoch = endTime.getEpochSecond();

        Map<Long, List<Double>> buckets = new TreeMap<>();

        for (MetricSeries dp : dataPoints) {
            long ts = dp.getTimestamp().getEpochSecond();
            if (ts < startEpoch || ts > endEpoch) {
                continue;
            }
            long bucketKey = startEpoch + ((ts - startEpoch) / resolutionSeconds) * resolutionSeconds;
            buckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(dp.getValue());
        }

        List<MetricTimeSeriesResponse.DataPoint> result = new ArrayList<>();
        for (Map.Entry<Long, List<Double>> entry : buckets.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            result.add(new MetricTimeSeriesResponse.DataPoint(
                    Instant.ofEpochSecond(entry.getKey()), avg, null));
        }

        return result;
    }

    /**
     * Inner record for aggregation results.
     */
    public record AggregationResult(
            long count,
            double sum,
            double avg,
            double min,
            double max,
            double p50,
            double p95,
            double p99,
            double stddev
    ) {}
}
