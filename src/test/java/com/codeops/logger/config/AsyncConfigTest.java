package com.codeops.logger.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AsyncConfig} to verify executor configuration.
 */
class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();

    @Test
    void getAsyncExecutorReturnsConfiguredExecutor() {
        Executor executor = asyncConfig.getAsyncExecutor();
        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(5);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("logger-async-");
    }

    @Test
    void getAsyncUncaughtExceptionHandlerReturnsHandler() {
        assertThat(asyncConfig.getAsyncUncaughtExceptionHandler()).isNotNull();
    }
}
