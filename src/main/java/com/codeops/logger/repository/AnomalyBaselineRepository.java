package com.codeops.logger.repository;

import com.codeops.logger.entity.AnomalyBaseline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AnomalyBaseline} entities.
 */
@Repository
public interface AnomalyBaselineRepository extends JpaRepository<AnomalyBaseline, UUID> {

    List<AnomalyBaseline> findByTeamId(UUID teamId);

    Page<AnomalyBaseline> findByTeamId(UUID teamId, Pageable pageable);

    List<AnomalyBaseline> findByTeamIdAndServiceName(UUID teamId, String serviceName);

    Optional<AnomalyBaseline> findByTeamIdAndServiceNameAndMetricName(UUID teamId, String serviceName, String metricName);

    List<AnomalyBaseline> findByIsActiveTrue();

    List<AnomalyBaseline> findByTeamIdAndIsActiveTrue(UUID teamId);

    long countByTeamId(UUID teamId);
}
