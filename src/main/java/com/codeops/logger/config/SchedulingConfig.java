package com.codeops.logger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduling and async infrastructure for timed retention policy execution
 * and asynchronous event-driven trap evaluation.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
