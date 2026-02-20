package com.codeops.logger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduling infrastructure for timed retention policy execution.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
