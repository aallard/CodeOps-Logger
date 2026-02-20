package com.codeops.logger.repository;

import com.codeops.logger.entity.TraceSpan;
import com.codeops.logger.entity.enums.SpanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TraceSpan} entities.
 */
@Repository
public interface TraceSpanRepository extends JpaRepository<TraceSpan, UUID> {

    List<TraceSpan> findByCorrelationIdOrderByStartTimeAsc(String correlationId);

    List<TraceSpan> findByTraceIdOrderByStartTimeAsc(String traceId);

    Page<TraceSpan> findByTeamId(UUID teamId, Pageable pageable);

    Page<TraceSpan> findByTeamIdAndServiceName(UUID teamId, String serviceName, Pageable pageable);

    List<TraceSpan> findByTeamIdAndStartTimeBetween(UUID teamId, Instant start, Instant end);

    List<TraceSpan> findByTeamIdAndServiceNameAndStatus(UUID teamId, String serviceName, SpanStatus status);

    void deleteByStartTimeBefore(Instant cutoff);

    long countByTeamId(UUID teamId);
}
