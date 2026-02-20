package com.codeops.logger.config;

import com.codeops.logger.dto.response.ErrorResponse;
import com.codeops.logger.exception.AuthorizationException;
import com.codeops.logger.exception.GlobalExceptionHandler;
import com.codeops.logger.exception.LoggerException;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GlobalExceptionHandler} verifying each handler returns correct status and message.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleLoggerNotFoundReturns404() {
        ResponseEntity<ErrorResponse> response = handler.handleLoggerNotFound(new NotFoundException("Item not found"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Item not found");
    }

    @Test
    void handleLoggerValidationReturns400() {
        ResponseEntity<ErrorResponse> response = handler.handleLoggerValidation(new ValidationException("Bad input"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Bad input");
    }

    @Test
    void handleLoggerAuthReturns403() {
        ResponseEntity<ErrorResponse> response = handler.handleLoggerAuth(new AuthorizationException("Not allowed"));
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Not allowed");
    }

    @Test
    void handleNotFoundReturns404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(new EntityNotFoundException("entity"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Resource not found");
    }

    @Test
    void handleBadRequestReturns400() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("bad arg"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Invalid request");
    }

    @Test
    void handleForbiddenReturns403() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(new AccessDeniedException("denied"));
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    void handleValidationReturns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "name", "must not be blank")
        ));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("name: must not be blank");
    }

    @Test
    void handleMessageNotReadableReturns400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("parse error");

        ResponseEntity<ErrorResponse> response = handler.handleMessageNotReadable(ex);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed request body");
    }

    @Test
    void handleLoggerExceptionReturns500() {
        ResponseEntity<ErrorResponse> response = handler.handleLogger(new LoggerException("internal"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An internal error occurred");
    }

    @Test
    void handleGeneralExceptionReturns500() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneral(new RuntimeException("unexpected"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An internal error occurred");
    }
}
