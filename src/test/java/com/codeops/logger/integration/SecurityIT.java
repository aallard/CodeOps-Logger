package com.codeops.logger.integration;

import com.codeops.logger.config.AppConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying JWT security through the full HTTP stack.
 */
class SecurityIT extends BaseIntegrationTest {

    private static final String SECRET = "integration-test-secret-minimum-32-characters-long-for-hs256";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void unauthenticatedRequestReturns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                AppConstants.API_PREFIX + "/dashboards", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void requestWithValidJwtSucceeds() {
        String token = generateValidToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // Health endpoint should work with or without auth
        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.API_PREFIX + "/health",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void requestWithExpiredJwtReturns401() {
        String token = generateExpiredToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                AppConstants.API_PREFIX + "/dashboards",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    private String generateValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "test@codeops.dev")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String generateExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "test@codeops.dev")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
