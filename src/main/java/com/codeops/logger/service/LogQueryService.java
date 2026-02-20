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
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.exception.AuthorizationException;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.QueryHistoryRepository;
import com.codeops.logger.repository.SavedQueryRepository;
import com.codeops.logger.service.LogQueryDslParser.DslCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Log query engine providing structured filtering, full-text search, and a SQL-like DSL
 * for searching log entries. Also manages saved queries and query history.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogQueryService {

    private final LogEntryRepository logEntryRepository;
    private final SavedQueryRepository savedQueryRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final LogEntryMapper logEntryMapper;
    private final SavedQueryMapper savedQueryMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final LogQueryDslParser dslParser;

    // ==================== Single Entry Lookup ====================

    /**
     * Retrieves a single log entry by its ID.
     *
     * @param logEntryId the log entry ID
     * @return the log entry response
     * @throws NotFoundException if the log entry is not found
     */
    public LogEntryResponse getLogEntry(UUID logEntryId) {
        LogEntry entry = logEntryRepository.findById(logEntryId)
                .orElseThrow(() -> new NotFoundException("Log entry not found: " + logEntryId));
        return logEntryMapper.toResponse(entry);
    }

    // ==================== Structured Query ====================

    /**
     * Executes a structured log query using field-level filters.
     * Builds a JPA Criteria query from the LogQueryRequest parameters.
     *
     * @param request the query parameters
     * @param teamId  the team scope
     * @param userId  the user executing the query (for history)
     * @return paginated log entry results
     */
    public PageResponse<LogEntryResponse> query(LogQueryRequest request, UUID teamId, UUID userId) {
        long startMs = System.currentTimeMillis();

        int page = (request.page() != null) ? request.page() : 0;
        int size = (request.size() != null) ? request.size() : AppConstants.DEFAULT_PAGE_SIZE;

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Build data query
        CriteriaQuery<LogEntry> cq = cb.createQuery(LogEntry.class);
        Root<LogEntry> root = cq.from(LogEntry.class);
        List<Predicate> predicates = buildStructuredPredicates(cb, root, request, teamId);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("timestamp")));

        TypedQuery<LogEntry> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<LogEntry> results = typedQuery.getResultList();

        // Build count query
        long totalElements = executeCountQuery(cb, predicates, teamId, request);

        long durationMs = System.currentTimeMillis() - startMs;
        List<LogEntryResponse> content = logEntryMapper.toResponseList(results);

        // Record history
        recordQueryHistory(serializeQuery(request), null, totalElements, durationMs, teamId, userId);

        int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isLast = (page + 1) >= totalPages;

        return new PageResponse<>(content, page, size, totalElements, totalPages, isLast);
    }

    // ==================== Full-Text Search ====================

    /**
     * Performs full-text search across message, loggerName, exceptionClass,
     * exceptionMessage, and customFields using case-insensitive LIKE matching.
     *
     * @param searchTerm the text to search for
     * @param teamId     the team scope
     * @param startTime  optional time range start
     * @param endTime    optional time range end
     * @param page       page number
     * @param size       page size
     * @return paginated results
     */
    public PageResponse<LogEntryResponse> search(String searchTerm, UUID teamId,
                                                  Instant startTime, Instant endTime,
                                                  int page, int size) {
        if (searchTerm == null || searchTerm.isBlank()) {
            throw new ValidationException("Search term cannot be empty");
        }

        long startMs = System.currentTimeMillis();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Data query
        CriteriaQuery<LogEntry> cq = cb.createQuery(LogEntry.class);
        Root<LogEntry> root = cq.from(LogEntry.class);
        List<Predicate> predicates = buildSearchPredicates(cb, root, searchTerm, teamId, startTime, endTime);
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("timestamp")));

        TypedQuery<LogEntry> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<LogEntry> results = typedQuery.getResultList();

        // Count query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<LogEntry> countRoot = countCq.from(LogEntry.class);
        List<Predicate> countPredicates = buildSearchPredicates(cb, countRoot, searchTerm, teamId, startTime, endTime);
        countCq.select(cb.count(countRoot));
        countCq.where(countPredicates.toArray(new Predicate[0]));
        long totalElements = entityManager.createQuery(countCq).getSingleResult();

        long durationMs = System.currentTimeMillis() - startMs;
        List<LogEntryResponse> content = logEntryMapper.toResponseList(results);

        int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isLast = (page + 1) >= totalPages;

        return new PageResponse<>(content, page, size, totalElements, totalPages, isLast);
    }

    // ==================== DSL Query ====================

    /**
     * Parses and executes a SQL-like DSL query string.
     *
     * @param dslQuery the DSL query string
     * @param teamId   the team scope
     * @param userId   the user executing the query (for history)
     * @param page     page number
     * @param size     page size
     * @return paginated results
     */
    public PageResponse<LogEntryResponse> executeDsl(String dslQuery, UUID teamId,
                                                      UUID userId, int page, int size) {
        long startMs = System.currentTimeMillis();

        List<DslCondition> conditions = dslParser.parse(dslQuery);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Data query
        CriteriaQuery<LogEntry> cq = cb.createQuery(LogEntry.class);
        Root<LogEntry> root = cq.from(LogEntry.class);
        Predicate dslPredicate = buildDslPredicate(cb, root, conditions);
        Predicate teamPredicate = cb.equal(root.get("teamId"), teamId);
        cq.where(cb.and(teamPredicate, dslPredicate));
        cq.orderBy(cb.desc(root.get("timestamp")));

        TypedQuery<LogEntry> typedQuery = entityManager.createQuery(cq);
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);
        List<LogEntry> results = typedQuery.getResultList();

        // Count query
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<LogEntry> countRoot = countCq.from(LogEntry.class);
        Predicate countDslPredicate = buildDslPredicate(cb, countRoot, conditions);
        Predicate countTeamPredicate = cb.equal(countRoot.get("teamId"), teamId);
        countCq.select(cb.count(countRoot));
        countCq.where(cb.and(countTeamPredicate, countDslPredicate));
        long totalElements = entityManager.createQuery(countCq).getSingleResult();

        long durationMs = System.currentTimeMillis() - startMs;
        List<LogEntryResponse> content = logEntryMapper.toResponseList(results);

        // Record history
        recordQueryHistory(null, dslQuery, totalElements, durationMs, teamId, userId);

        int totalPages = (size > 0) ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isLast = (page + 1) >= totalPages;

        return new PageResponse<>(content, page, size, totalElements, totalPages, isLast);
    }

    // ==================== Saved Queries ====================

    /**
     * Saves a query for later reuse.
     *
     * @param request the saved query request
     * @param teamId  the team scope
     * @param userId  the user saving the query
     * @return the saved query response
     */
    public SavedQueryResponse saveQuery(CreateSavedQueryRequest request, UUID teamId, UUID userId) {
        SavedQuery entity = savedQueryMapper.toEntity(request);
        entity.setTeamId(teamId);
        entity.setCreatedBy(userId);
        if (entity.getIsShared() == null) {
            entity.setIsShared(false);
        }
        if (entity.getExecutionCount() == null) {
            entity.setExecutionCount(0L);
        }
        SavedQuery saved = savedQueryRepository.save(entity);
        log.info("Saved query '{}' for team {} by user {}", request.name(), teamId, userId);
        return savedQueryMapper.toResponse(saved);
    }

    /**
     * Returns all saved queries visible to the user (own + shared team queries).
     *
     * @param teamId the team scope
     * @param userId the current user
     * @return list of saved queries
     */
    public List<SavedQueryResponse> getSavedQueries(UUID teamId, UUID userId) {
        List<SavedQuery> shared = savedQueryRepository.findByTeamIdAndIsSharedTrue(teamId);
        List<SavedQuery> own = savedQueryRepository.findByCreatedBy(userId);

        Set<UUID> seen = new HashSet<>();
        List<SavedQuery> merged = new ArrayList<>();
        for (SavedQuery sq : own) {
            if (seen.add(sq.getId())) {
                merged.add(sq);
            }
        }
        for (SavedQuery sq : shared) {
            if (seen.add(sq.getId())) {
                merged.add(sq);
            }
        }

        return savedQueryMapper.toResponseList(merged);
    }

    /**
     * Returns a single saved query by ID.
     *
     * @param queryId the saved query ID
     * @return the saved query response
     * @throws NotFoundException if not found
     */
    public SavedQueryResponse getSavedQuery(UUID queryId) {
        SavedQuery entity = savedQueryRepository.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Saved query not found: " + queryId));
        return savedQueryMapper.toResponse(entity);
    }

    /**
     * Updates a saved query. Only the owner may update.
     *
     * @param queryId the saved query ID
     * @param request the update request
     * @param userId  the current user
     * @return the updated saved query response
     * @throws NotFoundException      if not found
     * @throws AuthorizationException if user is not the owner
     */
    public SavedQueryResponse updateSavedQuery(UUID queryId, UpdateSavedQueryRequest request, UUID userId) {
        SavedQuery entity = savedQueryRepository.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Saved query not found: " + queryId));

        if (!entity.getCreatedBy().equals(userId)) {
            throw new AuthorizationException("Only the query owner can update this saved query");
        }

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.queryJson() != null) {
            entity.setQueryJson(request.queryJson());
        }
        if (request.queryDsl() != null) {
            entity.setQueryDsl(request.queryDsl());
        }
        if (request.isShared() != null) {
            entity.setIsShared(request.isShared());
        }

        SavedQuery saved = savedQueryRepository.save(entity);
        return savedQueryMapper.toResponse(saved);
    }

    /**
     * Deletes a saved query. Only the owner may delete.
     *
     * @param queryId the saved query ID
     * @param userId  the current user
     * @throws NotFoundException      if not found
     * @throws AuthorizationException if user is not the owner
     */
    public void deleteSavedQuery(UUID queryId, UUID userId) {
        SavedQuery entity = savedQueryRepository.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Saved query not found: " + queryId));

        if (!entity.getCreatedBy().equals(userId)) {
            throw new AuthorizationException("Only the query owner can delete this saved query");
        }

        savedQueryRepository.delete(entity);
        log.info("Deleted saved query '{}' ({})", entity.getName(), queryId);
    }

    /**
     * Executes a previously saved query, updating its execution stats.
     *
     * @param queryId the saved query ID
     * @param teamId  the team scope
     * @param userId  the user executing the query
     * @param page    page number
     * @param size    page size
     * @return paginated results
     */
    public PageResponse<LogEntryResponse> executeSavedQuery(UUID queryId, UUID teamId,
                                                             UUID userId, int page, int size) {
        SavedQuery savedQuery = savedQueryRepository.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Saved query not found: " + queryId));

        PageResponse<LogEntryResponse> result;

        if (savedQuery.getQueryDsl() != null && !savedQuery.getQueryDsl().isBlank()) {
            result = executeDsl(savedQuery.getQueryDsl(), teamId, userId, page, size);
        } else {
            LogQueryRequest queryRequest = deserializeQuery(savedQuery.getQueryJson());
            result = query(queryRequest, teamId, userId);
        }

        savedQuery.setLastExecutedAt(Instant.now());
        savedQuery.setExecutionCount(savedQuery.getExecutionCount() + 1);
        savedQueryRepository.save(savedQuery);

        return result;
    }

    // ==================== Query History ====================

    /**
     * Returns the user's query history, most recent first.
     *
     * @param userId the user
     * @param page   page number
     * @param size   page size
     * @return paginated query history
     */
    public PageResponse<QueryHistoryResponse> getQueryHistory(UUID userId, int page, int size) {
        var springPage = queryHistoryRepository.findByCreatedByOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        List<QueryHistoryResponse> content = queryHistoryMapper.toResponseList(springPage.getContent());
        return new PageResponse<>(
                content,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }

    /**
     * Records a query execution in history.
     *
     * @param queryJson       the serialized query parameters (may be null for DSL queries)
     * @param queryDsl        the DSL string (may be null for structured queries)
     * @param resultCount     the number of results returned
     * @param executionTimeMs the query execution time in milliseconds
     * @param teamId          the team scope
     * @param userId          the user who executed the query
     */
    void recordQueryHistory(String queryJson, String queryDsl, long resultCount,
                            long executionTimeMs, UUID teamId, UUID userId) {
        QueryHistory history = new QueryHistory();
        history.setQueryJson(queryJson != null ? queryJson : "{}");
        history.setQueryDsl(queryDsl);
        history.setResultCount(resultCount);
        history.setExecutionTimeMs(executionTimeMs);
        history.setTeamId(teamId);
        history.setCreatedBy(userId);
        queryHistoryRepository.save(history);
    }

    // ==================== Internal Helpers ====================

    /**
     * Builds JPA Criteria predicates from structured query parameters.
     */
    private List<Predicate> buildStructuredPredicates(CriteriaBuilder cb, Root<LogEntry> root,
                                                       LogQueryRequest request, UUID teamId) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("teamId"), teamId));

        if (request.serviceName() != null && !request.serviceName().isBlank()) {
            predicates.add(cb.equal(root.get("serviceName"), request.serviceName()));
        }
        if (request.level() != null && !request.level().isBlank()) {
            try {
                LogLevel level = LogLevel.valueOf(request.level().toUpperCase());
                predicates.add(cb.equal(root.get("level"), level));
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid log level: " + request.level());
            }
        }
        if (request.startTime() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), request.startTime()));
        }
        if (request.endTime() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), request.endTime()));
        }
        if (request.correlationId() != null && !request.correlationId().isBlank()) {
            predicates.add(cb.equal(root.get("correlationId"), request.correlationId()));
        }
        if (request.loggerName() != null && !request.loggerName().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("loggerName")),
                    "%" + request.loggerName().toLowerCase() + "%"));
        }
        if (request.exceptionClass() != null && !request.exceptionClass().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("exceptionClass")),
                    "%" + request.exceptionClass().toLowerCase() + "%"));
        }
        if (request.hostName() != null && !request.hostName().isBlank()) {
            predicates.add(cb.equal(root.get("hostName"), request.hostName()));
        }
        if (request.query() != null && !request.query().isBlank()) {
            String pattern = "%" + request.query().toLowerCase() + "%";
            Predicate msgLike = cb.like(cb.lower(root.get("message")), pattern);
            Predicate loggerLike = cb.like(cb.lower(root.get("loggerName")), pattern);
            Predicate excClassLike = cb.like(cb.lower(root.get("exceptionClass")), pattern);
            Predicate excMsgLike = cb.like(cb.lower(root.get("exceptionMessage")), pattern);
            Predicate customLike = cb.like(cb.lower(root.get("customFields")), pattern);
            predicates.add(cb.or(msgLike, loggerLike, excClassLike, excMsgLike, customLike));
        }

        return predicates;
    }

    /**
     * Builds JPA Criteria predicates for full-text search.
     */
    private List<Predicate> buildSearchPredicates(CriteriaBuilder cb, Root<LogEntry> root,
                                                    String searchTerm, UUID teamId,
                                                    Instant startTime, Instant endTime) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("teamId"), teamId));

        String pattern = "%" + searchTerm.toLowerCase() + "%";
        Predicate msgLike = cb.like(cb.lower(root.get("message")), pattern);
        Predicate loggerLike = cb.like(cb.lower(root.get("loggerName")), pattern);
        Predicate excClassLike = cb.like(cb.lower(root.get("exceptionClass")), pattern);
        Predicate excMsgLike = cb.like(cb.lower(root.get("exceptionMessage")), pattern);
        Predicate customLike = cb.like(cb.lower(root.get("customFields")), pattern);
        predicates.add(cb.or(msgLike, loggerLike, excClassLike, excMsgLike, customLike));

        if (startTime != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
        }
        if (endTime != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
        }

        return predicates;
    }

    /**
     * Builds a combined JPA predicate from parsed DSL conditions.
     */
    private Predicate buildDslPredicate(CriteriaBuilder cb, Root<LogEntry> root,
                                         List<DslCondition> conditions) {
        Predicate combined = null;

        for (DslCondition condition : conditions) {
            Predicate pred = buildSingleDslPredicate(cb, root, condition);

            if (combined == null) {
                combined = pred;
            } else if ("OR".equals(condition.conjunction())) {
                combined = cb.or(combined, pred);
            } else {
                combined = cb.and(combined, pred);
            }
        }

        return combined != null ? combined : cb.conjunction();
    }

    /**
     * Builds a single JPA predicate from one DSL condition.
     */
    @SuppressWarnings("unchecked")
    private Predicate buildSingleDslPredicate(CriteriaBuilder cb, Root<LogEntry> root,
                                               DslCondition condition) {
        String entityField = dslParser.mapFieldToEntityField(condition.field());
        String operator = condition.operator();
        String value = condition.value();

        // Handle level field specially — compare as enum
        if ("level".equals(entityField)) {
            return buildLevelPredicate(cb, root, operator, value);
        }

        // Handle timestamp field specially — compare as Instant
        if ("timestamp".equals(entityField)) {
            return buildTimestampPredicate(cb, root, operator, value);
        }

        // String field comparisons
        Path<String> path = root.get(entityField);
        return switch (operator) {
            case "=" -> cb.equal(path, value);
            case "!=" -> cb.notEqual(path, value);
            case ">" -> cb.greaterThan(path, value);
            case "<" -> cb.lessThan(path, value);
            case ">=" -> cb.greaterThanOrEqualTo(path, value);
            case "<=" -> cb.lessThanOrEqualTo(path, value);
            case "CONTAINS", "LIKE" -> cb.like(cb.lower(path), "%" + value.toLowerCase() + "%");
            case "NOT CONTAINS", "NOT LIKE" -> cb.notLike(cb.lower(path), "%" + value.toLowerCase() + "%");
            case "IN" -> {
                String[] values = value.split("\\s*,\\s*");
                yield path.in(Arrays.asList(values));
            }
            case "NOT IN" -> {
                String[] values = value.split("\\s*,\\s*");
                yield cb.not(path.in(Arrays.asList(values)));
            }
            default -> throw new ValidationException("Unsupported operator: " + operator);
        };
    }

    /**
     * Builds a predicate for level comparisons using LogLevel ordinal for ordering.
     */
    private Predicate buildLevelPredicate(CriteriaBuilder cb, Root<LogEntry> root,
                                           String operator, String value) {
        Path<LogLevel> levelPath = root.get("level");

        if ("IN".equals(operator)) {
            String[] values = value.split("\\s*,\\s*");
            List<LogLevel> levels = Arrays.stream(values)
                    .map(v -> parseLogLevel(v.trim()))
                    .collect(Collectors.toList());
            return levelPath.in(levels);
        }
        if ("NOT IN".equals(operator)) {
            String[] values = value.split("\\s*,\\s*");
            List<LogLevel> levels = Arrays.stream(values)
                    .map(v -> parseLogLevel(v.trim()))
                    .collect(Collectors.toList());
            return cb.not(levelPath.in(levels));
        }

        LogLevel targetLevel = parseLogLevel(value);
        return switch (operator) {
            case "=" -> cb.equal(levelPath, targetLevel);
            case "!=" -> cb.notEqual(levelPath, targetLevel);
            case ">=" -> cb.greaterThanOrEqualTo(levelPath, targetLevel);
            case "<=" -> cb.lessThanOrEqualTo(levelPath, targetLevel);
            case ">" -> cb.greaterThan(levelPath, targetLevel);
            case "<" -> cb.lessThan(levelPath, targetLevel);
            default -> throw new ValidationException("Operator '" + operator + "' not supported for level field");
        };
    }

    /**
     * Builds a predicate for timestamp comparisons.
     */
    private Predicate buildTimestampPredicate(CriteriaBuilder cb, Root<LogEntry> root,
                                               String operator, String value) {
        Instant instant = Instant.parse(value);
        Path<Instant> tsPath = root.get("timestamp");
        return switch (operator) {
            case "=" -> cb.equal(tsPath, instant);
            case "!=" -> cb.notEqual(tsPath, instant);
            case ">" -> cb.greaterThan(tsPath, instant);
            case "<" -> cb.lessThan(tsPath, instant);
            case ">=" -> cb.greaterThanOrEqualTo(tsPath, instant);
            case "<=" -> cb.lessThanOrEqualTo(tsPath, instant);
            default -> throw new ValidationException("Operator '" + operator + "' not supported for timestamp field");
        };
    }

    /**
     * Parses a log level string, throwing ValidationException on failure.
     */
    private LogLevel parseLogLevel(String value) {
        try {
            return LogLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid log level: " + value);
        }
    }

    /**
     * Executes a count query using the same predicates as the data query.
     */
    private long executeCountQuery(CriteriaBuilder cb, List<Predicate> dataPredicates,
                                    UUID teamId, LogQueryRequest request) {
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<LogEntry> countRoot = countCq.from(LogEntry.class);
        List<Predicate> countPredicates = buildStructuredPredicates(cb, countRoot, request, teamId);
        countCq.select(cb.count(countRoot));
        countCq.where(countPredicates.toArray(new Predicate[0]));
        return entityManager.createQuery(countCq).getSingleResult();
    }

    /**
     * Serializes a LogQueryRequest to JSON for storage in query history.
     */
    private String serializeQuery(LogQueryRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize query request: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Deserializes a JSON string back to a LogQueryRequest.
     */
    private LogQueryRequest deserializeQuery(String json) {
        try {
            return objectMapper.readValue(json, LogQueryRequest.class);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Failed to deserialize saved query: " + e.getMessage());
        }
    }
}
