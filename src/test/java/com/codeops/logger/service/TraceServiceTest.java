package com.codeops.logger.service;

import com.codeops.logger.dto.mapper.TraceSpanMapper;
import com.codeops.logger.dto.request.CreateTraceSpanRequest;
import com.codeops.logger.dto.response.*;
import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.TraceSpan;
import com.codeops.logger.entity.enums.SpanStatus;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.TraceSpanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TraceService}.
 */
@ExtendWith(MockitoExtension.class)
class TraceServiceTest {

    @Mock
    private TraceSpanRepository traceSpanRepository;

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private TraceSpanMapper traceSpanMapper;

    @Mock
    private TraceAnalysisService analysisService;

    @InjectMocks
    private TraceService traceService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final String CORRELATION_ID = "corr-123";
    private static final String TRACE_ID = "trace-456";

    private TraceSpan createSpanEntity(String spanId, String parentSpanId, String service,
                                         String operation, Instant start, Instant end,
                                         SpanStatus status) {
        TraceSpan span = new TraceSpan();
        span.setId(UUID.randomUUID());
        span.setCorrelationId(CORRELATION_ID);
        span.setTraceId(TRACE_ID);
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setServiceName(service);
        span.setOperationName(operation);
        span.setStartTime(start);
        span.setEndTime(end);
        span.setDurationMs(end != null ? end.toEpochMilli() - start.toEpochMilli() : null);
        span.setStatus(status);
        span.setTeamId(TEAM_ID);
        return span;
    }

    private TraceSpanResponse createResponse(TraceSpan span) {
        return new TraceSpanResponse(
                span.getId(), span.getCorrelationId(), span.getTraceId(),
                span.getSpanId(), span.getParentSpanId(), span.getServiceName(),
                span.getOperationName(), span.getStartTime(), span.getEndTime(),
                span.getDurationMs(), span.getStatus().name(), span.getStatusMessage(),
                span.getTags(), span.getTeamId(), Instant.now());
    }

    // ==================== Span CRUD Tests ====================

    @Test
    void testCreateSpan_valid_persists() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        Instant end = Instant.parse("2026-02-20T10:00:00.500Z");
        CreateTraceSpanRequest request = new CreateTraceSpanRequest(
                CORRELATION_ID, TRACE_ID, "span-1", null, "server",
                "handleRequest", start, end, 500L, "OK", null, null);

        TraceSpan entity = createSpanEntity("span-1", null, "server", "handleRequest",
                start, end, SpanStatus.OK);
        entity.setDurationMs(500L);
        TraceSpanResponse response = createResponse(entity);

        when(traceSpanMapper.toEntity(request)).thenReturn(entity);
        when(traceSpanRepository.save(any(TraceSpan.class))).thenReturn(entity);
        when(traceSpanMapper.toResponse(entity)).thenReturn(response);

        TraceSpanResponse result = traceService.createSpan(request, TEAM_ID);

        assertThat(result.spanId()).isEqualTo("span-1");
        verify(traceSpanRepository).save(any(TraceSpan.class));
    }

    @Test
    void testCreateSpan_calculatesDurationFromTimes() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        Instant end = Instant.parse("2026-02-20T10:00:00.250Z");
        CreateTraceSpanRequest request = new CreateTraceSpanRequest(
                CORRELATION_ID, TRACE_ID, "span-1", null, "server",
                "handle", start, end, null, "OK", null, null);

        TraceSpan entity = createSpanEntity("span-1", null, "server", "handle",
                start, end, SpanStatus.OK);
        entity.setDurationMs(null); // Will be calculated
        TraceSpanResponse response = createResponse(entity);

        when(traceSpanMapper.toEntity(request)).thenReturn(entity);
        when(traceSpanRepository.save(any(TraceSpan.class))).thenReturn(entity);
        when(traceSpanMapper.toResponse(entity)).thenReturn(response);

        traceService.createSpan(request, TEAM_ID);

        ArgumentCaptor<TraceSpan> captor = ArgumentCaptor.forClass(TraceSpan.class);
        verify(traceSpanRepository).save(captor.capture());
        assertThat(captor.getValue().getDurationMs()).isEqualTo(250L);
    }

    @Test
    void testCreateSpan_defaultStatusOK() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        CreateTraceSpanRequest request = new CreateTraceSpanRequest(
                CORRELATION_ID, TRACE_ID, "span-1", null, "server",
                "handle", start, null, null, null, null, null);

        TraceSpan entity = createSpanEntity("span-1", null, "server", "handle",
                start, start, SpanStatus.OK);
        TraceSpanResponse response = createResponse(entity);
        entity.setStatus(null); // simulate mapper not setting status

        when(traceSpanMapper.toEntity(request)).thenReturn(entity);
        when(traceSpanRepository.save(any(TraceSpan.class))).thenReturn(entity);
        when(traceSpanMapper.toResponse(entity)).thenReturn(response);

        traceService.createSpan(request, TEAM_ID);

        ArgumentCaptor<TraceSpan> captor = ArgumentCaptor.forClass(TraceSpan.class);
        verify(traceSpanRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SpanStatus.OK);
    }

    @Test
    void testCreateSpanBatch_savesAll() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        CreateTraceSpanRequest req1 = new CreateTraceSpanRequest(
                CORRELATION_ID, TRACE_ID, "span-1", null, "server",
                "handle", start, start.plusMillis(500), 500L, "OK", null, null);
        CreateTraceSpanRequest req2 = new CreateTraceSpanRequest(
                CORRELATION_ID, TRACE_ID, "span-2", "span-1", "db",
                "query", start.plusMillis(10), start.plusMillis(100), 90L, "OK", null, null);

        TraceSpan e1 = createSpanEntity("span-1", null, "server", "handle",
                start, start.plusMillis(500), SpanStatus.OK);
        TraceSpan e2 = createSpanEntity("span-2", "span-1", "db", "query",
                start.plusMillis(10), start.plusMillis(100), SpanStatus.OK);

        when(traceSpanMapper.toEntity(req1)).thenReturn(e1);
        when(traceSpanMapper.toEntity(req2)).thenReturn(e2);
        when(traceSpanRepository.saveAll(anyList())).thenReturn(List.of(e1, e2));
        when(traceSpanMapper.toResponseList(any())).thenReturn(List.of(
                mock(TraceSpanResponse.class), mock(TraceSpanResponse.class)));

        List<TraceSpanResponse> result = traceService.createSpanBatch(List.of(req1, req2), TEAM_ID);

        assertThat(result).hasSize(2);
        verify(traceSpanRepository).saveAll(anyList());
    }

    // ==================== Trace Assembly Tests ====================

    @Test
    void testGetTraceFlow_assemblesSpans() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan s1 = createSpanEntity("span-1", null, "server", "handle",
                start, start.plusMillis(500), SpanStatus.OK);
        TraceSpan s2 = createSpanEntity("span-2", "span-1", "db", "query",
                start.plusMillis(10), start.plusMillis(200), SpanStatus.OK);

        when(traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(CORRELATION_ID))
                .thenReturn(List.of(s1, s2));
        when(traceSpanMapper.toResponseList(any())).thenReturn(List.of(
                createResponse(s1), createResponse(s2)));

        TraceFlowResponse result = traceService.getTraceFlow(CORRELATION_ID);

        assertThat(result.spanCount()).isEqualTo(2);
        assertThat(result.correlationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void testGetTraceFlow_noSpans_throwsNotFound() {
        when(traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc("missing"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> traceService.getTraceFlow("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No trace found");
    }

    @Test
    void testGetTraceFlow_calculatesTotalDuration() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan s1 = createSpanEntity("span-1", null, "server", "handle",
                start, start.plusMillis(1000), SpanStatus.OK);

        when(traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(CORRELATION_ID))
                .thenReturn(List.of(s1));
        when(traceSpanMapper.toResponseList(any())).thenReturn(List.of(createResponse(s1)));

        TraceFlowResponse result = traceService.getTraceFlow(CORRELATION_ID);

        assertThat(result.totalDurationMs()).isEqualTo(1000);
    }

    // ==================== Waterfall Tests ====================

    @Test
    void testGetWaterfall_includesRelatedLogIds() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan s1 = createSpanEntity("span-1", null, "server", "handle",
                start, start.plusMillis(500), SpanStatus.OK);

        LogEntry log1 = new LogEntry();
        log1.setId(UUID.randomUUID());
        log1.setCorrelationId(CORRELATION_ID);
        log1.setSpanId("span-1");
        log1.setTimestamp(start.plusMillis(20));

        TraceWaterfallResponse waterfall = new TraceWaterfallResponse(
                CORRELATION_ID, TRACE_ID, 500L, 1, 1, false, List.of());

        when(traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(CORRELATION_ID))
                .thenReturn(List.of(s1));
        when(logEntryRepository.findByCorrelationIdOrderByTimestampAsc(CORRELATION_ID))
                .thenReturn(List.of(log1));
        when(analysisService.buildWaterfall(eq(List.of(s1)), any()))
                .thenReturn(waterfall);

        TraceWaterfallResponse result = traceService.getWaterfall(CORRELATION_ID);

        assertThat(result).isNotNull();
        verify(analysisService).buildWaterfall(eq(List.of(s1)), any());
    }

    // ==================== Root Cause Analysis Tests ====================

    @Test
    void testGetRootCauseAnalysis_withErrors_returnsAnalysis() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan errSpan = createSpanEntity("span-1", null, "db", "query",
                start, start.plusMillis(100), SpanStatus.ERROR);
        errSpan.setStatusMessage("Connection timeout");

        RootCauseAnalysisResponse rca = new RootCauseAnalysisResponse(
                CORRELATION_ID, TRACE_ID, mock(TraceSpanResponse.class),
                "db", "Connection timeout", List.of(), List.of(), 1, 100L);

        when(traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(CORRELATION_ID))
                .thenReturn(List.of(errSpan));
        when(logEntryRepository.findByCorrelationIdOrderByTimestampAsc(CORRELATION_ID))
                .thenReturn(List.of());
        when(analysisService.analyzeRootCause(any(), any())).thenReturn(rca);

        Optional<RootCauseAnalysisResponse> result = traceService.getRootCauseAnalysis(CORRELATION_ID);

        assertThat(result).isPresent();
        assertThat(result.get().rootCauseService()).isEqualTo("db");
    }

    @Test
    void testGetRootCauseAnalysis_noErrors_returnsEmpty() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan okSpan = createSpanEntity("span-1", null, "server", "handle",
                start, start.plusMillis(100), SpanStatus.OK);

        when(traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc(CORRELATION_ID))
                .thenReturn(List.of(okSpan));
        when(logEntryRepository.findByCorrelationIdOrderByTimestampAsc(CORRELATION_ID))
                .thenReturn(List.of());
        when(analysisService.analyzeRootCause(any(), any())).thenReturn(null);

        Optional<RootCauseAnalysisResponse> result = traceService.getRootCauseAnalysis(CORRELATION_ID);

        assertThat(result).isEmpty();
    }

    // ==================== Listing Tests ====================

    @Test
    void testListRecentTraces_returnsSummaries() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan s1 = createSpanEntity("span-1", null, "server", "handle",
                start, start.plusMillis(500), SpanStatus.OK);

        TraceListResponse summary = new TraceListResponse(
                CORRELATION_ID, TRACE_ID, "server", "handle",
                1, 1, 500L, false, start, start.plusMillis(500));

        when(traceSpanRepository.findByTeamId(eq(TEAM_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(s1)));
        when(analysisService.buildTraceSummary(any())).thenReturn(summary);

        PageResponse<TraceListResponse> result = traceService.listRecentTraces(TEAM_ID, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().rootService()).isEqualTo("server");
    }

    @Test
    void testListErrorTraces_onlyErrors() {
        Instant start = Instant.parse("2026-02-20T10:00:00Z");
        TraceSpan okSpan = createSpanEntity("span-1", null, "server", "handle",
                start, start.plusMillis(500), SpanStatus.OK);
        okSpan.setCorrelationId("ok-corr");
        TraceSpan errSpan = createSpanEntity("span-2", null, "db", "query",
                start.plusMillis(10), start.plusMillis(100), SpanStatus.ERROR);
        errSpan.setCorrelationId("err-corr");

        TraceListResponse errSummary = new TraceListResponse(
                "err-corr", TRACE_ID, "db", "query",
                1, 1, 90L, true, start.plusMillis(10), start.plusMillis(100));

        when(traceSpanRepository.findByTeamId(eq(TEAM_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(okSpan, errSpan)));
        when(traceSpanRepository.findByCorrelationIdOrderByStartTimeAsc("err-corr"))
                .thenReturn(List.of(errSpan));
        when(analysisService.buildTraceSummary(any())).thenReturn(errSummary);

        List<TraceListResponse> result = traceService.listErrorTraces(TEAM_ID, 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().hasErrors()).isTrue();
    }

    // ==================== Cleanup Tests ====================

    @Test
    void testPurgeOldSpans_deletesBeforeCutoff() {
        Instant cutoff = Instant.parse("2026-02-10T00:00:00Z");

        traceService.purgeOldSpans(cutoff);

        verify(traceSpanRepository).deleteByStartTimeBefore(cutoff);
    }
}
