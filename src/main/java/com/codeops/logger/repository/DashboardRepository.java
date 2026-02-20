package com.codeops.logger.repository;

import com.codeops.logger.entity.Dashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Dashboard} entities.
 */
@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, UUID> {

    List<Dashboard> findByTeamId(UUID teamId);

    Page<Dashboard> findByTeamId(UUID teamId, Pageable pageable);

    List<Dashboard> findByTeamIdAndIsSharedTrue(UUID teamId);

    List<Dashboard> findByTeamIdAndIsTemplateTrue(UUID teamId);

    List<Dashboard> findByCreatedBy(UUID createdBy);

    long countByTeamId(UUID teamId);
}
