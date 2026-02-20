package com.codeops.logger.repository;

import com.codeops.logger.entity.RetentionPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RetentionPolicy} entities.
 */
@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, UUID> {

    List<RetentionPolicy> findByTeamId(UUID teamId);

    Page<RetentionPolicy> findByTeamId(UUID teamId, Pageable pageable);

    List<RetentionPolicy> findByIsActiveTrue();

    List<RetentionPolicy> findByTeamIdAndIsActiveTrue(UUID teamId);

    long countByTeamId(UUID teamId);
}
