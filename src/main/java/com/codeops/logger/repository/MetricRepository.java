package com.codeops.logger.repository;

import com.codeops.logger.entity.Metric;
import com.codeops.logger.entity.enums.MetricType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Metric} entities.
 */
@Repository
public interface MetricRepository extends JpaRepository<Metric, UUID> {

    List<Metric> findByTeamId(UUID teamId);

    Page<Metric> findByTeamId(UUID teamId, Pageable pageable);

    List<Metric> findByTeamIdAndServiceName(UUID teamId, String serviceName);

    Optional<Metric> findByTeamIdAndNameAndServiceName(UUID teamId, String name, String serviceName);

    List<Metric> findByTeamIdAndMetricType(UUID teamId, MetricType metricType);

    long countByTeamId(UUID teamId);
}
