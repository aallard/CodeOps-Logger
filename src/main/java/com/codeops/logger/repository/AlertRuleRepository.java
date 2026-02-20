package com.codeops.logger.repository;

import com.codeops.logger.entity.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AlertRule} entities.
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    List<AlertRule> findByTeamId(UUID teamId);

    Page<AlertRule> findByTeamId(UUID teamId, Pageable pageable);

    List<AlertRule> findByTrapId(UUID trapId);

    List<AlertRule> findByTrapIdAndIsActiveTrue(UUID trapId);

    List<AlertRule> findByChannelId(UUID channelId);

    long countByTeamId(UUID teamId);
}
