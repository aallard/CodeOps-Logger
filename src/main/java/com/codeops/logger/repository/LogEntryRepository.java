package com.codeops.logger.repository;

import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.enums.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LogEntry} entities.
 * Provides extensive query methods for the log query engine.
 */
@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {

    Page<LogEntry> findByTeamId(UUID teamId, Pageable pageable);

    Page<LogEntry> findByTeamIdAndServiceName(UUID teamId, String serviceName, Pageable pageable);

    Page<LogEntry> findByTeamIdAndLevel(UUID teamId, LogLevel level, Pageable pageable);

    Page<LogEntry> findByTeamIdAndServiceNameAndLevel(UUID teamId, String serviceName, LogLevel level, Pageable pageable);

    Page<LogEntry> findByTeamIdAndTimestampBetween(UUID teamId, Instant start, Instant end, Pageable pageable);

    Page<LogEntry> findByTeamIdAndServiceNameAndTimestampBetween(UUID teamId, String serviceName, Instant start, Instant end, Pageable pageable);

    Page<LogEntry> findByTeamIdAndLevelAndTimestampBetween(UUID teamId, LogLevel level, Instant start, Instant end, Pageable pageable);

    Page<LogEntry> findByTeamIdAndCorrelationId(UUID teamId, String correlationId, Pageable pageable);

    List<LogEntry> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    long countByTeamIdAndLevel(UUID teamId, LogLevel level);

    long countByTeamIdAndServiceNameAndLevelAndTimestampBetween(UUID teamId, String serviceName, LogLevel level, Instant start, Instant end);

    long countByTeamIdAndTimestampBetween(UUID teamId, Instant start, Instant end);

    void deleteByTimestampBefore(Instant cutoff);

    void deleteByTeamIdAndTimestampBefore(UUID teamId, Instant cutoff);
}
