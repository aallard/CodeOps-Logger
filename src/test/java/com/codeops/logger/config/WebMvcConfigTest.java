package com.codeops.logger.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link WebMvcConfig}.
 */
class WebMvcConfigTest {

    @Test
    void registersLoggingInterceptor() {
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        WebMvcConfig config = new WebMvcConfig(loggingInterceptor);

        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        when(registry.addInterceptor(loggingInterceptor)).thenReturn(registration);
        when(registration.addPathPatterns("/api/**")).thenReturn(registration);

        config.addInterceptors(registry);

        verify(registry).addInterceptor(loggingInterceptor);
        verify(registration).addPathPatterns("/api/**");
    }
}
