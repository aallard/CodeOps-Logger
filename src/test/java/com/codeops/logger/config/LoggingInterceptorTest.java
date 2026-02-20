package com.codeops.logger.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link LoggingInterceptor}.
 */
class LoggingInterceptorTest {

    private final LoggingInterceptor interceptor = new LoggingInterceptor();
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        MDC.clear();
    }

    @Test
    void preHandleReturnsTrue() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/logger/health");
        boolean result = interceptor.preHandle(request, response, new Object());
        assertThat(result).isTrue();
    }

    @Test
    void preHandleSetsStartTimeAttribute() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/logger/health");
        interceptor.preHandle(request, response, new Object());
        verify(request).setAttribute(eq(LoggingInterceptor.START_TIME_ATTR), anyLong());
    }

    @Test
    void afterCompletionHandles5xx() {
        when(request.getAttribute(LoggingInterceptor.START_TIME_ATTR)).thenReturn(System.currentTimeMillis());
        when(response.getStatus()).thenReturn(500);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/logger/test");

        interceptor.afterCompletion(request, response, new Object(), null);
        // Should not throw, logs at ERROR level
    }

    @Test
    void afterCompletionHandles4xx() {
        when(request.getAttribute(LoggingInterceptor.START_TIME_ATTR)).thenReturn(System.currentTimeMillis());
        when(response.getStatus()).thenReturn(404);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/logger/missing");

        interceptor.afterCompletion(request, response, new Object(), null);
        // Should not throw, logs at WARN level
    }

    @Test
    void afterCompletionHandles2xx() {
        when(request.getAttribute(LoggingInterceptor.START_TIME_ATTR)).thenReturn(System.currentTimeMillis());
        when(response.getStatus()).thenReturn(200);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/logger/health");

        interceptor.afterCompletion(request, response, new Object(), null);
        // Should not throw, logs at INFO level
    }

    @Test
    void afterCompletionHandlesNullStartTime() {
        when(request.getAttribute(LoggingInterceptor.START_TIME_ATTR)).thenReturn(null);
        when(response.getStatus()).thenReturn(200);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/logger/health");

        interceptor.afterCompletion(request, response, new Object(), null);
        // Should not throw, duration reported as -1
    }
}
