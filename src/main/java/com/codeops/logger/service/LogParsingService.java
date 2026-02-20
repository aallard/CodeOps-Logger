package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw log strings into structured log entry requests.
 * Supports JSON, key-value pairs, and common log formats including
 * Spring Boot default, syslog, and plain text fallback.
 */
@Service
@Slf4j
public class LogParsingService {

    private final ObjectMapper objectMapper;

    private static final Pattern SPRING_BOOT_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}\\.?\\d*)\\s+"
                    + "(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s+"
                    + "(?:\\[([^]]*)]\\s+)?"
                    + "(\\d+)\\s+---\\s+\\[\\s*([^]]+)]\\s+"
                    + "(\\S+)\\s*:\\s*(.*)",
            Pattern.DOTALL
    );

    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
            "^(?:<(\\d{1,3})>)?"
                    + "(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+"
                    + "(\\S+)\\s+"
                    + "(\\S+?)(?:\\[(\\d+)])?:\\s*(.*)",
            Pattern.DOTALL
    );

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
            "(\\w+)=(\"[^\"]*\"|\\S+)"
    );

    private static final Set<String> LEVEL_FIELDS = Set.of("level", "severity", "log.level");
    private static final Set<String> MESSAGE_FIELDS = Set.of("message", "msg", "@message");
    private static final Set<String> TIMESTAMP_FIELDS = Set.of("timestamp", "@timestamp", "time");
    private static final Set<String> LOGGER_FIELDS = Set.of("logger", "logger_name", "loggername");
    private static final Set<String> THREAD_FIELDS = Set.of("thread", "thread_name", "threadname");
    private static final Set<String> SERVICE_FIELDS = Set.of("service", "service.name", "servicename");
    private static final Set<String> CORRELATION_FIELDS = Set.of("correlation_id", "correlationid", "x-correlation-id");
    private static final Set<String> TRACE_FIELDS = Set.of("trace_id", "traceid");
    private static final Set<String> SPAN_FIELDS = Set.of("span_id", "spanid");
    private static final Set<String> HOST_FIELDS = Set.of("host", "hostname");
    private static final Set<String> EXCEPTION_FIELDS = Set.of("exception", "error");
    private static final Set<String> STACK_TRACE_FIELDS = Set.of("stack_trace", "stacktrace", "exception.stacktrace");

    /**
     * Creates a new LogParsingService.
     *
     * @param objectMapper the Jackson ObjectMapper for JSON parsing
     */
    public LogParsingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Attempts to parse a raw log string into a structured request.
     * Tries JSON first, then key-value, then common patterns, then falls back to plain text.
     *
     * @param rawLog             the raw log string to parse
     * @param defaultServiceName service name to use if not extractable from the log
     * @return a structured IngestLogEntryRequest
     */
    public IngestLogEntryRequest parse(String rawLog, String defaultServiceName) {
        if (rawLog == null || rawLog.isBlank()) {
            return fallbackPlainText("", defaultServiceName);
        }

        String trimmed = rawLog.trim();

        IngestLogEntryRequest result = tryParseJson(trimmed);
        if (result != null) return applyDefaults(result, defaultServiceName);

        result = tryParseKeyValue(trimmed);
        if (result != null) return applyDefaults(result, defaultServiceName);

        result = tryParseSpringBoot(trimmed);
        if (result != null) return applyDefaults(result, defaultServiceName);

        result = tryParseSyslog(trimmed);
        if (result != null) return applyDefaults(result, defaultServiceName);

        return fallbackPlainText(trimmed, defaultServiceName);
    }

    /**
     * Attempts to parse the raw string as a JSON log entry.
     *
     * @param rawLog the raw string
     * @return parsed request, or null if not valid JSON
     */
    IngestLogEntryRequest tryParseJson(String rawLog) {
        if (!rawLog.startsWith("{")) return null;

        try {
            JsonNode root = objectMapper.readTree(rawLog);
            if (!root.isObject()) return null;

            String level = extractJsonField(root, LEVEL_FIELDS);
            String message = extractJsonField(root, MESSAGE_FIELDS);
            String timestampStr = extractJsonField(root, TIMESTAMP_FIELDS);
            String loggerName = extractJsonField(root, LOGGER_FIELDS);
            String threadName = extractJsonField(root, THREAD_FIELDS);
            String serviceName = extractJsonField(root, SERVICE_FIELDS);
            String correlationId = extractJsonField(root, CORRELATION_FIELDS);
            String traceId = extractJsonField(root, TRACE_FIELDS);
            String spanId = extractJsonField(root, SPAN_FIELDS);
            String hostName = extractJsonField(root, HOST_FIELDS);
            String exceptionInfo = extractJsonField(root, EXCEPTION_FIELDS);
            String stackTrace = extractJsonField(root, STACK_TRACE_FIELDS);

            if (level == null && message == null) return null;
            if (level == null) level = "INFO";
            if (message == null) message = rawLog;

            String exceptionClass = null;
            String exceptionMessage = null;
            if (exceptionInfo != null) {
                int colonIdx = exceptionInfo.indexOf(':');
                if (colonIdx > 0) {
                    exceptionClass = exceptionInfo.substring(0, colonIdx).trim();
                    exceptionMessage = exceptionInfo.substring(colonIdx + 1).trim();
                } else {
                    exceptionMessage = exceptionInfo;
                }
            }

            Set<String> knownFields = new HashSet<>();
            LEVEL_FIELDS.forEach(knownFields::add);
            MESSAGE_FIELDS.forEach(knownFields::add);
            TIMESTAMP_FIELDS.forEach(knownFields::add);
            LOGGER_FIELDS.forEach(knownFields::add);
            THREAD_FIELDS.forEach(knownFields::add);
            SERVICE_FIELDS.forEach(knownFields::add);
            CORRELATION_FIELDS.forEach(knownFields::add);
            TRACE_FIELDS.forEach(knownFields::add);
            SPAN_FIELDS.forEach(knownFields::add);
            HOST_FIELDS.forEach(knownFields::add);
            EXCEPTION_FIELDS.forEach(knownFields::add);
            STACK_TRACE_FIELDS.forEach(knownFields::add);

            Map<String, String> extras = new LinkedHashMap<>();
            root.fieldNames().forEachRemaining(field -> {
                if (!knownFields.contains(field.toLowerCase())) {
                    JsonNode val = root.get(field);
                    extras.put(field, val.isTextual() ? val.asText() : val.toString());
                }
            });

            String customFields = extras.isEmpty() ? null : objectMapper.writeValueAsString(extras);

            return new IngestLogEntryRequest(
                    level, truncateMessage(message), parseTimestamp(timestampStr),
                    serviceName, correlationId, traceId, spanId,
                    loggerName, threadName, exceptionClass, exceptionMessage,
                    stackTrace, customFields, hostName, null
            );
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse as JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Attempts to parse the raw string as key-value pairs.
     *
     * @param rawLog the raw string
     * @return parsed request, or null if not key-value format
     */
    IngestLogEntryRequest tryParseKeyValue(String rawLog) {
        Matcher matcher = KEY_VALUE_PATTERN.matcher(rawLog);
        Map<String, String> pairs = new LinkedHashMap<>();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value = matcher.group(2);
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            pairs.put(key, value);
        }

        if (pairs.size() < 2) return null;

        String level = getFromAnyKey(pairs, LEVEL_FIELDS);
        String message = getFromAnyKey(pairs, MESSAGE_FIELDS);
        String timestampStr = getFromAnyKey(pairs, TIMESTAMP_FIELDS);
        String serviceName = getFromAnyKey(pairs, SERVICE_FIELDS);
        String correlationId = getFromAnyKey(pairs, CORRELATION_FIELDS);
        String traceId = getFromAnyKey(pairs, TRACE_FIELDS);
        String spanId = getFromAnyKey(pairs, SPAN_FIELDS);
        String loggerName = getFromAnyKey(pairs, LOGGER_FIELDS);
        String threadName = getFromAnyKey(pairs, THREAD_FIELDS);
        String hostName = getFromAnyKey(pairs, HOST_FIELDS);

        if (level == null) level = "INFO";
        if (message == null) message = rawLog;

        return new IngestLogEntryRequest(
                level, truncateMessage(message), parseTimestamp(timestampStr),
                serviceName, correlationId, traceId, spanId,
                loggerName, threadName, null, null,
                null, null, hostName, null
        );
    }

    /**
     * Attempts to parse Spring Boot default log format.
     *
     * @param rawLog the raw string
     * @return parsed request, or null if not matching
     */
    IngestLogEntryRequest tryParseSpringBoot(String rawLog) {
        Matcher matcher = SPRING_BOOT_PATTERN.matcher(rawLog);
        if (!matcher.matches()) return null;

        String timestampStr = matcher.group(1);
        String level = matcher.group(2);
        String tracingInfo = matcher.group(3);
        String threadName = matcher.group(5).trim();
        String loggerName = matcher.group(6).trim();
        String message = matcher.group(7).trim();

        String serviceName = null;
        String traceId = null;
        String spanId = null;
        if (tracingInfo != null && !tracingInfo.isBlank()) {
            String[] parts = tracingInfo.split(",");
            if (parts.length >= 1) serviceName = parts[0].trim();
            if (parts.length >= 2) traceId = parts[1].trim();
            if (parts.length >= 3) spanId = parts[2].trim();
        }

        return new IngestLogEntryRequest(
                level, truncateMessage(message), parseTimestamp(timestampStr),
                serviceName, null, traceId, spanId,
                loggerName, threadName, null, null,
                null, null, null, null
        );
    }

    /**
     * Attempts to parse syslog format (RFC 3164 variant).
     *
     * @param rawLog the raw string
     * @return parsed request, or null if not syslog format
     */
    IngestLogEntryRequest tryParseSyslog(String rawLog) {
        Matcher matcher = SYSLOG_PATTERN.matcher(rawLog);
        if (!matcher.matches()) return null;

        String priorityStr = matcher.group(1);
        String hostName = matcher.group(3);
        String appName = matcher.group(4);
        String message = matcher.group(6).trim();

        String level = "INFO";
        if (priorityStr != null) {
            int priority = Integer.parseInt(priorityStr);
            int severity = priority & 0x07;
            level = syslogSeverityToLevel(severity);
        }

        return new IngestLogEntryRequest(
                level, truncateMessage(message), null,
                appName, null, null, null,
                null, null, null, null,
                null, null, hostName, null
        );
    }

    /**
     * Fallback: wraps raw text as a plain INFO log entry.
     *
     * @param rawLog             the raw text
     * @param defaultServiceName the service name to use
     * @return an IngestLogEntryRequest with the raw text as message
     */
    IngestLogEntryRequest fallbackPlainText(String rawLog, String defaultServiceName) {
        String message = rawLog == null || rawLog.isBlank() ? "(empty)" : rawLog;
        return new IngestLogEntryRequest(
                "INFO", truncateMessage(message), null,
                defaultServiceName, null, null, null,
                null, null, null, null,
                null, null, null, null
        );
    }

    private IngestLogEntryRequest applyDefaults(IngestLogEntryRequest request, String defaultServiceName) {
        String serviceName = request.serviceName();
        if (serviceName == null || serviceName.isBlank()) {
            serviceName = defaultServiceName;
        }
        if (serviceName == null || serviceName.isBlank()) {
            serviceName = "unknown";
        }
        if (Objects.equals(serviceName, request.serviceName())) {
            return request;
        }
        return new IngestLogEntryRequest(
                request.level(), request.message(), request.timestamp(),
                serviceName, request.correlationId(), request.traceId(), request.spanId(),
                request.loggerName(), request.threadName(), request.exceptionClass(),
                request.exceptionMessage(), request.stackTrace(), request.customFields(),
                request.hostName(), request.ipAddress()
        );
    }

    private String extractJsonField(JsonNode root, Set<String> fieldNames) {
        for (String field : fieldNames) {
            JsonNode node = root.get(field);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }
        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (fieldNames.contains(name.toLowerCase())) {
                JsonNode node = root.get(name);
                if (node != null && !node.isNull()) {
                    return node.asText();
                }
            }
        }
        return null;
    }

    private String getFromAnyKey(Map<String, String> map, Set<String> keys) {
        for (String key : keys) {
            String val = map.get(key);
            if (val != null) return val;
        }
        return null;
    }

    /**
     * Parses a timestamp string in various formats.
     *
     * @param timestampStr the timestamp string (ISO-8601, epoch millis, epoch seconds)
     * @return the parsed Instant, or null if unparseable
     */
    Instant parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isBlank()) return null;

        try {
            return Instant.parse(timestampStr);
        } catch (DateTimeParseException ignored) {
        }

        try {
            long val = Long.parseLong(timestampStr);
            if (val > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(val);
            } else {
                return Instant.ofEpochSecond(val);
            }
        } catch (NumberFormatException ignored) {
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(timestampStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(timestampStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime ldt = LocalDateTime.parse(timestampStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        log.debug("Unable to parse timestamp: {}", timestampStr);
        return null;
    }

    private String truncateMessage(String message) {
        if (message == null) return null;
        if (message.length() > AppConstants.MAX_LOG_MESSAGE_LENGTH) {
            return message.substring(0, AppConstants.MAX_LOG_MESSAGE_LENGTH);
        }
        return message;
    }

    private String syslogSeverityToLevel(int severity) {
        return switch (severity) {
            case 0, 1, 2 -> "FATAL";
            case 3 -> "ERROR";
            case 4 -> "WARN";
            case 5, 6 -> "INFO";
            case 7 -> "DEBUG";
            default -> "INFO";
        };
    }
}
