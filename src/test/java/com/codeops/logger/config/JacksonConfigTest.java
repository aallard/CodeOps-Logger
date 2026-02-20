package com.codeops.logger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JacksonConfig} to verify Instant serialization.
 */
@SpringBootTest
@ActiveProfiles("test")
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void serializesInstantAsIsoString() throws Exception {
        Instant instant = Instant.parse("2026-01-15T10:30:00Z");
        String json = objectMapper.writeValueAsString(instant);
        assertThat(json).contains("2026-01-15T10:30:00");
        assertThat(json).doesNotContain("1737");  // Not a timestamp number
    }

    @Test
    void deserializesInstantWithTimezone() throws Exception {
        Instant result = objectMapper.readValue("\"2026-01-15T10:30:00Z\"", Instant.class);
        assertThat(result).isEqualTo(Instant.parse("2026-01-15T10:30:00Z"));
    }

    @Test
    void deserializesInstantWithoutTimezone() throws Exception {
        Instant result = objectMapper.readValue("\"2026-01-15T10:30:00\"", Instant.class);
        assertThat(result).isEqualTo(Instant.parse("2026-01-15T10:30:00Z"));
    }
}
