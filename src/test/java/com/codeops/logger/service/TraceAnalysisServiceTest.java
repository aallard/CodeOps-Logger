package com.codeops.logger.service;

import com.codeops.logger.dto.mapper.TraceSpanMapper;
import com.codeops.logger.dto.response.RootCauseAnalysisResponse;
import com.codeops.logger.dto.response.TraceListResponse;
import com.codeops.logger.dto.response.TraceSpanResponse;
import com.codeops.logger.dto.response.TraceWaterfallResponse;
import com.codeops.logger.entity.TraceSpan;
import com.codeops.logger.entity.enums.SpanStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link TraceAnalysisService}.
 */
@ExtendWith(MockitoExtension.class)
class TraceAnalysisServiceTest {

    @Mock
    private TraceSpanMapper traceSpanMapper;

    @InjectMocks
    private TraceAnalysisService analysisService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final String CORRELATION_ID = "test-correlation-123";
    private static final String TRACE_ID = "test-trace-456";

    private TraceSpan buildSpan(String spanId, String parentSpanId, String service,
                                 String operation, Instant start, Instant end, SpanStatus status) {
        TraceSpan span = new TraceSpan();
        span.setId(UUID.randomUUID());
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setServiceName(service);
        span.setOperationName(operation);
        span.setStartTime(start);
        span.setEndTime(end);
        span.setDurationMs(Duration.between(start, end).toMillis());
        span.setStatus(status);
        span.setCorrelationId(CORRELATION_ID);
        span.setTraceId(TRACE_ID);
        span.setTeamId(TEAM_ID);
        return span;
    }

    private TraceSpanResponse mockResponse(TraceSpan span) {
        return new TraceSpanResponse(
                span.getId(), span.getCorrelationId(), span.getTraceId(),
                span.getSpanId(), span.getParentSpanId(), span.getServiceName(),
                span.getOperationName(), span.getStartTime(), span.getEndTime(),
                span.getDurationMs(), span.getStatus().name(), span.getStatusMessage(),
                span.getTags(), span.getTeamId(), Instant.now());
    }

    // ==================== buildWaterfall() Tests ====================

    @Test
    void testBuildWaterfall_simpleTrace_calculatesOffsets() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan root = buildSpan("span-1", null, "server", "handleRequest",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan child = buildSpan("span-2", "span-1", "server", "validateInput",
                base.plusMillis(50), base.plusMillis(200), SpanStatus.OK);

        TraceWaterfallResponse result = analysisService.buildWaterfall(
                List.of(root, child), Map.of());

        assertThat(result.spanCount()).isEqualTo(2);
        assertThat(result.totalDurationMs()).isEqualTo(500);
        assertThat(result.spans().get(0).offsetMs()).isZero();
        assertThat(result.spans().get(1).offsetMs()).isEqualTo(50);
    }

    @Test
    void testBuildWaterfall_nestedSpans_correctDepths() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan root = buildSpan("span-1", null, "server", "handle",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan child = buildSpan("span-2", "span-1", "server", "validate",
                base.plusMillis(10), base.plusMillis(100), SpanStatus.OK);
        TraceSpan grandchild = buildSpan("span-3", "span-2", "db", "query",
                base.plusMillis(20), base.plusMillis(80), SpanStatus.OK);

        TraceWaterfallResponse result = analysisService.buildWaterfall(
                List.of(root, child, grandchild), Map.of());

        assertThat(result.spans().get(0).depth()).isZero(); // root
        assertThat(result.spans().get(1).depth()).isEqualTo(1); // child
        assertThat(result.spans().get(2).depth()).isEqualTo(2); // grandchild
    }

    @Test
    void testBuildWaterfall_parallelSpans_sameDepth() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan root = buildSpan("span-1", null, "server", "handle",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan child1 = buildSpan("span-2", "span-1", "svc-a", "callA",
                base.plusMillis(10), base.plusMillis(200), SpanStatus.OK);
        TraceSpan child2 = buildSpan("span-3", "span-1", "svc-b", "callB",
                base.plusMillis(10), base.plusMillis(300), SpanStatus.OK);

        TraceWaterfallResponse result = analysisService.buildWaterfall(
                List.of(root, child1, child2), Map.of());

        assertThat(result.spans().get(1).depth()).isEqualTo(1);
        assertThat(result.spans().get(2).depth()).isEqualTo(1);
    }

    @Test
    void testBuildWaterfall_orphanedSpans_depth0() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        // Both spans have parent IDs that don't exist in the trace
        TraceSpan orphan1 = buildSpan("span-1", "missing-parent", "server", "op1",
                base, base.plusMillis(100), SpanStatus.OK);
        TraceSpan orphan2 = buildSpan("span-2", "also-missing", "server", "op2",
                base.plusMillis(50), base.plusMillis(200), SpanStatus.OK);

        TraceWaterfallResponse result = analysisService.buildWaterfall(
                List.of(orphan1, orphan2), Map.of());

        assertThat(result.spans().get(0).depth()).isZero();
        assertThat(result.spans().get(1).depth()).isZero();
    }

    @Test
    void testBuildWaterfall_countServicesCorrectly() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan s1 = buildSpan("span-1", null, "server", "handle",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan s2 = buildSpan("span-2", "span-1", "auth-service", "validate",
                base.plusMillis(10), base.plusMillis(100), SpanStatus.OK);
        TraceSpan s3 = buildSpan("span-3", "span-1", "db-service", "query",
                base.plusMillis(100), base.plusMillis(400), SpanStatus.OK);

        TraceWaterfallResponse result = analysisService.buildWaterfall(
                List.of(s1, s2, s3), Map.of());

        assertThat(result.serviceCount()).isEqualTo(3);
    }

    @Test
    void testBuildWaterfall_detectsErrors() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan ok = buildSpan("span-1", null, "server", "handle",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan err = buildSpan("span-2", "span-1", "db", "query",
                base.plusMillis(10), base.plusMillis(100), SpanStatus.ERROR);

        TraceWaterfallResponse result = analysisService.buildWaterfall(
                List.of(ok, err), Map.of());

        assertThat(result.hasErrors()).isTrue();
    }

    // ==================== analyzeRootCause() Tests ====================

    @Test
    void testAnalyzeRootCause_findsEarliestError() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan ok = buildSpan("span-1", null, "server", "handle",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan err1 = buildSpan("span-2", "span-1", "db", "query",
                base.plusMillis(50), base.plusMillis(100), SpanStatus.ERROR);
        err1.setStatusMessage("Connection refused");
        TraceSpan err2 = buildSpan("span-3", "span-1", "server", "respond",
                base.plusMillis(200), base.plusMillis(400), SpanStatus.ERROR);
        err2.setStatusMessage("Internal error");

        lenient().when(traceSpanMapper.toResponse(any(TraceSpan.class)))
                .thenAnswer(inv -> mockResponse(inv.getArgument(0)));

        RootCauseAnalysisResponse result = analysisService.analyzeRootCause(
                List.of(ok, err1, err2), List.of(UUID.randomUUID()));

        assertThat(result).isNotNull();
        assertThat(result.rootCauseService()).isEqualTo("db");
        assertThat(result.rootCauseMessage()).isEqualTo("Connection refused");
    }

    @Test
    void testAnalyzeRootCause_buildsErrorChain() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan rootErr = buildSpan("span-1", null, "db", "connect",
                base, base.plusMillis(50), SpanStatus.ERROR);
        rootErr.setStatusMessage("Timeout");
        TraceSpan childErr = buildSpan("span-2", "span-1", "server", "query",
                base.plusMillis(60), base.plusMillis(100), SpanStatus.ERROR);
        childErr.setStatusMessage("Query failed");

        lenient().when(traceSpanMapper.toResponse(any(TraceSpan.class)))
                .thenAnswer(inv -> mockResponse(inv.getArgument(0)));

        RootCauseAnalysisResponse result = analysisService.analyzeRootCause(
                List.of(rootErr, childErr), List.of());

        assertThat(result.errorChain()).hasSize(2);
        assertThat(result.impactedServiceCount()).isEqualTo(2);
    }

    @Test
    void testAnalyzeRootCause_noErrors_returnsNull() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan ok = buildSpan("span-1", null, "server", "handle",
                base, base.plusMillis(100), SpanStatus.OK);

        RootCauseAnalysisResponse result = analysisService.analyzeRootCause(
                List.of(ok), List.of());

        assertThat(result).isNull();
    }

    // ==================== findRootSpan() Tests ====================

    @Test
    void testFindRootSpan_noParent() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan root = buildSpan("span-1", null, "server", "handle",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan child = buildSpan("span-2", "span-1", "db", "query",
                base.plusMillis(10), base.plusMillis(100), SpanStatus.OK);

        TraceSpan result = analysisService.findRootSpan(List.of(root, child));

        assertThat(result.getSpanId()).isEqualTo("span-1");
    }

    @Test
    void testFindRootSpan_multipleRoots_earliestWins() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan root1 = buildSpan("span-1", null, "svc-a", "op1",
                base.plusMillis(100), base.plusMillis(200), SpanStatus.OK);
        TraceSpan root2 = buildSpan("span-2", null, "svc-b", "op2",
                base, base.plusMillis(300), SpanStatus.OK);

        TraceSpan result = analysisService.findRootSpan(List.of(root1, root2));

        assertThat(result.getSpanId()).isEqualTo("span-2"); // earlier start
    }

    // ==================== buildTraceSummary() Tests ====================

    @Test
    void testBuildTraceSummary_correctCounts() {
        Instant base = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan root = buildSpan("span-1", null, "server", "handleRequest",
                base, base.plusMillis(500), SpanStatus.OK);
        TraceSpan child1 = buildSpan("span-2", "span-1", "auth", "validate",
                base.plusMillis(10), base.plusMillis(100), SpanStatus.OK);
        TraceSpan child2 = buildSpan("span-3", "span-1", "db", "query",
                base.plusMillis(100), base.plusMillis(400), SpanStatus.ERROR);

        TraceListResponse result = analysisService.buildTraceSummary(
                List.of(root, child1, child2));

        assertThat(result.spanCount()).isEqualTo(3);
        assertThat(result.serviceCount()).isEqualTo(3);
        assertThat(result.rootService()).isEqualTo("server");
        assertThat(result.rootOperation()).isEqualTo("handleRequest");
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.totalDurationMs()).isEqualTo(500);
    }
}
