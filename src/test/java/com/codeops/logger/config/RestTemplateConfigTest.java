package com.codeops.logger.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestTemplateConfig}.
 */
@SpringBootTest
@ActiveProfiles("test")
class RestTemplateConfigTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void restTemplateBeanIsCreated() {
        assertThat(restTemplate).isNotNull();
    }
}
