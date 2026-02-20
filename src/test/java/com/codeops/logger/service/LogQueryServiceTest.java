package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.LogEntryMapper;
import com.codeops.logger.dto.mapper.QueryHistoryMapper;
import com.codeops.logger.dto.mapper.SavedQueryMapper;
import com.codeops.logger.dto.request.CreateSavedQueryRequest;
import com.codeops.logger.dto.request.LogQueryRequest;
import com.codeops.logger.dto.request.UpdateSavedQueryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.dto.response.PageResponse;
import com.codeops.logger.dto.response.QueryHistoryResponse;
import com.codeops.logger.dto.response.SavedQueryResponse;
import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.QueryHistory;
import com.codeops.logger.entity.SavedQuery;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.exception.AuthorizationException;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.QueryHistoryRepository;
import com.codeops.logger.repository.SavedQueryRepository;
import com.codeops.logger.service.LogQueryDslParser.DslCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for {@link LogQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class LogQueryServiceTest {

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private SavedQueryRepository savedQueryRepository;

    @Mock
    private QueryHistoryRepository queryHistoryRepository;

    @Mock
    private LogEntryMapper logEntryMapper;

    @Mock
    private SavedQueryMapper savedQueryMapper;

    @Mock
    private QueryHistoryMapper queryHistoryMapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private LogQueryDslParser dslParser;

    private LogQueryService logQueryService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        logQueryService = new LogQueryService(
                logEntryRepository,
                savedQueryRepository,
                queryHistoryRepository,
                logEntryMapper,
                savedQueryMapper,
                queryHistoryMapper,
                objectMapper,
                entityManager,
                dslParser
        );
    }

    // ==================== Structured Query Tests ====================

    @SuppressWarnings("unchecked")
    private void setupCriteriaQuery(List<LogEntry> results, long count) {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<LogEntry> cq = mock(CriteriaQuery.class);
        CriteriaQuery<Long> countCq = mock(CriteriaQuery.class);
        Root<LogEntry> root = mock(Root.class);
        Root<LogEntry> countRoot = mock(Root.class);
        TypedQuery<LogEntry> typedQuery = mock(TypedQuery.class);
        TypedQuery<Long> countTypedQuery = mock(TypedQuery.class);
        Path<Object> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Expression<String> stringExpr = mock(Expression.class);
        Order order = mock(Order.class);

        lenient().when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        lenient().when(cb.createQuery(LogEntry.class)).thenReturn(cq);
        lenient().when(cb.createQuery(Long.class)).thenReturn(countCq);
        lenient().when(cq.from(LogEntry.class)).thenReturn(root);
        lenient().when(countCq.from(LogEntry.class)).thenReturn(countRoot);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(countRoot.get(anyString())).thenReturn(path);
        lenient().when(cb.equal(any(), any(UUID.class))).thenReturn(predicate);
        lenient().when(cb.equal(any(Path.class), any(String.class))).thenReturn(predicate);
        lenient().when(cb.equal(any(Path.class), any(LogLevel.class))).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(Instant.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(), any(Instant.class))).thenReturn(predicate);
        lenient().when(cb.like(any(Expression.class), anyString())).thenReturn(predicate);
        lenient().when(cb.lower(any())).thenReturn(stringExpr);
        lenient().when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        lenient().when(cb.desc(any())).thenReturn(order);
        lenient().when(cb.count(any())).thenReturn(mock(Expression.class));
        lenient().when(cq.where(any(Predicate[].class))).thenReturn(cq);
        lenient().when(cq.orderBy(any(Order.class))).thenReturn(cq);
        lenient().when(countCq.select(any())).thenReturn(countCq);
        lenient().when(countCq.where(any(Predicate[].class))).thenReturn(countCq);
        lenient().when(entityManager.createQuery(cq)).thenReturn(typedQuery);
        lenient().when(entityManager.createQuery(countCq)).thenReturn(countTypedQuery);
        lenient().when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        lenient().when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
        lenient().when(typedQuery.getResultList()).thenReturn(results);
        lenient().when(countTypedQuery.getSingleResult()).thenReturn(count);

        when(logEntryMapper.toResponseList(any())).thenReturn(
                results.stream().map(e -> mock(LogEntryResponse.class)).toList()
        );
    }

    @Test
    void testQuery_byServiceName_returnsFiltered() {
        LogEntry entry = new LogEntry();
        entry.setServiceName("test-service");
        setupCriteriaQuery(List.of(entry), 1L);

        LogQueryRequest request = new LogQueryRequest(
                "test-service", null, null, null, null, null, null, null, null, 0, 20);

        PageResponse<LogEntryResponse> result = logQueryService.query(request, TEAM_ID, USER_ID);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        verify(queryHistoryRepository).save(any(QueryHistory.class));
    }

    @Test
    void testQuery_byLevel_returnsFiltered() {
        LogEntry entry = new LogEntry();
        entry.setLevel(LogLevel.ERROR);
        setupCriteriaQuery(List.of(entry), 1L);

        LogQueryRequest request = new LogQueryRequest(
                null, "ERROR", null, null, null, null, null, null, null, 0, 20);

        PageResponse<LogEntryResponse> result = logQueryService.query(request, TEAM_ID, USER_ID);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testQuery_byTimeRange_returnsFiltered() {
        LogEntry entry = new LogEntry();
        setupCriteriaQuery(List.of(entry), 1L);

        Instant start = Instant.parse("2026-02-01T00:00:00Z");
        Instant end = Instant.parse("2026-02-15T00:00:00Z");
        LogQueryRequest request = new LogQueryRequest(
                null, null, start, end, null, null, null, null, null, 0, 20);

        PageResponse<LogEntryResponse> result = logQueryService.query(request, TEAM_ID, USER_ID);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testQuery_byCorrelationId_returnsFiltered() {
        LogEntry entry = new LogEntry();
        entry.setCorrelationId("abc-123");
        setupCriteriaQuery(List.of(entry), 1L);

        LogQueryRequest request = new LogQueryRequest(
                null, null, null, null, "abc-123", null, null, null, null, 0, 20);

        PageResponse<LogEntryResponse> result = logQueryService.query(request, TEAM_ID, USER_ID);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testQuery_multipleFilters_returnsIntersection() {
        LogEntry entry = new LogEntry();
        setupCriteriaQuery(List.of(entry), 1L);

        LogQueryRequest request = new LogQueryRequest(
                "svc", "ERROR", Instant.now().minusSeconds(3600), Instant.now(),
                null, null, null, null, null, 0, 20);

        PageResponse<LogEntryResponse> result = logQueryService.query(request, TEAM_ID, USER_ID);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testQuery_noFilters_returnsAll() {
        LogEntry e1 = new LogEntry();
        LogEntry e2 = new LogEntry();
        setupCriteriaQuery(List.of(e1, e2), 2L);

        LogQueryRequest request = new LogQueryRequest(
                null, null, null, null, null, null, null, null, null, null, null);

        PageResponse<LogEntryResponse> result = logQueryService.query(request, TEAM_ID, USER_ID);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void testQuery_pagination_returnsCorrectPage() {
        setupCriteriaQuery(List.of(new LogEntry()), 50L);

        LogQueryRequest request = new LogQueryRequest(
                null, null, null, null, null, null, null, null, null, 2, 10);

        PageResponse<LogEntryResponse> result = logQueryService.query(request, TEAM_ID, USER_ID);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(50);
        assertThat(result.totalPages()).isEqualTo(5);
    }

    @Test
    void testQuery_recordsHistory() {
        setupCriteriaQuery(List.of(), 0L);

        LogQueryRequest request = new LogQueryRequest(
                null, null, null, null, null, null, null, null, null, 0, 20);

        logQueryService.query(request, TEAM_ID, USER_ID);

        ArgumentCaptor<QueryHistory> captor = ArgumentCaptor.forClass(QueryHistory.class);
        verify(queryHistoryRepository).save(captor.capture());
        QueryHistory saved = captor.getValue();
        assertThat(saved.getTeamId()).isEqualTo(TEAM_ID);
        assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(saved.getResultCount()).isEqualTo(0L);
    }

    // ==================== Full-Text Search Tests ====================

    @Test
    void testSearch_matchesMessage() {
        LogEntry entry = new LogEntry();
        entry.setMessage("Connection timeout occurred");
        setupCriteriaQuery(List.of(entry), 1L);

        PageResponse<LogEntryResponse> result = logQueryService.search(
                "timeout", TEAM_ID, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testSearch_matchesExceptionClass() {
        LogEntry entry = new LogEntry();
        entry.setExceptionClass("java.sql.SQLException");
        setupCriteriaQuery(List.of(entry), 1L);

        PageResponse<LogEntryResponse> result = logQueryService.search(
                "SQLException", TEAM_ID, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testSearch_matchesLoggerName() {
        LogEntry entry = new LogEntry();
        entry.setLoggerName("com.codeops.AuthService");
        setupCriteriaQuery(List.of(entry), 1L);

        PageResponse<LogEntryResponse> result = logQueryService.search(
                "AuthService", TEAM_ID, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testSearch_caseInsensitive() {
        LogEntry entry = new LogEntry();
        entry.setMessage("ERROR occurred");
        setupCriteriaQuery(List.of(entry), 1L);

        PageResponse<LogEntryResponse> result = logQueryService.search(
                "error", TEAM_ID, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testSearch_noResults_returnsEmpty() {
        setupCriteriaQuery(List.of(), 0L);

        PageResponse<LogEntryResponse> result = logQueryService.search(
                "nonexistent", TEAM_ID, null, null, 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void testSearch_withTimeRange_filtersCorrectly() {
        LogEntry entry = new LogEntry();
        setupCriteriaQuery(List.of(entry), 1L);

        Instant start = Instant.parse("2026-02-01T00:00:00Z");
        Instant end = Instant.parse("2026-02-15T00:00:00Z");

        PageResponse<LogEntryResponse> result = logQueryService.search(
                "test", TEAM_ID, start, end, 0, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void testSearch_emptyTerm_throwsValidation() {
        assertThatThrownBy(() -> logQueryService.search("", TEAM_ID, null, null, 0, 20))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Search term cannot be empty");
    }

    // ==================== Saved Query Tests ====================

    @Test
    void testSaveQuery_createsSuccessfully() {
        CreateSavedQueryRequest request = new CreateSavedQueryRequest(
                "My Query", "Description", "{\"level\":\"ERROR\"}", null, false);

        SavedQuery entity = new SavedQuery();
        entity.setName("My Query");
        entity.setQueryJson("{\"level\":\"ERROR\"}");

        SavedQuery saved = new SavedQuery();
        saved.setId(UUID.randomUUID());
        saved.setName("My Query");

        SavedQueryResponse response = new SavedQueryResponse(
                saved.getId(), "My Query", "Description", "{\"level\":\"ERROR\"}",
                null, TEAM_ID, USER_ID, false, null, 0L, Instant.now(), Instant.now());

        when(savedQueryMapper.toEntity(request)).thenReturn(entity);
        when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(saved);
        when(savedQueryMapper.toResponse(saved)).thenReturn(response);

        SavedQueryResponse result = logQueryService.saveQuery(request, TEAM_ID, USER_ID);

        assertThat(result.name()).isEqualTo("My Query");
        verify(savedQueryRepository).save(any(SavedQuery.class));
    }

    @Test
    void testGetSavedQueries_returnsOwnAndShared() {
        SavedQuery own = new SavedQuery();
        own.setId(UUID.randomUUID());
        own.setCreatedBy(USER_ID);
        own.setIsShared(false);

        SavedQuery shared = new SavedQuery();
        shared.setId(UUID.randomUUID());
        shared.setCreatedBy(UUID.randomUUID());
        shared.setIsShared(true);

        when(savedQueryRepository.findByCreatedBy(USER_ID)).thenReturn(List.of(own));
        when(savedQueryRepository.findByTeamIdAndIsSharedTrue(TEAM_ID)).thenReturn(List.of(shared));
        when(savedQueryMapper.toResponseList(any())).thenReturn(List.of(
                mock(SavedQueryResponse.class), mock(SavedQueryResponse.class)));

        List<SavedQueryResponse> result = logQueryService.getSavedQueries(TEAM_ID, USER_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    void testUpdateSavedQuery_byOwner_succeeds() {
        UUID queryId = UUID.randomUUID();
        SavedQuery entity = new SavedQuery();
        entity.setId(queryId);
        entity.setCreatedBy(USER_ID);
        entity.setName("Old Name");
        entity.setQueryJson("{}");

        UpdateSavedQueryRequest request = new UpdateSavedQueryRequest(
                "New Name", null, null, null, null);

        SavedQueryResponse response = new SavedQueryResponse(
                queryId, "New Name", null, "{}", null, TEAM_ID, USER_ID,
                false, null, 0L, Instant.now(), Instant.now());

        when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(entity));
        when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(entity);
        when(savedQueryMapper.toResponse(entity)).thenReturn(response);

        SavedQueryResponse result = logQueryService.updateSavedQuery(queryId, request, USER_ID);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(entity.getName()).isEqualTo("New Name");
    }

    @Test
    void testUpdateSavedQuery_byNonOwner_throwsAuthorization() {
        UUID queryId = UUID.randomUUID();
        SavedQuery entity = new SavedQuery();
        entity.setId(queryId);
        entity.setCreatedBy(UUID.randomUUID()); // different user

        UpdateSavedQueryRequest request = new UpdateSavedQueryRequest(
                "New Name", null, null, null, null);

        when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> logQueryService.updateSavedQuery(queryId, request, USER_ID))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Only the query owner");
    }

    @Test
    void testDeleteSavedQuery_byOwner_succeeds() {
        UUID queryId = UUID.randomUUID();
        SavedQuery entity = new SavedQuery();
        entity.setId(queryId);
        entity.setName("To Delete");
        entity.setCreatedBy(USER_ID);

        when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(entity));

        logQueryService.deleteSavedQuery(queryId, USER_ID);

        verify(savedQueryRepository).delete(entity);
    }

    @Test
    void testDeleteSavedQuery_byNonOwner_throwsAuthorization() {
        UUID queryId = UUID.randomUUID();
        SavedQuery entity = new SavedQuery();
        entity.setId(queryId);
        entity.setCreatedBy(UUID.randomUUID());

        when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> logQueryService.deleteSavedQuery(queryId, USER_ID))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void testExecuteSavedQuery_updatesStats() {
        UUID queryId = UUID.randomUUID();
        SavedQuery savedQuery = new SavedQuery();
        savedQuery.setId(queryId);
        savedQuery.setQueryJson("{\"serviceName\":null,\"level\":null,\"startTime\":null,\"endTime\":null,\"correlationId\":null,\"query\":null,\"loggerName\":null,\"exceptionClass\":null,\"hostName\":null,\"page\":0,\"size\":20}");
        savedQuery.setExecutionCount(5L);

        setupCriteriaQuery(List.of(), 0L);

        when(savedQueryRepository.findById(queryId)).thenReturn(Optional.of(savedQuery));
        when(savedQueryRepository.save(any(SavedQuery.class))).thenReturn(savedQuery);

        logQueryService.executeSavedQuery(queryId, TEAM_ID, USER_ID, 0, 20);

        assertThat(savedQuery.getExecutionCount()).isEqualTo(6L);
        assertThat(savedQuery.getLastExecutedAt()).isNotNull();
        verify(savedQueryRepository).save(savedQuery);
    }

    @Test
    void testGetSavedQuery_notFound_throwsException() {
        UUID queryId = UUID.randomUUID();
        when(savedQueryRepository.findById(queryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logQueryService.getSavedQuery(queryId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Saved query not found");
    }

    // ==================== Query History Tests ====================

    @Test
    void testGetQueryHistory_returnsMostRecentFirst() {
        QueryHistory h1 = new QueryHistory();
        h1.setCreatedBy(USER_ID);
        QueryHistory h2 = new QueryHistory();
        h2.setCreatedBy(USER_ID);

        QueryHistoryResponse r1 = mock(QueryHistoryResponse.class);
        QueryHistoryResponse r2 = mock(QueryHistoryResponse.class);

        when(queryHistoryRepository.findByCreatedByOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h1, h2)));
        when(queryHistoryMapper.toResponseList(any())).thenReturn(List.of(r1, r2));

        PageResponse<QueryHistoryResponse> result = logQueryService.getQueryHistory(USER_ID, 0, 20);

        assertThat(result.content()).hasSize(2);
    }

    @Test
    void testGetQueryHistory_paginates() {
        when(queryHistoryRepository.findByCreatedByOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(queryHistoryMapper.toResponseList(any())).thenReturn(List.of());

        PageResponse<QueryHistoryResponse> result = logQueryService.getQueryHistory(USER_ID, 0, 10);

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    void testGetQueryHistory_emptyForNewUser() {
        when(queryHistoryRepository.findByCreatedByOrderByCreatedAtDesc(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(queryHistoryMapper.toResponseList(any())).thenReturn(List.of());

        PageResponse<QueryHistoryResponse> result = logQueryService.getQueryHistory(
                UUID.randomUUID(), 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void testRecordQueryHistory_persists() {
        logQueryService.recordQueryHistory(
                "{\"level\":\"ERROR\"}", null, 42L, 150L, TEAM_ID, USER_ID);

        ArgumentCaptor<QueryHistory> captor = ArgumentCaptor.forClass(QueryHistory.class);
        verify(queryHistoryRepository).save(captor.capture());
        QueryHistory saved = captor.getValue();
        assertThat(saved.getQueryJson()).isEqualTo("{\"level\":\"ERROR\"}");
        assertThat(saved.getResultCount()).isEqualTo(42L);
        assertThat(saved.getExecutionTimeMs()).isEqualTo(150L);
        assertThat(saved.getTeamId()).isEqualTo(TEAM_ID);
        assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
    }

    @Test
    void testRecordQueryHistory_capturesExecutionTime() {
        logQueryService.recordQueryHistory("{}", null, 10L, 275L, TEAM_ID, USER_ID);

        ArgumentCaptor<QueryHistory> captor = ArgumentCaptor.forClass(QueryHistory.class);
        verify(queryHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getExecutionTimeMs()).isEqualTo(275L);
    }

    @Test
    void testRecordQueryHistory_capturesResultCount() {
        logQueryService.recordQueryHistory("{}", null, 99L, 50L, TEAM_ID, USER_ID);

        ArgumentCaptor<QueryHistory> captor = ArgumentCaptor.forClass(QueryHistory.class);
        verify(queryHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getResultCount()).isEqualTo(99L);
    }

    @Test
    void testRecordQueryHistory_capturesDsl() {
        logQueryService.recordQueryHistory(null, "level = ERROR", 5L, 30L, TEAM_ID, USER_ID);

        ArgumentCaptor<QueryHistory> captor = ArgumentCaptor.forClass(QueryHistory.class);
        verify(queryHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQueryDsl()).isEqualTo("level = ERROR");
        assertThat(captor.getValue().getQueryJson()).isEqualTo("{}");
    }
}
