package com.codeops.logger.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthController}.
 */
class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void healthReturns200() {
        ResponseEntity<Map<String, Object>> response = controller.health();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void healthResponseContainsRequiredFields() {
        ResponseEntity<Map<String, Object>> response = controller.health();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("status");
        assertThat(body).containsKey("service");
        assertThat(body).containsKey("timestamp");
    }

    @Test
    void healthResponseHasCorrectServiceName() {
        ResponseEntity<Map<String, Object>> response = controller.health();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("service")).isEqualTo("codeops-logger");
    }

    @Test
    void healthResponseStatusIsUp() {
        ResponseEntity<Map<String, Object>> response = controller.health();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
