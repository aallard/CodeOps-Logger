package com.codeops.logger.service;

import com.codeops.logger.dto.mapper.TraceSpanMapper;
import com.codeops.logger.dto.response.*;
import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.TraceSpan;
import com.codeops.logger.entity.enums.SpanStatus;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.TraceSpanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages trace spans and provides trace assembly, waterfall visualization,
 * and root cause analysis for cross-service request tracing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TraceService {

    private final TraceSpanRepository traceSpanRepository;
    private final LogEntryRepository logEntryRepository;
    private final TraceSpanMapper traceSpanMapper;
    private final TraceAnalysisService analysisService;

    // ==================== Span CRUD ====================

    /**
     * Records a new trace span.
     *
     * @param request the span data
     * @param teamId  the team scope
     * @return the created span response
     */
    @Transactional
    public TraceSpanResponse createSpan(com.codeops.logger.dto.request.CreateTraceSpanRequest request,
                                          UUID teamId) {
        TraceSpan entity = traceSpanMapper.toEntity(request);
        entity.setTeamId(teamId);

        if (request.status() == null) {
            entity.setStatus(SpanStatus.OK);
        }

        if (entity.getDurationMs() == null && entity.getEndTime() != null) {
            entity.setDurationMs(Duration.between(entity.getStartTime(), entity.getEndTime()).toMillis());
        }

        TraceSpan saved = traceSpanRepository.save(entity);
        log.info("Created trace span '{}' (service='{}', operation='{}')",
                saved.getSpanId(), saved.getServiceName(), saved.getOperationName());
        return traceSpanMapper.toResponse(saved);
    }

    /**
     * Records multiple spans in a batch.
     *
     * @param requests list of span data
     * @param teamId   the team scope
     * @return list of created span responses
     */
    @Transactional
    public List<TraceSpanResponse> createSpanBatch(
            List<com.codeops.logger.dto.request.CreateTraceSpanRequest> requests, UUID teamId) {
        List<TraceSpan> entities = new ArrayList<>();
        for (var request : requests) {
            TraceSpan entity = traceSpanMapper.toEntity(request);
            entity.setTeamId(teamId);
            if (request.status() == null) {
                entity.setStatus(SpanStatus.OK);
            }
            if (entity.getDurationMs() == null && entity.getEndTime() != null) {
                entity.setDurationMs(
                        Duration.between(entity.getStartTime(), entity.getEndTime()).toMillis());
            }
            entities.add(entity);
        }

        List<TraceSpan> saved = traceSpanRepository.saveAll(entities);
        log.info("Created {} trace spans in batch", saved.size());
        return traceSpanMapper.toResponseList(saved);
    }

    /**
     * Returns a single span by ID.
     *
     * @param spanId the span entity ID
     * @return the span response
     * @throws NotFoundException if not found
     */
    public TraceSpanResponse getSpan(UUID spanId) {
        TraceSpan span = traceSpanRepository.findById(spanId)
                .orElseThrow(() -> new NotFoundException("Trace span not found: " + spanId));
        return traceSpanMapper.toResponse(span);
    }

    // ==================== Trace Assembly ====================

    /**
     * Assembles a complete trace flow by correlationId.
     * Collects all spans sharing the correlationId and orders by start time.
     *
     * @param correlationId the correlation ID linking spans across services
     * @return the assembled trace flow
     * @throws NotFoundException if no spans found for the correlationId
     */
    public TraceFlowResponse getTraceFlow(String correlationId) {
        List<TraceSpan> spans = traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(correlationId);
        if (spans.isEmpty()) {
            throw new NotFoundException("No trace found for correlationId: " + correlationId);
        }
        return buildTraceFlow(spans, correlationId);
    }

    /**
     * Assembles a trace flow by traceId (alternative to correlationId).
     *
     * @param traceId the trace ID
     * @return the assembled trace flow
     * @throws NotFoundException if no spans found for the traceId
     */
    public TraceFlowResponse getTraceFlowByTraceId(String traceId) {
        List<TraceSpan> spans = traceSpanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        if (spans.isEmpty()) {
            throw new NotFoundException("No trace found for traceId: " + traceId);
        }
        return buildTraceFlow(spans, spans.getFirst().getCorrelationId());
    }

    // ==================== Waterfall Visualization ====================

    /**
     * Builds a waterfall visualization for a trace.
     * Includes span timing offsets, depth levels, and related log entry IDs.
     *
     * @param correlationId the correlation ID
     * @return waterfall visualization data
     * @throws NotFoundException if no spans found
     */
    public TraceWaterfallResponse getWaterfall(String correlationId) {
        List<TraceSpan> spans = traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(correlationId);
        if (spans.isEmpty()) {
            throw new NotFoundException("No trace found for correlationId: " + correlationId);
        }

        List<LogEntry> logEntries = logEntryRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);
        Map<String, List<UUID>> relatedLogsBySpan = buildSpanLogMap(logEntries, spans);

        return analysisService.buildWaterfall(spans, relatedLogsBySpan);
    }

    // ==================== Root Cause Analysis ====================

    /**
     * Performs root cause analysis on a trace.
     * Identifies the earliest error and its propagation chain.
     *
     * @param correlationId the correlation ID
     * @return root cause analysis, or empty if trace has no errors
     * @throws NotFoundException if no spans found
     */
    public Optional<RootCauseAnalysisResponse> getRootCauseAnalysis(String correlationId) {
        List<TraceSpan> spans = traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(correlationId);
        if (spans.isEmpty()) {
            throw new NotFoundException("No trace found for correlationId: " + correlationId);
        }

        List<LogEntry> logEntries = logEntryRepository.findByCorrelationIdOrderByTimestampAsc(correlationId);
        List<UUID> logIds = logEntries.stream().map(LogEntry::getId).toList();

        RootCauseAnalysisResponse analysis = analysisService.analyzeRootCause(spans, logIds);
        return Optional.ofNullable(analysis);
    }

    // ==================== Trace Listing ====================

    /**
     * Lists recent traces for a team with summary information.
     *
     * @param teamId the team scope
     * @param page   page number
     * @param size   page size
     * @return paginated trace summaries
     */
    public PageResponse<TraceListResponse> listRecentTraces(UUID teamId, int page, int size) {
        List<TraceSpan> allSpans = traceSpanRepository.findByTeamId(teamId,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "startTime")))
                .getContent();

        Map<String, List<TraceSpan>> byCorrelationId = allSpans.stream()
                .collect(Collectors.groupingBy(TraceSpan::getCorrelationId, LinkedHashMap::new,
                        Collectors.toList()));

        List<TraceListResponse> summaries = byCorrelationId.values().stream()
                .map(analysisService::buildTraceSummary)
                .toList();

        int totalElements = summaries.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<TraceListResponse> pageContent = summaries.subList(fromIndex, toIndex);
        boolean isLast = toIndex >= totalElements;

        return new PageResponse<>(pageContent, page, size, totalElements, totalPages, isLast);
    }

    /**
     * Lists traces for a specific service.
     *
     * @param teamId      the team scope
     * @param serviceName the service name
     * @param page        page number
     * @param size        page size
     * @return paginated trace summaries
     */
    public PageResponse<TraceListResponse> listTracesByService(UUID teamId,
                                                                  String serviceName, int page, int size) {
        List<TraceSpan> allSpans = traceSpanRepository.findByTeamIdAndServiceName(teamId, serviceName,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "startTime")))
                .getContent();

        Map<String, List<TraceSpan>> byCorrelationId = allSpans.stream()
                .collect(Collectors.groupingBy(TraceSpan::getCorrelationId, LinkedHashMap::new,
                        Collectors.toList()));

        // Reload full traces for each correlationId so summaries include all services
        List<TraceListResponse> summaries = new ArrayList<>();
        for (String corrId : byCorrelationId.keySet()) {
            List<TraceSpan> fullTrace = traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(corrId);
            summaries.add(analysisService.buildTraceSummary(fullTrace));
        }

        int totalElements = summaries.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<TraceListResponse> pageContent = summaries.subList(fromIndex, toIndex);
        boolean isLast = toIndex >= totalElements;

        return new PageResponse<>(pageContent, page, size, totalElements, totalPages, isLast);
    }

    /**
     * Lists traces that contain errors.
     *
     * @param teamId the team scope
     * @param limit  maximum number of traces to return
     * @return list of error trace summaries
     */
    public List<TraceListResponse> listErrorTraces(UUID teamId, int limit) {
        List<TraceSpan> errorSpans = traceSpanRepository.findByTeamIdAndServiceNameAndStatus(
                teamId, null, SpanStatus.ERROR);

        // The repository method filters by serviceName, so get all team spans and filter
        List<TraceSpan> allSpans = traceSpanRepository.findByTeamId(teamId,
                PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "startTime")))
                .getContent();

        Set<String> errorCorrelationIds = allSpans.stream()
                .filter(s -> s.getStatus() == SpanStatus.ERROR)
                .map(TraceSpan::getCorrelationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<TraceListResponse> summaries = new ArrayList<>();
        int count = 0;
        for (String corrId : errorCorrelationIds) {
            if (count >= limit) break;
            List<TraceSpan> fullTrace = traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(corrId);
            summaries.add(analysisService.buildTraceSummary(fullTrace));
            count++;
        }

        return summaries;
    }

    // ==================== Correlation with Logs ====================

    /**
     * Returns log entry IDs associated with a trace (by correlationId).
     *
     * @param correlationId the correlation ID
     * @return list of log entry IDs
     */
    public List<UUID> getRelatedLogEntries(String correlationId) {
        return logEntryRepository.findByCorrelationIdOrderByTimestampAsc(correlationId)
                .stream()
                .map(LogEntry::getId)
                .toList();
    }

    // ==================== Cleanup ====================

    /**
     * Deletes trace spans older than the cutoff.
     *
     * @param cutoff delete spans before this time
     */
    @Transactional
    public void purgeOldSpans(Instant cutoff) {
        traceSpanRepository.deleteByStartTimeBefore(cutoff);
        log.info("Purged trace spans older than {}", cutoff);
    }

    // ==================== Private Helpers ====================

    /**
     * Builds a TraceFlowResponse from a list of spans.
     */
    private TraceFlowResponse buildTraceFlow(List<TraceSpan> spans, String correlationId) {
        List<TraceSpanResponse> responses = traceSpanMapper.toResponseList(spans);

        TraceSpan first = spans.getFirst();
        TraceSpan last = spans.getLast();
        Instant endTime = last.getEndTime() != null ? last.getEndTime() : last.getStartTime();
        long totalDurationMs = Duration.between(first.getStartTime(), endTime).toMillis();

        boolean hasErrors = spans.stream()
                .anyMatch(s -> s.getStatus() == SpanStatus.ERROR);

        String traceId = first.getTraceId();

        return new TraceFlowResponse(
                correlationId, traceId, responses,
                totalDurationMs, spans.size(), hasErrors);
    }

    /**
     * Builds a map of spanId to related log entry IDs.
     * Log entries with a spanId are directly mapped; those without are associated
     * with the closest span by timestamp.
     */
    private Map<String, List<UUID>> buildSpanLogMap(List<LogEntry> logEntries,
                                                      List<TraceSpan> spans) {
        Map<String, List<UUID>> map = new HashMap<>();

        for (LogEntry logEntry : logEntries) {
            String spanId = logEntry.getSpanId();
            if (spanId != null && !spanId.isBlank()) {
                map.computeIfAbsent(spanId, k -> new ArrayList<>()).add(logEntry.getId());
            } else {
                // Associate with closest span by timestamp
                TraceSpan closest = findClosestSpan(logEntry.getTimestamp(), spans);
                if (closest != null) {
                    map.computeIfAbsent(closest.getSpanId(), k -> new ArrayList<>())
                            .add(logEntry.getId());
                }
            }
        }

        return map;
    }

    /**
     * Finds the span whose time range is closest to the given timestamp.
     */
    private TraceSpan findClosestSpan(Instant timestamp, List<TraceSpan> spans) {
        TraceSpan closest = null;
        long minDistance = Long.MAX_VALUE;

        for (TraceSpan span : spans) {
            long distance = Math.abs(Duration.between(span.getStartTime(), timestamp).toMillis());
            if (distance < minDistance) {
                minDistance = distance;
                closest = span;
            }
        }

        return closest;
    }
}
