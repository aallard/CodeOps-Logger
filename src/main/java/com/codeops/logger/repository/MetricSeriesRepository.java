package com.codeops.logger.repository;

import com.codeops.logger.entity.MetricSeries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link MetricSeries} entities.
 */
@Repository
public interface MetricSeriesRepository extends JpaRepository<MetricSeries, UUID> {

    List<MetricSeries> findByMetricIdAndTimestampBetweenOrderByTimestampAsc(UUID metricId, Instant start, Instant end);

    Page<MetricSeries> findByMetricId(UUID metricId, Pageable pageable);

    void deleteByTimestampBefore(Instant cutoff);

    void deleteByMetricId(UUID metricId);

    long countByMetricId(UUID metricId);

    @Query("SELECT AVG(ms.value) FROM MetricSeries ms WHERE ms.metric.id = :metricId AND ms.timestamp BETWEEN :start AND :end")
    Optional<Double> findAverageValueByMetricIdAndTimestampBetween(@Param("metricId") UUID metricId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT MAX(ms.value) FROM MetricSeries ms WHERE ms.metric.id = :metricId AND ms.timestamp BETWEEN :start AND :end")
    Optional<Double> findMaxValueByMetricIdAndTimestampBetween(@Param("metricId") UUID metricId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT MIN(ms.value) FROM MetricSeries ms WHERE ms.metric.id = :metricId AND ms.timestamp BETWEEN :start AND :end")
    Optional<Double> findMinValueByMetricIdAndTimestampBetween(@Param("metricId") UUID metricId, @Param("start") Instant start, @Param("end") Instant end);
}
