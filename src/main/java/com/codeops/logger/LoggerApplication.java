package com.codeops.logger;

import com.codeops.logger.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the CodeOps-Logger microservice. Provides centralized logging,
 * metrics, alerting, trace correlation, dashboards, and anomaly detection.
 */
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class LoggerApplication {

    /**
     * Launches the CodeOps-Logger Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LoggerApplication.class, args);
    }
}
