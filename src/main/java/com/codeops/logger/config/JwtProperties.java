package com.codeops.logger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT token validation. Logger validates tokens issued
 * by CodeOps-Server but never issues its own.
 *
 * @param secret               the HMAC-SHA256 signing secret (minimum 32 characters)
 * @param expirationHours      access token lifetime in hours
 * @param refreshExpirationDays refresh token lifetime in days
 */
@ConfigurationProperties(prefix = "codeops.jwt")
public record JwtProperties(
        String secret,
        int expirationHours,
        int refreshExpirationDays
) {}
