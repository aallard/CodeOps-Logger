package com.codeops.logger.repository;

import com.codeops.logger.entity.QueryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link QueryHistory} entities.
 */
@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, UUID> {

    Page<QueryHistory> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

    Page<QueryHistory> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    void deleteByCreatedAtBefore(Instant cutoff);
}
