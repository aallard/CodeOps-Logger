package com.codeops.logger.service;

import com.codeops.logger.dto.response.MetricTimeSeriesResponse;
import com.codeops.logger.entity.MetricSeries;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link MetricAggregationService}.
 * Pure computation — no mocks needed.
 */
class MetricAggregationServiceTest {

    private final MetricAggregationService service = new MetricAggregationService();

    // ==================== aggregate() Tests ====================

    @Test
    void testAggregate_multipleValues_calculatesAll() {
        List<Double> values = List.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0);

        MetricAggregationService.AggregationResult result = service.aggregate(values);

        assertThat(result.count()).isEqualTo(10);
        assertThat(result.sum()).isCloseTo(550.0, within(0.001));
        assertThat(result.avg()).isCloseTo(55.0, within(0.001));
        assertThat(result.min()).isCloseTo(10.0, within(0.001));
        assertThat(result.max()).isCloseTo(100.0, within(0.001));
        assertThat(result.p50()).isCloseTo(50.0, within(0.001));
        assertThat(result.p95()).isCloseTo(100.0, within(0.001));
        assertThat(result.p99()).isCloseTo(100.0, within(0.001));
        assertThat(result.stddev()).isGreaterThan(0.0);
    }

    @Test
    void testAggregate_singleValue_allEqual() {
        List<Double> values = List.of(42.0);

        MetricAggregationService.AggregationResult result = service.aggregate(values);

        assertThat(result.count()).isEqualTo(1);
        assertThat(result.sum()).isCloseTo(42.0, within(0.001));
        assertThat(result.avg()).isCloseTo(42.0, within(0.001));
        assertThat(result.min()).isCloseTo(42.0, within(0.001));
        assertThat(result.max()).isCloseTo(42.0, within(0.001));
        assertThat(result.p50()).isCloseTo(42.0, within(0.001));
        assertThat(result.p95()).isCloseTo(42.0, within(0.001));
        assertThat(result.p99()).isCloseTo(42.0, within(0.001));
        assertThat(result.stddev()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testAggregate_emptyList_returnsZeros() {
        MetricAggregationService.AggregationResult result = service.aggregate(List.of());

        assertThat(result.count()).isZero();
        assertThat(result.sum()).isCloseTo(0.0, within(0.001));
        assertThat(result.avg()).isCloseTo(0.0, within(0.001));
        assertThat(result.min()).isCloseTo(0.0, within(0.001));
        assertThat(result.max()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testAggregate_nullList_returnsZeros() {
        MetricAggregationService.AggregationResult result = service.aggregate(null);

        assertThat(result.count()).isZero();
        assertThat(result.sum()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testAggregate_twoValues_correctAvg() {
        List<Double> values = List.of(10.0, 30.0);

        MetricAggregationService.AggregationResult result = service.aggregate(values);

        assertThat(result.count()).isEqualTo(2);
        assertThat(result.sum()).isCloseTo(40.0, within(0.001));
        assertThat(result.avg()).isCloseTo(20.0, within(0.001));
        assertThat(result.min()).isCloseTo(10.0, within(0.001));
        assertThat(result.max()).isCloseTo(30.0, within(0.001));
    }

    // ==================== calculatePercentile() Tests ====================

    @Test
    void testCalculatePercentile_p50_median() {
        List<Double> sorted = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);

        double p50 = service.calculatePercentile(sorted, 50);

        assertThat(p50).isCloseTo(5.0, within(0.001));
    }

    @Test
    void testCalculatePercentile_p95() {
        List<Double> sorted = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            sorted.add((double) i);
        }

        double p95 = service.calculatePercentile(sorted, 95);

        assertThat(p95).isCloseTo(95.0, within(0.001));
    }

    @Test
    void testCalculatePercentile_p99() {
        List<Double> sorted = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            sorted.add((double) i);
        }

        double p99 = service.calculatePercentile(sorted, 99);

        assertThat(p99).isCloseTo(99.0, within(0.001));
    }

    @Test
    void testCalculatePercentile_p0_returnsFirst() {
        List<Double> sorted = List.of(5.0, 10.0, 15.0);

        double p0 = service.calculatePercentile(sorted, 0);

        assertThat(p0).isCloseTo(5.0, within(0.001));
    }

    @Test
    void testCalculatePercentile_p100_returnsLast() {
        List<Double> sorted = List.of(5.0, 10.0, 15.0);

        double p100 = service.calculatePercentile(sorted, 100);

        assertThat(p100).isCloseTo(15.0, within(0.001));
    }

    @Test
    void testCalculatePercentile_singleValue() {
        List<Double> sorted = List.of(99.0);

        assertThat(service.calculatePercentile(sorted, 50)).isCloseTo(99.0, within(0.001));
        assertThat(service.calculatePercentile(sorted, 95)).isCloseTo(99.0, within(0.001));
    }

    @Test
    void testCalculatePercentile_emptyList_returnsZero() {
        assertThat(service.calculatePercentile(Collections.emptyList(), 50)).isCloseTo(0.0, within(0.001));
    }

    // ==================== calculateStdDev() Tests ====================

    @Test
    void testCalculateStdDev_uniform_zero() {
        List<Double> values = List.of(5.0, 5.0, 5.0, 5.0);

        double stddev = service.calculateStdDev(values, 5.0);

        assertThat(stddev).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testCalculateStdDev_varied() {
        List<Double> values = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);
        double mean = 5.0;

        double stddev = service.calculateStdDev(values, mean);

        // Variance = (9+1+1+1+0+0+4+16)/8 = 32/8 = 4, stddev = 2.0
        assertThat(stddev).isCloseTo(2.0, within(0.001));
    }

    // ==================== aggregateByResolution() Tests ====================

    @Test
    void testAggregateByResolution_60s_bucketsCorrectly() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        List<MetricSeries> dataPoints = List.of(
                createSeries(base.plusSeconds(5), 10.0),
                createSeries(base.plusSeconds(30), 20.0),
                createSeries(base.plusSeconds(65), 30.0),
                createSeries(base.plusSeconds(90), 40.0)
        );

        List<MetricTimeSeriesResponse.DataPoint> result =
                service.aggregateByResolution(dataPoints, base, base.plusSeconds(120), 60);

        assertThat(result).hasSize(2);
        // Bucket 1 (10:00:00): avg(10, 20) = 15
        assertThat(result.get(0).timestamp()).isEqualTo(base);
        assertThat(result.get(0).value()).isCloseTo(15.0, within(0.001));
        // Bucket 2 (10:01:00): avg(30, 40) = 35
        assertThat(result.get(1).timestamp()).isEqualTo(base.plusSeconds(60));
        assertThat(result.get(1).value()).isCloseTo(35.0, within(0.001));
    }

    @Test
    void testAggregateByResolution_emptyBuckets_omitted() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        List<MetricSeries> dataPoints = List.of(
                createSeries(base.plusSeconds(5), 10.0),
                createSeries(base.plusSeconds(125), 50.0)
        );

        List<MetricTimeSeriesResponse.DataPoint> result =
                service.aggregateByResolution(dataPoints, base, base.plusSeconds(180), 60);

        // Bucket 0 (10:00:00) has data, bucket 1 (10:01:00) empty → omitted, bucket 2 (10:02:00) has data
        assertThat(result).hasSize(2);
        assertThat(result.get(0).timestamp()).isEqualTo(base);
        assertThat(result.get(1).timestamp()).isEqualTo(base.plusSeconds(120));
    }

    @Test
    void testAggregateByResolution_singleBucket() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        List<MetricSeries> dataPoints = List.of(
                createSeries(base.plusSeconds(10), 100.0),
                createSeries(base.plusSeconds(20), 200.0),
                createSeries(base.plusSeconds(30), 300.0)
        );

        List<MetricTimeSeriesResponse.DataPoint> result =
                service.aggregateByResolution(dataPoints, base, base.plusSeconds(60), 60);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isCloseTo(200.0, within(0.001));
    }

    @Test
    void testAggregateByResolution_emptyDataPoints_returnsEmpty() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        List<MetricTimeSeriesResponse.DataPoint> result =
                service.aggregateByResolution(List.of(), base, base.plusSeconds(60), 60);

        assertThat(result).isEmpty();
    }

    private MetricSeries createSeries(Instant timestamp, Double value) {
        MetricSeries series = new MetricSeries();
        series.setTimestamp(timestamp);
        series.setValue(value);
        return series;
    }
}
