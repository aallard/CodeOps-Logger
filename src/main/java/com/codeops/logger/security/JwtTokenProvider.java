package com.codeops.logger.security;

import com.codeops.logger.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * JWT token validation provider. Validates tokens issued by CodeOps-Server using the
 * shared HMAC-SHA256 secret. This service never issues tokens.
 *
 * @see JwtProperties
 * @see JwtAuthFilter
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;

    /**
     * Validates that the JWT secret is configured and meets the minimum length requirement
     * of 32 characters. Invoked automatically after dependency injection.
     *
     * @throws IllegalStateException if the secret is null, blank, or shorter than 32 characters
     */
    @PostConstruct
    public void validateSecret() {
        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters. Set the JWT_SECRET environment variable.");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates a JWT token by verifying its signature, expiration, and format.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts the user ID from the JWT token's subject claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the user's UUID parsed from the token subject
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the email address from the JWT token's {@code "email"} claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the email address stored in the token, or {@code null} if absent
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts the list of role names from the JWT token's {@code "roles"} claim.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the list of role name strings from the token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("roles", List.class);
    }

    /**
     * Parses and verifies a JWT token, returning the claims payload.
     *
     * @param token the raw JWT string (without {@code "Bearer "} prefix)
     * @return the parsed {@link Claims} from the token payload
     */
    Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
