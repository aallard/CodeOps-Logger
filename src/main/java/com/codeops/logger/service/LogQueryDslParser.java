package com.codeops.logger.service;

import com.codeops.logger.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the Logger SQL-like DSL query language.
 * Converts human-readable query strings into structured conditions
 * that can be translated to JPA Criteria predicates.
 *
 * <p>Supported syntax examples:</p>
 * <ul>
 *   <li>{@code service = "codeops-server" AND level >= WARN}</li>
 *   <li>{@code message CONTAINS "timeout" AND timestamp > "2026-02-01"}</li>
 *   <li>{@code level IN (ERROR, FATAL) AND host = "prod-server-1"}</li>
 * </ul>
 */
@Component
@Slf4j
public class LogQueryDslParser {

    /** Valid DSL field names. */
    static final Set<String> VALID_FIELDS = Set.of(
            "service", "level", "message", "correlationId", "traceId",
            "logger", "thread", "exception", "host", "timestamp", "ip",
            "spanId", "hostName", "exceptionMessage"
    );

    /** Valid comparison operators. */
    static final Set<String> VALID_OPERATORS = Set.of(
            "=", "!=", ">", "<", ">=", "<=",
            "CONTAINS", "NOT CONTAINS", "IN", "NOT IN",
            "LIKE", "NOT LIKE"
    );

    /** Maps DSL field names to JPA entity field names. */
    static final Map<String, String> FIELD_MAPPING = Map.ofEntries(
            Map.entry("service", "serviceName"),
            Map.entry("level", "level"),
            Map.entry("message", "message"),
            Map.entry("correlationId", "correlationId"),
            Map.entry("traceId", "traceId"),
            Map.entry("logger", "loggerName"),
            Map.entry("thread", "threadName"),
            Map.entry("exception", "exceptionClass"),
            Map.entry("exceptionMessage", "exceptionMessage"),
            Map.entry("host", "hostName"),
            Map.entry("hostName", "hostName"),
            Map.entry("timestamp", "timestamp"),
            Map.entry("ip", "ipAddress"),
            Map.entry("spanId", "spanId")
    );

    /**
     * Pattern that splits on AND/OR conjunctions while preserving them.
     * Uses a lookbehind/lookahead to keep the conjunction as a separator.
     */
    private static final Pattern CONJUNCTION_PATTERN =
            Pattern.compile("\\s+(?i)(AND|OR)\\s+");

    /** Pattern for matching a single condition: field operator value. */
    private static final Pattern CONDITION_PATTERN =
            Pattern.compile("^(\\w+)\\s+(NOT\\s+CONTAINS|NOT\\s+IN|NOT\\s+LIKE|CONTAINS|IN|LIKE|!=|>=|<=|>|<|=)\\s+(.+)$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Parses a DSL query string into a list of conditions.
     *
     * @param dsl the query string
     * @return list of parsed conditions
     * @throws ValidationException on syntax errors
     */
    public List<DslCondition> parse(String dsl) {
        if (dsl == null || dsl.isBlank()) {
            throw new ValidationException("DSL query cannot be empty");
        }

        String trimmed = dsl.trim();
        List<DslCondition> conditions = new ArrayList<>();

        // Split on AND/OR while capturing the conjunctions
        List<String> parts = new ArrayList<>();
        List<String> conjunctions = new ArrayList<>();

        Matcher conjMatcher = CONJUNCTION_PATTERN.matcher(trimmed);
        int lastEnd = 0;
        while (conjMatcher.find()) {
            parts.add(trimmed.substring(lastEnd, conjMatcher.start()).trim());
            conjunctions.add(conjMatcher.group(1).toUpperCase());
            lastEnd = conjMatcher.end();
        }
        parts.add(trimmed.substring(lastEnd).trim());

        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            String conjunction = (i == 0) ? null : conjunctions.get(i - 1);
            DslCondition condition = parseCondition(part, conjunction);
            conditions.add(condition);
        }

        return conditions;
    }

    /**
     * Parses a single condition string into a {@link DslCondition}.
     *
     * @param conditionStr the raw condition text (e.g., {@code service = "codeops-server"})
     * @param conjunction  the conjunction preceding this condition ("AND", "OR", or null for first)
     * @return the parsed condition
     * @throws ValidationException if the condition syntax is invalid
     */
    DslCondition parseCondition(String conditionStr, String conjunction) {
        Matcher matcher = CONDITION_PATTERN.matcher(conditionStr.trim());
        if (!matcher.matches()) {
            throw new ValidationException(
                    "Invalid DSL condition: '" + conditionStr + "'. "
                            + "Expected format: field operator value");
        }

        String field = matcher.group(1);
        String operator = matcher.group(2).toUpperCase().replaceAll("\\s+", " ");
        String value = matcher.group(3).trim();

        validateField(field);
        validateOperator(operator);
        value = stripQuotes(value);

        return new DslCondition(field, operator, value, conjunction);
    }

    /**
     * Validates that a field name is recognized.
     *
     * @param field the field name to validate
     * @throws ValidationException if the field is unknown
     */
    void validateField(String field) {
        if (!VALID_FIELDS.contains(field)) {
            throw new ValidationException(
                    "Unknown DSL field: '" + field + "'. Valid fields: " + VALID_FIELDS);
        }
    }

    /**
     * Validates that an operator is recognized.
     *
     * @param operator the operator to validate
     * @throws ValidationException if the operator is unknown
     */
    void validateOperator(String operator) {
        if (!VALID_OPERATORS.contains(operator)) {
            throw new ValidationException(
                    "Unknown DSL operator: '" + operator + "'. Valid operators: " + VALID_OPERATORS);
        }
    }

    /**
     * Maps a DSL field name to the corresponding JPA entity field name.
     *
     * @param dslField the DSL field name
     * @return the JPA entity field name
     * @throws ValidationException if the field is unknown
     */
    public String mapFieldToEntityField(String dslField) {
        String mapped = FIELD_MAPPING.get(dslField);
        if (mapped == null) {
            throw new ValidationException("Unknown DSL field: '" + dslField + "'");
        }
        return mapped;
    }

    /**
     * Strips surrounding double quotes from a value string.
     * Also handles IN operator parenthesized lists.
     *
     * @param value the raw value from the DSL
     * @return the cleaned value
     */
    String stripQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        // Handle quoted strings
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        // Handle IN lists: (val1, val2, val3) — strip parens but leave values
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * Represents a parsed condition from a DSL query.
     *
     * @param field       the DSL field name
     * @param operator    the comparison operator
     * @param value       the comparison value
     * @param conjunction the logical conjunction ("AND", "OR", or null for first condition)
     */
    record DslCondition(
            String field,
            String operator,
            String value,
            String conjunction
    ) {}
}
