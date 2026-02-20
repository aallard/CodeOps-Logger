package com.codeops.logger.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link BaseEntity} lifecycle callbacks.
 */
class BaseEntityTest {

    /** Concrete subclass for testing the abstract BaseEntity. */
    static class TestEntity extends BaseEntity {}

    @Test
    void onCreateSetsBothTimestamps() {
        TestEntity entity = new TestEntity();
        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isCloseTo(entity.getUpdatedAt(), within(1, ChronoUnit.MILLIS));
    }

    @Test
    void onUpdateSetsOnlyUpdatedAt() throws InterruptedException {
        TestEntity entity = new TestEntity();
        entity.onCreate();
        Instant originalCreatedAt = entity.getCreatedAt();
        Instant originalUpdatedAt = entity.getUpdatedAt();

        Thread.sleep(10);
        entity.onUpdate();

        assertThat(entity.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(entity.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void idIsNullBeforePersist() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getId()).isNull();
    }

    @Test
    void timestampsAreNullBeforeOnCreate() {
        TestEntity entity = new TestEntity();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }
}
