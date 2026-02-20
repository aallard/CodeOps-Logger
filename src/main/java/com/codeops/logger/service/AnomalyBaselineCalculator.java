package com.codeops.logger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Stateless statistical calculator for anomaly detection baselines.
 * Computes mean, standard deviation, and z-scores for time-series data.
 */
@Component
@Slf4j
public class AnomalyBaselineCalculator {

    /** Minimum number of hourly data points required to compute a meaningful baseline. */
    static final int MINIMUM_DATA_POINTS = 24;

    /** Small epsilon used instead of zero stddev to avoid division-by-zero in z-score. */
    static final double STDDEV_EPSILON = 0.001;

    /**
     * Computes a baseline (mean and standard deviation) from a list of hourly data points.
     *
     * @param hourlyValues list of values, one per hour in the baseline window
     * @return computed baseline stats, or empty if insufficient data (fewer than 24 points)
     */
    public Optional<BaselineStats> computeBaseline(List<Double> hourlyValues) {
        if (hourlyValues == null || hourlyValues.size() < MINIMUM_DATA_POINTS) {
            log.debug("Insufficient data points for baseline: {} (minimum {})",
                    hourlyValues == null ? 0 : hourlyValues.size(), MINIMUM_DATA_POINTS);
            return Optional.empty();
        }

        double sum = 0;
        for (double v : hourlyValues) {
            sum += v;
        }
        double mean = sum / hourlyValues.size();

        double varianceSum = 0;
        for (double v : hourlyValues) {
            varianceSum += (v - mean) * (v - mean);
        }
        double stddev = Math.sqrt(varianceSum / hourlyValues.size());

        if (stddev == 0) {
            stddev = STDDEV_EPSILON;
        }

        return Optional.of(new BaselineStats(mean, stddev, hourlyValues.size()));
    }

    /**
     * Calculates the z-score of a value against a baseline.
     * Z-score measures how many standard deviations a value is from the mean.
     *
     * @param value  the current value
     * @param mean   the baseline mean
     * @param stddev the baseline standard deviation
     * @return the absolute z-score, or 0 if stddev is non-positive
     */
    public double calculateZScore(double value, double mean, double stddev) {
        if (stddev <= 0) {
            return 0;
        }
        return Math.abs(value - mean) / stddev;
    }

    /**
     * Determines if a value is anomalous given a baseline and threshold.
     *
     * @param value     the current value
     * @param mean      the baseline mean
     * @param stddev    the baseline standard deviation
     * @param threshold the z-score threshold for anomaly detection
     * @return true if the z-score exceeds the threshold
     */
    public boolean isAnomaly(double value, double mean, double stddev, double threshold) {
        return calculateZScore(value, mean, stddev) > threshold;
    }

    /**
     * Determines the direction of deviation from the baseline.
     *
     * @param value the current value
     * @param mean  the baseline mean
     * @return "ABOVE" if value exceeds mean, "BELOW" if below mean, "NORMAL" if equal
     */
    public String getDirection(double value, double mean) {
        if (value > mean) return "ABOVE";
        if (value < mean) return "BELOW";
        return "NORMAL";
    }

    /**
     * Inner record for baseline computation results.
     *
     * @param mean        the computed mean value
     * @param stddev      the computed standard deviation
     * @param sampleCount the number of data points used
     */
    public record BaselineStats(double mean, double stddev, long sampleCount) {}
}
