package com.codeops.logger.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying the exception hierarchy and message propagation.
 */
class ExceptionHierarchyTest {

    @Test
    void loggerExceptionExtendsRuntimeException() {
        assertThat(new LoggerException("test")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void notFoundExceptionExtendsLoggerException() {
        assertThat(new NotFoundException("test")).isInstanceOf(LoggerException.class);
    }

    @Test
    void validationExceptionExtendsLoggerException() {
        assertThat(new ValidationException("test")).isInstanceOf(LoggerException.class);
    }

    @Test
    void authorizationExceptionExtendsLoggerException() {
        assertThat(new AuthorizationException("test")).isInstanceOf(LoggerException.class);
    }

    @Test
    void loggerExceptionMessagePropagated() {
        assertThat(new LoggerException("test message").getMessage()).isEqualTo("test message");
    }

    @Test
    void loggerExceptionCausePropagated() {
        Throwable cause = new RuntimeException("root cause");
        LoggerException ex = new LoggerException("wrapper", cause);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void notFoundExceptionMessagePropagated() {
        assertThat(new NotFoundException("not found").getMessage()).isEqualTo("not found");
    }

    @Test
    void validationExceptionMessagePropagated() {
        assertThat(new ValidationException("bad input").getMessage()).isEqualTo("bad input");
    }

    @Test
    void authorizationExceptionMessagePropagated() {
        assertThat(new AuthorizationException("forbidden").getMessage()).isEqualTo("forbidden");
    }
}
