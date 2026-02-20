package com.codeops.logger.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AppConstants} to verify key constant values.
 */
class AppConstantsTest {

    @Test
    void apiPrefixIsCorrect() {
        assertThat(AppConstants.API_PREFIX).isEqualTo("/api/v1/logger");
    }

    @Test
    void defaultPageSizeIsCorrect() {
        assertThat(AppConstants.DEFAULT_PAGE_SIZE).isEqualTo(20);
    }

    @Test
    void maxPageSizeIsCorrect() {
        assertThat(AppConstants.MAX_PAGE_SIZE).isEqualTo(100);
    }

    @Test
    void maxBatchSizeIsCorrect() {
        assertThat(AppConstants.MAX_BATCH_SIZE).isEqualTo(1000);
    }

    @Test
    void kafkaConsumerGroupIsCorrect() {
        assertThat(AppConstants.KAFKA_CONSUMER_GROUP).isEqualTo("codeops-logger");
    }

    @Test
    void retentionDaysDefaultIsCorrect() {
        assertThat(AppConstants.DEFAULT_RETENTION_DAYS).isEqualTo(30);
    }

    @Test
    void requestTimeoutIsCorrect() {
        assertThat(AppConstants.REQUEST_TIMEOUT_SECONDS).isEqualTo(30);
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<AppConstants> constructor = AppConstants.class.getDeclaredConstructor();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
