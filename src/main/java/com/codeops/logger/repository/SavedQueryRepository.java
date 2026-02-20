package com.codeops.logger.repository;

import com.codeops.logger.entity.SavedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SavedQuery} entities.
 */
@Repository
public interface SavedQueryRepository extends JpaRepository<SavedQuery, UUID> {

    List<SavedQuery> findByTeamId(UUID teamId);

    Page<SavedQuery> findByTeamId(UUID teamId, Pageable pageable);

    List<SavedQuery> findByCreatedBy(UUID createdBy);

    List<SavedQuery> findByTeamIdAndIsSharedTrue(UUID teamId);

    boolean existsByTeamIdAndName(UUID teamId, String name);
}
