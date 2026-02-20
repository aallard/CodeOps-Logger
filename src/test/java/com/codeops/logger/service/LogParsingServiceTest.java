package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogParsingService}.
 * Uses a real ObjectMapper since parsing depends on Jackson behavior.
 */
class LogParsingServiceTest {

    private LogParsingService parsingService;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        parsingService = new LogParsingService(mapper);
    }

    @Test
    void testParseValidJson_allFields() {
        String json = """
                {"level":"ERROR","message":"Connection failed","timestamp":"2026-01-15T10:30:45.123Z",
                 "logger":"com.codeops.AuthService","thread":"http-exec-5",
                 "service":"codeops-server","correlation_id":"abc-123",
                 "trace_id":"trace-1","span_id":"span-1","host":"dev-machine",
                 "exception":"java.sql.SQLException: timeout","stack_trace":"at com.codeops..."}
                """;
        IngestLogEntryRequest result = parsingService.parse(json, "fallback");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("ERROR");
        assertThat(result.message()).isEqualTo("Connection failed");
        assertThat(result.timestamp()).isNotNull();
        assertThat(result.loggerName()).isEqualTo("com.codeops.AuthService");
        assertThat(result.threadName()).isEqualTo("http-exec-5");
        assertThat(result.serviceName()).isEqualTo("codeops-server");
        assertThat(result.correlationId()).isEqualTo("abc-123");
        assertThat(result.traceId()).isEqualTo("trace-1");
        assertThat(result.spanId()).isEqualTo("span-1");
        assertThat(result.hostName()).isEqualTo("dev-machine");
        assertThat(result.exceptionClass()).isEqualTo("java.sql.SQLException");
        assertThat(result.exceptionMessage()).isEqualTo("timeout");
        assertThat(result.stackTrace()).isEqualTo("at com.codeops...");
    }

    @Test
    void testParseValidJson_logstashFormat() {
        String json = """
                {"severity":"WARN","@message":"Slow query","@timestamp":"2026-01-15T10:30:45Z",
                 "service.name":"my-service"}
                """;
        IngestLogEntryRequest result = parsingService.parse(json, "default-svc");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("WARN");
        assertThat(result.message()).isEqualTo("Slow query");
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    void testParseValidJson_minimalFields() {
        String json = """
                {"level":"INFO","message":"Startup complete"}
                """;
        IngestLogEntryRequest result = parsingService.parse(json, "my-app");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
        assertThat(result.message()).isEqualTo("Startup complete");
        assertThat(result.serviceName()).isEqualTo("my-app");
    }

    @Test
    void testParseValidJson_extraFields_goToCustomFields() {
        String json = """
                {"level":"DEBUG","message":"test","custom_key":"custom_value","another":"val2"}
                """;
        IngestLogEntryRequest result = parsingService.parse(json, "svc");

        assertThat(result).isNotNull();
        assertThat(result.customFields()).isNotNull();
        assertThat(result.customFields()).contains("custom_key");
        assertThat(result.customFields()).contains("custom_value");
    }

    @Test
    void testParseInvalidJson_fallsThrough() {
        IngestLogEntryRequest result = parsingService.tryParseJson("{invalid json!!!");
        assertThat(result).isNull();
    }

    @Test
    void testParseKeyValue_standardFormat() {
        String kv = "level=INFO message=hello service=myapp";
        IngestLogEntryRequest result = parsingService.parse(kv, "default");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
        assertThat(result.message()).isEqualTo("hello");
        assertThat(result.serviceName()).isEqualTo("myapp");
    }

    @Test
    void testParseKeyValue_quotedValues() {
        String kv = "message=\"hello world\" level=ERROR service=test-svc";
        IngestLogEntryRequest result = parsingService.parse(kv, "default");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("ERROR");
        assertThat(result.message()).isEqualTo("hello world");
    }

    @Test
    void testParseKeyValue_tooFewPairs_returnsNull() {
        IngestLogEntryRequest result = parsingService.tryParseKeyValue("just_plain_text");
        assertThat(result).isNull();
    }

    @Test
    void testParseSpringBootFormat_withTracing() {
        String log = "2026-01-15 10:30:45.123  INFO [my-service,trace-abc,span-def] 12345 --- [http-exec-1] com.codeops.Main : Application started";
        IngestLogEntryRequest result = parsingService.parse(log, "default");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
        assertThat(result.serviceName()).isEqualTo("my-service");
        assertThat(result.traceId()).isEqualTo("trace-abc");
        assertThat(result.spanId()).isEqualTo("span-def");
        assertThat(result.threadName()).isEqualTo("http-exec-1");
        assertThat(result.loggerName()).isEqualTo("com.codeops.Main");
        assertThat(result.message()).isEqualTo("Application started");
    }

    @Test
    void testParseSpringBootFormat_withoutTracing() {
        String log = "2026-01-15 10:30:45.123  ERROR 12345 --- [main] com.codeops.Boot : Failed to start";
        IngestLogEntryRequest result = parsingService.parse(log, "default-svc");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("ERROR");
        assertThat(result.threadName()).isEqualTo("main");
        assertThat(result.loggerName()).isEqualTo("com.codeops.Boot");
        assertThat(result.message()).isEqualTo("Failed to start");
    }

    @Test
    void testParseSyslogFormat_rfc3164() {
        String syslog = "<14>Jan 15 10:30:45 webserver nginx[1234]: GET /api/health 200";
        IngestLogEntryRequest result = parsingService.parse(syslog, "default");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
        assertThat(result.serviceName()).isEqualTo("nginx");
        assertThat(result.hostName()).isEqualTo("webserver");
        assertThat(result.message()).isEqualTo("GET /api/health 200");
    }

    @Test
    void testFallbackPlainText() {
        String plain = "Some random log text that matches no pattern";
        IngestLogEntryRequest result = parsingService.parse(plain, "my-service");

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
        assertThat(result.message()).isEqualTo(plain);
        assertThat(result.serviceName()).isEqualTo("my-service");
    }

    @Test
    void testFallbackPlainText_truncatesLongMessage() {
        String longMsg = "A".repeat(AppConstants.MAX_LOG_MESSAGE_LENGTH + 100);
        IngestLogEntryRequest result = parsingService.parse(longMsg, "svc");

        assertThat(result).isNotNull();
        assertThat(result.message()).hasSize(AppConstants.MAX_LOG_MESSAGE_LENGTH);
    }

    @Test
    void testParse_triesAllParsers_inOrder() {
        // JSON should be tried first and succeed
        String json = """
                {"level":"WARN","message":"JSON wins"}
                """;
        IngestLogEntryRequest result = parsingService.parse(json, "svc");
        assertThat(result.message()).isEqualTo("JSON wins");
        assertThat(result.level()).isEqualTo("WARN");
    }

    @Test
    void testParse_nullInput_returnsPlainText() {
        IngestLogEntryRequest result = parsingService.parse(null, "default-svc");
        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
        assertThat(result.serviceName()).isEqualTo("default-svc");
    }

    @Test
    void testParse_blankInput_returnsPlainText() {
        IngestLogEntryRequest result = parsingService.parse("   ", "default-svc");
        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
    }

    @Test
    void testTimestampParsing_iso8601() {
        Instant result = parsingService.parseTimestamp("2026-01-15T10:30:45.123Z");
        assertThat(result).isNotNull();
        assertThat(result.toString()).startsWith("2026-01-15T10:30:45");
    }

    @Test
    void testTimestampParsing_epochMillis() {
        Instant result = parsingService.parseTimestamp("1705312245123");
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Instant.ofEpochMilli(1705312245123L));
    }

    @Test
    void testTimestampParsing_epochSeconds() {
        Instant result = parsingService.parseTimestamp("1705312245");
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Instant.ofEpochSecond(1705312245L));
    }
}
