package com.codeops.logger.repository;

import com.codeops.logger.entity.TrapCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TrapCondition} entities.
 */
@Repository
public interface TrapConditionRepository extends JpaRepository<TrapCondition, UUID> {

    List<TrapCondition> findByTrapId(UUID trapId);

    void deleteByTrapId(UUID trapId);
}
