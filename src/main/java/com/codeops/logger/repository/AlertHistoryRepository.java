package com.codeops.logger.repository;

import com.codeops.logger.entity.AlertHistory;
import com.codeops.logger.entity.enums.AlertSeverity;
import com.codeops.logger.entity.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AlertHistory} entities.
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, UUID> {

    Page<AlertHistory> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    Page<AlertHistory> findByTeamIdAndStatus(UUID teamId, AlertStatus status, Pageable pageable);

    Page<AlertHistory> findByTeamIdAndSeverity(UUID teamId, AlertSeverity severity, Pageable pageable);

    Page<AlertHistory> findByRuleId(UUID ruleId, Pageable pageable);

    List<AlertHistory> findByTeamIdAndStatusAndCreatedAtAfter(UUID teamId, AlertStatus status, Instant since);

    long countByTeamIdAndStatus(UUID teamId, AlertStatus status);

    long countByTeamIdAndSeverityAndStatus(UUID teamId, AlertSeverity severity, AlertStatus status);
}
