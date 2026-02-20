package com.codeops.logger.security;

import com.codeops.logger.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JwtTokenProvider}.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-minimum-32-characters-long-for-hs256-testing";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, 24, 30);
        provider = new JwtTokenProvider(props);
        provider.validateSecret();
    }

    @Test
    void validateSecretRejectsShortSecret() {
        JwtProperties shortProps = new JwtProperties("short", 24, 30);
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);
        assertThatThrownBy(shortProvider::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 characters");
    }

    @Test
    void validateSecretRejectsNullSecret() {
        JwtProperties nullProps = new JwtProperties(null, 24, 30);
        JwtTokenProvider nullProvider = new JwtTokenProvider(nullProps);
        assertThatThrownBy(nullProvider::validateSecret)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateSecretRejectsBlankSecret() {
        JwtProperties blankProps = new JwtProperties("   ", 24, 30);
        JwtTokenProvider blankProvider = new JwtTokenProvider(blankProps);
        assertThatThrownBy(blankProvider::validateSecret)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateTokenReturnsTrueForValid() {
        String token = generateTestToken(UUID.randomUUID(), "test@test.com", List.of("ADMIN"));
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateTokenReturnsFalseForExpired() {
        String token = generateExpiredToken();
        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void validateTokenReturnsFalseForTampered() {
        String token = generateTestToken(UUID.randomUUID(), "test@test.com", List.of("ADMIN"));
        assertThat(provider.validateToken(token + "tampered")).isFalse();
    }

    @Test
    void validateTokenReturnsFalseForWrongSecret() {
        String wrongSecret = "wrong-secret-key-minimum-32-characters-long-for-hs256-wrong!!";
        SecretKey wrongKey = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();
        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void getUserIdFromTokenExtractsCorrectId() {
        UUID userId = UUID.randomUUID();
        String token = generateTestToken(userId, "test@test.com", List.of("MEMBER"));
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(userId);
    }

    @Test
    void getEmailFromTokenExtractsCorrectEmail() {
        String token = generateTestToken(UUID.randomUUID(), "admin@codeops.dev", List.of("ADMIN"));
        assertThat(provider.getEmailFromToken(token)).isEqualTo("admin@codeops.dev");
    }

    @Test
    void getRolesFromTokenExtractsCorrectRoles() {
        String token = generateTestToken(UUID.randomUUID(), "test@test.com", List.of("ADMIN", "OWNER"));
        List<String> roles = provider.getRolesFromToken(token);
        assertThat(roles).containsExactlyInAnyOrder("ADMIN", "OWNER");
    }

    private String generateTestToken(UUID userId, String email, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private String generateExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
