package com.codeops.logger.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link RequestCorrelationFilter}.
 */
class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        MDC.clear();
    }

    @Test
    void generatesUuidWhenNoHeader() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logger/health");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull().isNotEmpty();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void preservesExistingHeader() throws ServletException, IOException {
        String existingId = "test-correlation-id-123";
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, existingId);
        request.setRequestURI("/api/v1/logger/health");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
    }

    @Test
    void addsCorrelationIdToResponse() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logger/health");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isNotNull();
    }

    @Test
    void clearsMdcAfterRequest() throws ServletException, IOException {
        request.setRequestURI("/api/v1/logger/health");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get(RequestCorrelationFilter.MDC_CORRELATION_ID)).isNull();
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_PATH)).isNull();
        assertThat(MDC.get(RequestCorrelationFilter.MDC_REQUEST_METHOD)).isNull();
    }

    @Test
    void generatesNewIdForBlankHeader() throws ServletException, IOException {
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, "   ");
        request.setRequestURI("/api/v1/logger/health");
        request.setMethod("GET");

        filter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId).isNotEqualTo("   ");
    }
}
