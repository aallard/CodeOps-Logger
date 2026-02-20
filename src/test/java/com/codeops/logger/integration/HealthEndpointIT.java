package com.codeops.logger.integration;

import com.codeops.logger.config.AppConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the health endpoint with full HTTP stack.
 */
class HealthEndpointIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void healthEndpointReturns200WithCorrectStructure() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                AppConstants.API_PREFIX + "/health", Map.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody()).containsKey("service");
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("codeops-logger");
    }
}
