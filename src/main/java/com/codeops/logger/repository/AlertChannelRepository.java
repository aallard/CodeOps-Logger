package com.codeops.logger.repository;

import com.codeops.logger.entity.AlertChannel;
import com.codeops.logger.entity.enums.AlertChannelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AlertChannel} entities.
 */
@Repository
public interface AlertChannelRepository extends JpaRepository<AlertChannel, UUID> {

    List<AlertChannel> findByTeamId(UUID teamId);

    Page<AlertChannel> findByTeamId(UUID teamId, Pageable pageable);

    List<AlertChannel> findByTeamIdAndIsActiveTrue(UUID teamId);

    List<AlertChannel> findByTeamIdAndChannelType(UUID teamId, AlertChannelType channelType);

    long countByTeamId(UUID teamId);
}
