package com.codeops.logger.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CorsConfig} to verify CORS policy.
 */
@SpringBootTest
@ActiveProfiles("test")
class CorsConfigTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void corsConfigurationSourceBeanExists() {
        assertThat(corsConfigurationSource).isNotNull();
        assertThat(corsConfigurationSource).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsAllowsExpectedOrigins() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).contains("http://localhost:3000");
    }

    @Test
    void corsAllowsExpectedMethods() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config).isNotNull();
        assertThat(config.getAllowedMethods()).containsExactlyInAnyOrder(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @Test
    void corsAllowsCredentials() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config).isNotNull();
        assertThat(config.getAllowCredentials()).isTrue();
    }
}
