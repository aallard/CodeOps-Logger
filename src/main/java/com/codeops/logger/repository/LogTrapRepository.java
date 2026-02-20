package com.codeops.logger.repository;

import com.codeops.logger.entity.LogTrap;
import com.codeops.logger.entity.enums.TrapType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LogTrap} entities.
 */
@Repository
public interface LogTrapRepository extends JpaRepository<LogTrap, UUID> {

    List<LogTrap> findByTeamId(UUID teamId);

    Page<LogTrap> findByTeamId(UUID teamId, Pageable pageable);

    List<LogTrap> findByTeamIdAndIsActiveTrue(UUID teamId);

    List<LogTrap> findByTrapTypeAndIsActiveTrue(TrapType trapType);

    long countByTeamId(UUID teamId);
}
