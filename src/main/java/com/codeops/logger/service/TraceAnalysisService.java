package com.codeops.logger.service;

import com.codeops.logger.dto.mapper.TraceSpanMapper;
import com.codeops.logger.dto.response.RootCauseAnalysisResponse;
import com.codeops.logger.dto.response.TraceListResponse;
import com.codeops.logger.dto.response.TraceSpanResponse;
import com.codeops.logger.dto.response.TraceWaterfallResponse;
import com.codeops.logger.entity.TraceSpan;
import com.codeops.logger.entity.enums.SpanStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes trace spans to build waterfall visualizations and perform root cause analysis.
 * Operates on in-memory span lists — no direct database access.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TraceAnalysisService {

    private final TraceSpanMapper traceSpanMapper;

    /**
     * Builds a waterfall visualization from a list of trace spans.
     * Spans are sorted by start time and assigned depth based on parent-child relationships.
     *
     * @param spans             the trace spans (must all share the same correlationId or traceId)
     * @param relatedLogsBySpan map of spanId to list of related log entry IDs
     * @return waterfall response for frontend rendering
     */
    public TraceWaterfallResponse buildWaterfall(List<TraceSpan> spans,
                                                   Map<String, List<UUID>> relatedLogsBySpan) {
        if (spans.isEmpty()) {
            return new TraceWaterfallResponse(null, null, 0L, 0, 0, false, List.of());
        }

        List<TraceSpan> sorted = spans.stream()
                .sorted(Comparator.comparing(TraceSpan::getStartTime))
                .toList();

        TraceSpan root = findRootSpan(sorted);
        Instant rootStart = root.getStartTime();

        Instant lastEnd = sorted.stream()
                .map(s -> s.getEndTime() != null ? s.getEndTime() : s.getStartTime())
                .max(Comparator.naturalOrder())
                .orElse(rootStart);
        long totalDurationMs = Duration.between(rootStart, lastEnd).toMillis();

        Map<String, Integer> depthMap = calculateDepths(sorted);

        Set<String> services = sorted.stream()
                .map(TraceSpan::getServiceName)
                .collect(Collectors.toSet());

        boolean hasErrors = sorted.stream()
                .anyMatch(s -> s.getStatus() == SpanStatus.ERROR);

        List<TraceWaterfallResponse.WaterfallSpan> waterfallSpans = sorted.stream()
                .map(s -> {
                    long offsetMs = Duration.between(rootStart, s.getStartTime()).toMillis();
                    long durationMs = resolveDuration(s);
                    int depth = depthMap.getOrDefault(s.getSpanId(), 0);
                    List<UUID> logIds = relatedLogsBySpan != null
                            ? relatedLogsBySpan.getOrDefault(s.getSpanId(), List.of())
                            : List.of();
                    return new TraceWaterfallResponse.WaterfallSpan(
                            s.getId(), s.getSpanId(), s.getParentSpanId(),
                            s.getServiceName(), s.getOperationName(),
                            offsetMs, durationMs, s.getStatus().name(),
                            s.getStatusMessage(), depth, logIds);
                })
                .toList();

        return new TraceWaterfallResponse(
                root.getCorrelationId(), root.getTraceId(),
                totalDurationMs, sorted.size(), services.size(),
                hasErrors, waterfallSpans);
    }

    /**
     * Performs root cause analysis on a failed trace.
     * Identifies the earliest error span and traces the error propagation chain.
     *
     * @param spans          all spans in the trace
     * @param relatedLogIds  log entry IDs associated with the trace
     * @return root cause analysis, or null if no errors found
     */
    public RootCauseAnalysisResponse analyzeRootCause(List<TraceSpan> spans,
                                                        List<UUID> relatedLogIds) {
        List<TraceSpan> errorSpans = spans.stream()
                .filter(s -> s.getStatus() == SpanStatus.ERROR)
                .sorted(Comparator.comparing(TraceSpan::getStartTime))
                .toList();

        if (errorSpans.isEmpty()) {
            return null;
        }

        TraceSpan rootCause = errorSpans.getFirst();
        TraceSpanResponse rootCauseResponse = traceSpanMapper.toResponse(rootCause);

        List<TraceSpanResponse> errorChain = buildErrorChain(rootCause, errorSpans);

        Set<String> impactedServices = errorSpans.stream()
                .map(TraceSpan::getServiceName)
                .collect(Collectors.toSet());

        Instant earliest = spans.stream()
                .map(TraceSpan::getStartTime)
                .min(Comparator.naturalOrder())
                .orElse(rootCause.getStartTime());
        Instant latest = spans.stream()
                .map(s -> s.getEndTime() != null ? s.getEndTime() : s.getStartTime())
                .max(Comparator.naturalOrder())
                .orElse(rootCause.getStartTime());
        long totalDurationMs = Duration.between(earliest, latest).toMillis();

        return new RootCauseAnalysisResponse(
                rootCause.getCorrelationId(), rootCause.getTraceId(),
                rootCauseResponse, rootCause.getServiceName(),
                rootCause.getStatusMessage(),
                errorChain, relatedLogIds != null ? relatedLogIds : List.of(),
                impactedServices.size(), totalDurationMs);
    }

    /**
     * Calculates the depth of each span in the parent-child tree.
     *
     * @param spans the trace spans
     * @return map of spanId to depth (root = 0)
     */
    Map<String, Integer> calculateDepths(List<TraceSpan> spans) {
        Map<String, Integer> depthMap = new HashMap<>();
        Map<String, List<TraceSpan>> childrenMap = new HashMap<>();

        for (TraceSpan span : spans) {
            String parentId = span.getParentSpanId();
            if (parentId != null) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(span);
            }
        }

        // Find root spans (no parent) and start BFS
        Queue<TraceSpan> queue = new LinkedList<>();
        for (TraceSpan span : spans) {
            if (span.getParentSpanId() == null) {
                depthMap.put(span.getSpanId(), 0);
                queue.add(span);
            }
        }

        while (!queue.isEmpty()) {
            TraceSpan current = queue.poll();
            int currentDepth = depthMap.get(current.getSpanId());
            List<TraceSpan> children = childrenMap.getOrDefault(current.getSpanId(), List.of());
            for (TraceSpan child : children) {
                depthMap.put(child.getSpanId(), currentDepth + 1);
                queue.add(child);
            }
        }

        // Assign depth 0 to orphaned spans (parent not in this trace)
        for (TraceSpan span : spans) {
            depthMap.putIfAbsent(span.getSpanId(), 0);
        }

        return depthMap;
    }

    /**
     * Finds the root span in a trace (no parent, earliest start time).
     *
     * @param spans the spans to search
     * @return the root span, or the earliest span if no root found
     */
    TraceSpan findRootSpan(List<TraceSpan> spans) {
        List<TraceSpan> roots = spans.stream()
                .filter(s -> s.getParentSpanId() == null)
                .toList();

        if (roots.size() == 1) {
            return roots.getFirst();
        }
        if (roots.size() > 1) {
            return roots.stream()
                    .min(Comparator.comparing(TraceSpan::getStartTime))
                    .orElse(roots.getFirst());
        }

        // All spans have parents — return earliest by start time
        return spans.stream()
                .min(Comparator.comparing(TraceSpan::getStartTime))
                .orElse(spans.getFirst());
    }

    /**
     * Builds a summary for a trace (for list views).
     *
     * @param spans all spans in the trace
     * @return trace list summary
     */
    public TraceListResponse buildTraceSummary(List<TraceSpan> spans) {
        if (spans.isEmpty()) {
            return new TraceListResponse(null, null, null, null, 0, 0, 0L, false, null, null);
        }

        TraceSpan root = findRootSpan(spans);

        Instant earliest = spans.stream()
                .map(TraceSpan::getStartTime)
                .min(Comparator.naturalOrder())
                .orElse(root.getStartTime());
        Instant latest = spans.stream()
                .map(s -> s.getEndTime() != null ? s.getEndTime() : s.getStartTime())
                .max(Comparator.naturalOrder())
                .orElse(root.getStartTime());
        long totalDurationMs = Duration.between(earliest, latest).toMillis();

        Set<String> services = spans.stream()
                .map(TraceSpan::getServiceName)
                .collect(Collectors.toSet());

        boolean hasErrors = spans.stream()
                .anyMatch(s -> s.getStatus() == SpanStatus.ERROR);

        return new TraceListResponse(
                root.getCorrelationId(), root.getTraceId(),
                root.getServiceName(), root.getOperationName(),
                spans.size(), services.size(), totalDurationMs,
                hasErrors, earliest, latest);
    }

    /**
     * Builds the error propagation chain from the root cause span.
     * Follows child error spans in chronological order.
     */
    private List<TraceSpanResponse> buildErrorChain(TraceSpan rootCause,
                                                      List<TraceSpan> errorSpans) {
        List<TraceSpanResponse> chain = new ArrayList<>();
        chain.add(traceSpanMapper.toResponse(rootCause));

        Set<String> visited = new HashSet<>();
        visited.add(rootCause.getSpanId());

        Map<String, List<TraceSpan>> childErrorMap = new HashMap<>();
        for (TraceSpan span : errorSpans) {
            if (span.getParentSpanId() != null) {
                childErrorMap.computeIfAbsent(span.getParentSpanId(), k -> new ArrayList<>()).add(span);
            }
        }

        Queue<String> queue = new LinkedList<>();
        queue.add(rootCause.getSpanId());

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<TraceSpan> children = childErrorMap.getOrDefault(current, List.of());
            for (TraceSpan child : children) {
                if (visited.add(child.getSpanId())) {
                    chain.add(traceSpanMapper.toResponse(child));
                    queue.add(child.getSpanId());
                }
            }
        }

        return chain;
    }

    /**
     * Resolves the duration of a span in milliseconds.
     */
    private long resolveDuration(TraceSpan span) {
        if (span.getDurationMs() != null) {
            return span.getDurationMs();
        }
        if (span.getEndTime() != null && span.getStartTime() != null) {
            return Duration.between(span.getStartTime(), span.getEndTime()).toMillis();
        }
        return 0L;
    }
}
