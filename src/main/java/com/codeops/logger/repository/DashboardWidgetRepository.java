package com.codeops.logger.repository;

import com.codeops.logger.entity.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DashboardWidget} entities.
 */
@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, UUID> {

    List<DashboardWidget> findByDashboardIdOrderBySortOrderAsc(UUID dashboardId);

    void deleteByDashboardId(UUID dashboardId);

    long countByDashboardId(UUID dashboardId);
}
