package com.codeops.logger.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Jackson ObjectMapper customizations for CodeOps-Logger.
 *
 * <p>Registers a lenient {@link Instant} deserializer that accepts ISO-8601
 * timestamps both with and without a timezone/offset suffix. Timestamps
 * without zone info (e.g. {@code 2026-02-16T08:25:00.000}) are interpreted
 * as UTC.</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Customizes the Jackson ObjectMapper to handle Instant timestamps
     * that lack a timezone indicator by assuming UTC.
     *
     * @return the customizer bean
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer instantDeserializerCustomizer() {
        return builder -> {
            JavaTimeModule module = new JavaTimeModule();
            module.addDeserializer(Instant.class, new LenientInstantDeserializer());
            builder.modules(module);
        };
    }

    /**
     * Deserializes ISO-8601 strings to {@link Instant}, falling back to
     * interpreting zone-less timestamps as UTC.
     */
    static class LenientInstantDeserializer extends JsonDeserializer<Instant> {

        /**
         * Deserializes the JSON text to an {@link Instant}.
         *
         * @param p    the JSON parser
         * @param ctxt the deserialization context
         * @return the parsed Instant
         * @throws IOException if an I/O error occurs
         */
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException e) {
                return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            }
        }
    }
}
