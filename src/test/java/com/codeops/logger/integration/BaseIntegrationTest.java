package com.codeops.logger.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Abstract base class for integration tests using the Docker PostgreSQL container.
 *
 * <p>Connects to the running PostgreSQL container started by {@code docker compose up -d}
 * on port 5437. Database URL, username, and password are dynamically injected into the
 * Spring context. Uses the {@code integration} profile with {@code create-drop} DDL strategy.</p>
 *
 * <p>When Testcontainers Docker compatibility is resolved, this class can be migrated back
 * to use {@code @Testcontainers} with a {@code @Container} annotation for fully isolated tests.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5437/codeops_logger");
        registry.add("spring.datasource.username", () -> "codeops");
        registry.add("spring.datasource.password", () -> "codeops");
    }
}
