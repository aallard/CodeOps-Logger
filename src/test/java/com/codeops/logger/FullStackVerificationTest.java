package com.codeops.logger;

import com.codeops.logger.controller.*;
import com.codeops.logger.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack verification test ensuring all components wire up correctly
 * and the application context loads with all controllers, services, and repositories.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FullStackVerificationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void allControllersAreRegistered() {
        assertThat(applicationContext.getBean("healthController")).isNotNull();
        assertThat(applicationContext.getBean("logIngestionController")).isNotNull();
        assertThat(applicationContext.getBean("logQueryController")).isNotNull();
        assertThat(applicationContext.getBean("logSourceController")).isNotNull();
        assertThat(applicationContext.getBean("logTrapController")).isNotNull();
        assertThat(applicationContext.getBean("alertController")).isNotNull();
        assertThat(applicationContext.getBean("metricsController")).isNotNull();
        assertThat(applicationContext.getBean("traceController")).isNotNull();
        assertThat(applicationContext.getBean("dashboardController")).isNotNull();
        assertThat(applicationContext.getBean("retentionController")).isNotNull();
        assertThat(applicationContext.getBean("anomalyController")).isNotNull();
    }

    @Test
    void allEventListenersAreRegistered() {
        assertThat(applicationContext.getBean("logEntryEventListener")).isNotNull();
    }

    @Test
    void allServicesAreRegistered() {
        assertThat(applicationContext.getBean("logIngestionService")).isNotNull();
        assertThat(applicationContext.getBean("logParsingService")).isNotNull();
        assertThat(applicationContext.getBean("logQueryService")).isNotNull();
        assertThat(applicationContext.getBean("logQueryDslParser")).isNotNull();
        assertThat(applicationContext.getBean("logSourceService")).isNotNull();
        assertThat(applicationContext.getBean("logTrapService")).isNotNull();
        assertThat(applicationContext.getBean("trapEvaluationEngine")).isNotNull();
        assertThat(applicationContext.getBean("alertService")).isNotNull();
        assertThat(applicationContext.getBean("alertChannelService")).isNotNull();
        assertThat(applicationContext.getBean("metricsService")).isNotNull();
        assertThat(applicationContext.getBean("metricAggregationService")).isNotNull();
        assertThat(applicationContext.getBean("traceService")).isNotNull();
        assertThat(applicationContext.getBean("traceAnalysisService")).isNotNull();
        assertThat(applicationContext.getBean("dashboardService")).isNotNull();
        assertThat(applicationContext.getBean("retentionService")).isNotNull();
        assertThat(applicationContext.getBean("retentionExecutor")).isNotNull();
        assertThat(applicationContext.getBean("anomalyDetectionService")).isNotNull();
        assertThat(applicationContext.getBean("anomalyBaselineCalculator")).isNotNull();
    }

    @Test
    void allRepositoriesAreRegistered() {
        assertThat(applicationContext.getBean("logSourceRepository")).isNotNull();
        assertThat(applicationContext.getBean("logEntryRepository")).isNotNull();
        assertThat(applicationContext.getBean("logTrapRepository")).isNotNull();
        assertThat(applicationContext.getBean("trapConditionRepository")).isNotNull();
        assertThat(applicationContext.getBean("alertChannelRepository")).isNotNull();
        assertThat(applicationContext.getBean("alertRuleRepository")).isNotNull();
        assertThat(applicationContext.getBean("alertHistoryRepository")).isNotNull();
        assertThat(applicationContext.getBean("metricRepository")).isNotNull();
        assertThat(applicationContext.getBean("metricSeriesRepository")).isNotNull();
        assertThat(applicationContext.getBean("dashboardRepository")).isNotNull();
        assertThat(applicationContext.getBean("dashboardWidgetRepository")).isNotNull();
        assertThat(applicationContext.getBean("traceSpanRepository")).isNotNull();
        assertThat(applicationContext.getBean("retentionPolicyRepository")).isNotNull();
        assertThat(applicationContext.getBean("anomalyBaselineRepository")).isNotNull();
        assertThat(applicationContext.getBean("savedQueryRepository")).isNotNull();
        assertThat(applicationContext.getBean("queryHistoryRepository")).isNotNull();
    }

    @Test
    void allMappersAreRegistered() {
        assertThat(applicationContext.getBean("logSourceMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("logEntryMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("logTrapMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("alertChannelMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("alertRuleMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("alertHistoryMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("metricMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("dashboardMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("traceSpanMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("retentionPolicyMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("anomalyBaselineMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("savedQueryMapperImpl")).isNotNull();
        assertThat(applicationContext.getBean("queryHistoryMapperImpl")).isNotNull();
    }

    @Test
    void healthEndpointReturns200() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/logger/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void protectedEndpointsReturn401WithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/logger/logs/search?q=test", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void swaggerUiIsAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void openApiSpecIsAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("openapi");
    }

    @Test
    void kafkaConsumerBeanRegistered() {
        assertThat(applicationContext.getBean("kafkaLogConsumer")).isNotNull();
    }
}
