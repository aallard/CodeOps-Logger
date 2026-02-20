package com.codeops.logger.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaConsumerConfig} verifying consumer factory creation.
 */
class KafkaConsumerConfigTest {

    @Test
    void consumerFactoryIsCreated() throws Exception {
        KafkaConsumerConfig config = createConfig();
        ConsumerFactory<String, String> factory = config.consumerFactory();
        assertThat(factory).isNotNull();
        assertThat(factory).isInstanceOf(DefaultKafkaConsumerFactory.class);
    }

    @Test
    void kafkaListenerContainerFactoryIsCreated() throws Exception {
        KafkaConsumerConfig config = createConfig();
        assertThat(config.kafkaListenerContainerFactory()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumerFactoryHasCorrectBootstrapServers() throws Exception {
        KafkaConsumerConfig config = createConfig();
        DefaultKafkaConsumerFactory<String, String> factory =
                (DefaultKafkaConsumerFactory<String, String>) config.consumerFactory();
        Map<String, Object> configProps = factory.getConfigurationProperties();
        assertThat(configProps.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:9094");
    }

    private KafkaConsumerConfig createConfig() throws Exception {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        setField(config, "bootstrapServers", "localhost:9094");
        setField(config, "groupId", "codeops-logger");
        setField(config, "autoOffsetReset", "earliest");
        return config;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
