package com.codeops.logger.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JwtProperties} to verify property binding.
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtPropertiesTest {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void secretIsLoaded() {
        assertThat(jwtProperties.secret()).isNotNull();
        assertThat(jwtProperties.secret()).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void expirationHoursIsLoaded() {
        assertThat(jwtProperties.expirationHours()).isEqualTo(24);
    }

    @Test
    void refreshExpirationDaysIsLoaded() {
        assertThat(jwtProperties.refreshExpirationDays()).isEqualTo(30);
    }
}
