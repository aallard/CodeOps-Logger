package com.codeops.logger.service;

import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.TrapCondition;
import com.codeops.logger.entity.enums.ConditionType;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.repository.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TrapEvaluationEngine}.
 */
@ExtendWith(MockitoExtension.class)
class TrapEvaluationEngineTest {

    @Mock
    private LogEntryRepository logEntryRepository;

    private TrapEvaluationEngine engine;

    private static final UUID TEAM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        engine = new TrapEvaluationEngine(logEntryRepository);
    }

    private LogEntry createEntry(String message, String serviceName, LogLevel level) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID());
        entry.setMessage(message);
        entry.setServiceName(serviceName);
        entry.setLevel(level);
        entry.setTimestamp(Instant.now());
        entry.setTeamId(TEAM_ID);
        return entry;
    }

    private TrapCondition createRegexCondition(String field, String pattern) {
        TrapCondition condition = new TrapCondition();
        condition.setConditionType(ConditionType.REGEX);
        condition.setField(field);
        condition.setPattern(pattern);
        return condition;
    }

    private TrapCondition createKeywordCondition(String field, String keyword) {
        TrapCondition condition = new TrapCondition();
        condition.setConditionType(ConditionType.KEYWORD);
        condition.setField(field);
        condition.setPattern(keyword);
        return condition;
    }

    @Test
    void testEvaluateRegex_matches() {
        LogEntry entry = createEntry("timeout establishing connection to db", "svc", LogLevel.ERROR);
        TrapCondition condition = createRegexCondition("message", "timeout.*connection");

        boolean result = engine.evaluateRegex(entry, condition);

        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateRegex_noMatch() {
        LogEntry entry = createEntry("Application started successfully", "svc", LogLevel.INFO);
        TrapCondition condition = createRegexCondition("message", "timeout.*connection");

        boolean result = engine.evaluateRegex(entry, condition);

        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateRegex_invalidPattern_returnsFalse() {
        LogEntry entry = createEntry("some message", "svc", LogLevel.INFO);
        TrapCondition condition = createRegexCondition("message", "[invalid");

        boolean result = engine.evaluateRegex(entry, condition);

        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateRegex_nullFieldValue_returnsFalse() {
        LogEntry entry = createEntry("msg", "svc", LogLevel.INFO);
        // loggerName is null
        TrapCondition condition = createRegexCondition("loggerName", "anything");

        boolean result = engine.evaluateRegex(entry, condition);

        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateKeyword_matches_caseInsensitive() {
        LogEntry entry = createEntry("An error occurred in processing", "svc", LogLevel.ERROR);
        TrapCondition condition = createKeywordCondition("message", "ERROR");

        boolean result = engine.evaluateKeyword(entry, condition);

        assertThat(result).isTrue();
    }

    @Test
    void testEvaluateKeyword_noMatch() {
        LogEntry entry = createEntry("Application healthy", "svc", LogLevel.INFO);
        TrapCondition condition = createKeywordCondition("message", "error");

        boolean result = engine.evaluateKeyword(entry, condition);

        assertThat(result).isFalse();
    }

    @Test
    void testEvaluateKeyword_nullFieldValue_returnsFalse() {
        LogEntry entry = createEntry("msg", "svc", LogLevel.INFO);
        TrapCondition condition = createKeywordCondition("loggerName", "keyword");

        boolean result = engine.evaluateKeyword(entry, condition);

        assertThat(result).isFalse();
    }

    @Test
    void testEvaluatePatternConditions_allMatch_returnsTrue() {
        LogEntry entry = createEntry("timeout connecting to database", "codeops-server", LogLevel.ERROR);
        TrapCondition regex = createRegexCondition("message", "timeout.*database");
        TrapCondition keyword = createKeywordCondition("message", "connecting");

        boolean result = engine.evaluatePatternConditions(entry, List.of(regex, keyword));

        assertThat(result).isTrue();
    }

    @Test
    void testEvaluatePatternConditions_oneFailure_returnsFalse() {
        LogEntry entry = createEntry("timeout connecting to database", "svc", LogLevel.ERROR);
        TrapCondition match = createKeywordCondition("message", "timeout");
        TrapCondition noMatch = createKeywordCondition("message", "nonexistent");

        boolean result = engine.evaluatePatternConditions(entry, List.of(match, noMatch));

        assertThat(result).isFalse();
    }

    @Test
    void testEvaluatePatternConditions_emptyConditions_returnsTrue() {
        LogEntry entry = createEntry("any message", "svc", LogLevel.INFO);

        boolean result = engine.evaluatePatternConditions(entry, List.of());

        assertThat(result).isTrue();
    }

    @Test
    void testEvaluatePatternConditions_serviceNameFilter_match() {
        LogEntry entry = createEntry("error msg", "target-service", LogLevel.ERROR);
        TrapCondition condition = createKeywordCondition("message", "error");
        condition.setServiceName("target-service");

        boolean result = engine.evaluatePatternConditions(entry, List.of(condition));

        assertThat(result).isTrue();
    }

    @Test
    void testEvaluatePatternConditions_serviceNameFilter_noMatch_returnsFalse() {
        LogEntry entry = createEntry("error msg", "other-service", LogLevel.ERROR);
        TrapCondition condition = createKeywordCondition("message", "error");
        condition.setServiceName("target-service");

        boolean result = engine.evaluatePatternConditions(entry, List.of(condition));

        assertThat(result).isFalse();
    }

    @Test
    void testEvaluatePatternConditions_logLevelFilter_atLevel_returnsTrue() {
        LogEntry entry = createEntry("msg", "svc", LogLevel.ERROR);
        TrapCondition condition = createKeywordCondition("message", "msg");
        condition.setLogLevel(LogLevel.WARN);

        boolean result = engine.evaluatePatternConditions(entry, List.of(condition));

        assertThat(result).isTrue();
    }

    @Test
    void testEvaluatePatternConditions_logLevelFilter_belowLevel_returnsFalse() {
        LogEntry entry = createEntry("msg", "svc", LogLevel.DEBUG);
        TrapCondition condition = createKeywordCondition("message", "msg");
        condition.setLogLevel(LogLevel.WARN);

        boolean result = engine.evaluatePatternConditions(entry, List.of(condition));

        assertThat(result).isFalse();
    }

    @Test
    void testExtractFieldValue_message() {
        LogEntry entry = createEntry("hello world", "svc", LogLevel.INFO);

        String value = engine.extractFieldValue(entry, "message");

        assertThat(value).isEqualTo("hello world");
    }

    @Test
    void testExtractFieldValue_allFields() {
        LogEntry entry = new LogEntry();
        entry.setMessage("msg");
        entry.setLoggerName("com.codeops.Test");
        entry.setThreadName("http-exec-1");
        entry.setExceptionClass("java.lang.RuntimeException");
        entry.setExceptionMessage("boom");
        entry.setStackTrace("at com.codeops...");
        entry.setServiceName("my-svc");
        entry.setHostName("host-1");
        entry.setIpAddress("10.0.0.1");
        entry.setCorrelationId("corr-123");
        entry.setCustomFields("{\"key\":\"val\"}");
        entry.setLevel(LogLevel.ERROR);

        assertThat(engine.extractFieldValue(entry, "message")).isEqualTo("msg");
        assertThat(engine.extractFieldValue(entry, "loggerName")).isEqualTo("com.codeops.Test");
        assertThat(engine.extractFieldValue(entry, "logger_name")).isEqualTo("com.codeops.Test");
        assertThat(engine.extractFieldValue(entry, "threadName")).isEqualTo("http-exec-1");
        assertThat(engine.extractFieldValue(entry, "exceptionClass")).isEqualTo("java.lang.RuntimeException");
        assertThat(engine.extractFieldValue(entry, "exceptionMessage")).isEqualTo("boom");
        assertThat(engine.extractFieldValue(entry, "stackTrace")).isEqualTo("at com.codeops...");
        assertThat(engine.extractFieldValue(entry, "serviceName")).isEqualTo("my-svc");
        assertThat(engine.extractFieldValue(entry, "hostName")).isEqualTo("host-1");
        assertThat(engine.extractFieldValue(entry, "ipAddress")).isEqualTo("10.0.0.1");
        assertThat(engine.extractFieldValue(entry, "correlationId")).isEqualTo("corr-123");
        assertThat(engine.extractFieldValue(entry, "customFields")).isEqualTo("{\"key\":\"val\"}");
        assertThat(engine.extractFieldValue(entry, "level")).isEqualTo("ERROR");
        assertThat(engine.extractFieldValue(entry, "unknownField")).isNull();
    }

    @Test
    void testIsLevelAtOrAbove_variousCombinations() {
        assertThat(engine.isLevelAtOrAbove(LogLevel.ERROR, LogLevel.WARN)).isTrue();
        assertThat(engine.isLevelAtOrAbove(LogLevel.WARN, LogLevel.WARN)).isTrue();
        assertThat(engine.isLevelAtOrAbove(LogLevel.INFO, LogLevel.WARN)).isFalse();
        assertThat(engine.isLevelAtOrAbove(LogLevel.FATAL, LogLevel.TRACE)).isTrue();
        assertThat(engine.isLevelAtOrAbove(LogLevel.TRACE, LogLevel.FATAL)).isFalse();
        assertThat(engine.isLevelAtOrAbove(null, LogLevel.WARN)).isFalse();
        assertThat(engine.isLevelAtOrAbove(LogLevel.ERROR, null)).isFalse();
    }
}
