package com.codeops.logger.repository;

import com.codeops.logger.entity.LogSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LogSource} entities.
 */
@Repository
public interface LogSourceRepository extends JpaRepository<LogSource, UUID> {

    List<LogSource> findByTeamId(UUID teamId);

    Page<LogSource> findByTeamId(UUID teamId, Pageable pageable);

    List<LogSource> findByTeamIdAndIsActiveTrue(UUID teamId);

    Optional<LogSource> findByTeamIdAndName(UUID teamId, String name);

    Optional<LogSource> findByServiceId(UUID serviceId);

    boolean existsByTeamIdAndName(UUID teamId, String name);

    long countByTeamId(UUID teamId);
}
