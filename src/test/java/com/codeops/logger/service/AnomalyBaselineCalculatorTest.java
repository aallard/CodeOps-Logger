package com.codeops.logger.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link AnomalyBaselineCalculator}.
 * Pure computation tests — no mocks needed.
 */
class AnomalyBaselineCalculatorTest {

    private final AnomalyBaselineCalculator calculator = new AnomalyBaselineCalculator();

    @Test
    void testComputeBaseline_sufficientData_calculatesCorrectly() {
        // 48 data points: alternating 10.0 and 20.0
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            values.add(i % 2 == 0 ? 10.0 : 20.0);
        }

        Optional<AnomalyBaselineCalculator.BaselineStats> result = calculator.computeBaseline(values);

        assertThat(result).isPresent();
        assertThat(result.get().mean()).isCloseTo(15.0, within(0.01));
        assertThat(result.get().stddev()).isCloseTo(5.0, within(0.01));
        assertThat(result.get().sampleCount()).isEqualTo(48);
    }

    @Test
    void testComputeBaseline_insufficientData_returnsEmpty() {
        // Only 23 points — below the 24 minimum
        List<Double> values = new ArrayList<>(Collections.nCopies(23, 10.0));

        Optional<AnomalyBaselineCalculator.BaselineStats> result = calculator.computeBaseline(values);

        assertThat(result).isEmpty();
    }

    @Test
    void testComputeBaseline_nullInput_returnsEmpty() {
        Optional<AnomalyBaselineCalculator.BaselineStats> result = calculator.computeBaseline(null);

        assertThat(result).isEmpty();
    }

    @Test
    void testComputeBaseline_uniformValues_stddevEpsilon() {
        // All values identical — stddev should be epsilon (0.001)
        List<Double> values = new ArrayList<>(Collections.nCopies(48, 100.0));

        Optional<AnomalyBaselineCalculator.BaselineStats> result = calculator.computeBaseline(values);

        assertThat(result).isPresent();
        assertThat(result.get().mean()).isCloseTo(100.0, within(0.01));
        assertThat(result.get().stddev()).isCloseTo(AnomalyBaselineCalculator.STDDEV_EPSILON, within(0.0001));
    }

    @Test
    void testComputeBaseline_variableValues_correctStddev() {
        // Known values: [2, 4, 4, 4, 5, 5, 7, 9] repeated 3x = 24 points
        // Mean = 5.0, Variance = 4.0, StdDev = 2.0
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            values.addAll(List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0));
        }

        Optional<AnomalyBaselineCalculator.BaselineStats> result = calculator.computeBaseline(values);

        assertThat(result).isPresent();
        assertThat(result.get().mean()).isCloseTo(5.0, within(0.01));
        assertThat(result.get().stddev()).isCloseTo(2.0, within(0.01));
        assertThat(result.get().sampleCount()).isEqualTo(24);
    }

    @Test
    void testCalculateZScore_normalValue_lowScore() {
        // Value=12, Mean=10, StdDev=5 → z = |12-10|/5 = 0.4
        double zScore = calculator.calculateZScore(12.0, 10.0, 5.0);

        assertThat(zScore).isCloseTo(0.4, within(0.001));
    }

    @Test
    void testCalculateZScore_anomalousValue_highScore() {
        // Value=30, Mean=10, StdDev=5 → z = |30-10|/5 = 4.0
        double zScore = calculator.calculateZScore(30.0, 10.0, 5.0);

        assertThat(zScore).isCloseTo(4.0, within(0.001));
    }

    @Test
    void testCalculateZScore_zeroStddev_returnsZero() {
        double zScore = calculator.calculateZScore(100.0, 50.0, 0.0);

        assertThat(zScore).isEqualTo(0.0);
    }

    @Test
    void testIsAnomaly_aboveThreshold_true() {
        // Value=30, Mean=10, StdDev=5, Threshold=2.0 → z=4.0 > 2.0
        boolean result = calculator.isAnomaly(30.0, 10.0, 5.0, 2.0);

        assertThat(result).isTrue();
    }

    @Test
    void testIsAnomaly_belowThreshold_false() {
        // Value=12, Mean=10, StdDev=5, Threshold=2.0 → z=0.4 < 2.0
        boolean result = calculator.isAnomaly(12.0, 10.0, 5.0, 2.0);

        assertThat(result).isFalse();
    }

    @Test
    void testGetDirection_aboveBelowNormal() {
        assertThat(calculator.getDirection(15.0, 10.0)).isEqualTo("ABOVE");
        assertThat(calculator.getDirection(5.0, 10.0)).isEqualTo("BELOW");
        assertThat(calculator.getDirection(10.0, 10.0)).isEqualTo("NORMAL");
    }
}
