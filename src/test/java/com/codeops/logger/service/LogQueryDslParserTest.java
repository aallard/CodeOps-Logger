package com.codeops.logger.service;

import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.service.LogQueryDslParser.DslCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LogQueryDslParser}.
 */
class LogQueryDslParserTest {

    private LogQueryDslParser parser;

    @BeforeEach
    void setUp() {
        parser = new LogQueryDslParser();
    }

    @Test
    void testParse_simpleEquality() {
        List<DslCondition> conditions = parser.parse("service = \"codeops-server\"");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).field()).isEqualTo("service");
        assertThat(conditions.get(0).operator()).isEqualTo("=");
        assertThat(conditions.get(0).value()).isEqualTo("codeops-server");
        assertThat(conditions.get(0).conjunction()).isNull();
    }

    @Test
    void testParse_withAnd() {
        List<DslCondition> conditions = parser.parse("service = \"x\" AND level = ERROR");

        assertThat(conditions).hasSize(2);
        assertThat(conditions.get(0).field()).isEqualTo("service");
        assertThat(conditions.get(0).conjunction()).isNull();
        assertThat(conditions.get(1).field()).isEqualTo("level");
        assertThat(conditions.get(1).operator()).isEqualTo("=");
        assertThat(conditions.get(1).value()).isEqualTo("ERROR");
        assertThat(conditions.get(1).conjunction()).isEqualTo("AND");
    }

    @Test
    void testParse_withOr() {
        List<DslCondition> conditions = parser.parse("level = ERROR OR level = FATAL");

        assertThat(conditions).hasSize(2);
        assertThat(conditions.get(0).value()).isEqualTo("ERROR");
        assertThat(conditions.get(1).value()).isEqualTo("FATAL");
        assertThat(conditions.get(1).conjunction()).isEqualTo("OR");
    }

    @Test
    void testParse_containsOperator() {
        List<DslCondition> conditions = parser.parse("message CONTAINS \"timeout\"");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).field()).isEqualTo("message");
        assertThat(conditions.get(0).operator()).isEqualTo("CONTAINS");
        assertThat(conditions.get(0).value()).isEqualTo("timeout");
    }

    @Test
    void testParse_greaterThan() {
        List<DslCondition> conditions = parser.parse("timestamp > \"2026-02-01T00:00:00Z\"");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).field()).isEqualTo("timestamp");
        assertThat(conditions.get(0).operator()).isEqualTo(">");
        assertThat(conditions.get(0).value()).isEqualTo("2026-02-01T00:00:00Z");
    }

    @Test
    void testParse_lessThanOrEqual() {
        List<DslCondition> conditions = parser.parse("level <= WARN");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).operator()).isEqualTo("<=");
        assertThat(conditions.get(0).value()).isEqualTo("WARN");
    }

    @Test
    void testParse_inOperator() {
        List<DslCondition> conditions = parser.parse("level IN (ERROR, FATAL, WARN)");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).operator()).isEqualTo("IN");
        assertThat(conditions.get(0).value()).isEqualTo("ERROR, FATAL, WARN");
    }

    @Test
    void testParse_notEquals() {
        List<DslCondition> conditions = parser.parse("service != \"codeops-logger\"");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).operator()).isEqualTo("!=");
        assertThat(conditions.get(0).value()).isEqualTo("codeops-logger");
    }

    @Test
    void testParse_quotedValueWithSpaces() {
        List<DslCondition> conditions = parser.parse("message CONTAINS \"connection timeout\"");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).value()).isEqualTo("connection timeout");
    }

    @Test
    void testParse_unquotedEnumValue() {
        List<DslCondition> conditions = parser.parse("level = ERROR");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).value()).isEqualTo("ERROR");
    }

    @Test
    void testParse_multipleAndConditions() {
        List<DslCondition> conditions = parser.parse(
                "service = \"svc\" AND level = ERROR AND message CONTAINS \"fail\"");

        assertThat(conditions).hasSize(3);
        assertThat(conditions.get(0).conjunction()).isNull();
        assertThat(conditions.get(1).conjunction()).isEqualTo("AND");
        assertThat(conditions.get(2).conjunction()).isEqualTo("AND");
        assertThat(conditions.get(2).field()).isEqualTo("message");
        assertThat(conditions.get(2).operator()).isEqualTo("CONTAINS");
    }

    @Test
    void testParse_mixedAndOr() {
        List<DslCondition> conditions = parser.parse(
                "service = \"a\" AND level = ERROR OR host = \"prod\"");

        assertThat(conditions).hasSize(3);
        assertThat(conditions.get(0).conjunction()).isNull();
        assertThat(conditions.get(1).conjunction()).isEqualTo("AND");
        assertThat(conditions.get(2).conjunction()).isEqualTo("OR");
    }

    @Test
    void testParse_invalidField_throwsValidation() {
        assertThatThrownBy(() -> parser.parse("bogusField = \"value\""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unknown DSL field");
    }

    @Test
    void testParse_invalidOperator_throwsValidation() {
        assertThatThrownBy(() -> parser.parse("service MATCHES \"value\""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid DSL condition");
    }

    @Test
    void testParse_emptyQuery_throwsValidation() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    void testParse_missingValue_throwsValidation() {
        assertThatThrownBy(() -> parser.parse("service ="))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid DSL condition");
    }

    @Test
    void testParse_unclosedQuote_treatedAsValue() {
        // An unclosed quote is simply treated as the raw value since regex captures everything after operator
        List<DslCondition> conditions = parser.parse("service = \"unclosed");
        assertThat(conditions).hasSize(1);
        // stripQuotes won't strip because it doesn't end with a quote
        assertThat(conditions.get(0).value()).isEqualTo("\"unclosed");
    }

    @Test
    void testParse_notContains() {
        List<DslCondition> conditions = parser.parse("message NOT CONTAINS \"debug\"");

        assertThat(conditions).hasSize(1);
        assertThat(conditions.get(0).operator()).isEqualTo("NOT CONTAINS");
        assertThat(conditions.get(0).value()).isEqualTo("debug");
    }

    @Test
    void testMapFieldToEntityField_validFields() {
        assertThat(parser.mapFieldToEntityField("service")).isEqualTo("serviceName");
        assertThat(parser.mapFieldToEntityField("logger")).isEqualTo("loggerName");
        assertThat(parser.mapFieldToEntityField("thread")).isEqualTo("threadName");
        assertThat(parser.mapFieldToEntityField("exception")).isEqualTo("exceptionClass");
        assertThat(parser.mapFieldToEntityField("host")).isEqualTo("hostName");
        assertThat(parser.mapFieldToEntityField("ip")).isEqualTo("ipAddress");
        assertThat(parser.mapFieldToEntityField("level")).isEqualTo("level");
        assertThat(parser.mapFieldToEntityField("message")).isEqualTo("message");
        assertThat(parser.mapFieldToEntityField("timestamp")).isEqualTo("timestamp");
    }

    @Test
    void testMapFieldToEntityField_unknownField_throwsValidation() {
        assertThatThrownBy(() -> parser.mapFieldToEntityField("nonexistent"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unknown DSL field");
    }
}
