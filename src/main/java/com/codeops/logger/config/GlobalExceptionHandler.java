package com.codeops.logger.config;

import com.codeops.logger.dto.response.ErrorResponse;
import com.codeops.logger.exception.AuthorizationException;
import com.codeops.logger.exception.LoggerException;
import com.codeops.logger.exception.NotFoundException;
import com.codeops.logger.exception.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST controllers in the CodeOps-Logger API.
 *
 * <p>Catches application-specific exceptions ({@link NotFoundException}, {@link ValidationException},
 * {@link AuthorizationException}, {@link LoggerException}), Spring/JPA exceptions
 * ({@link EntityNotFoundException}, {@link AccessDeniedException}, {@link MethodArgumentNotValidException}),
 * and general uncaught exceptions. Each handler returns a structured {@link ErrorResponse} with the
 * appropriate HTTP status code.</p>
 *
 * <p>Internal error details are never exposed to clients.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles CodeOps-specific {@link NotFoundException} by returning a 404 response.
     *
     * @param ex the thrown exception
     * @return a 404 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLoggerNotFound(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorResponse(404, ex.getMessage()));
    }

    /**
     * Handles CodeOps-specific {@link ValidationException} by returning a 400 response.
     *
     * @param ex the thrown exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleLoggerValidation(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new ErrorResponse(400, ex.getMessage()));
    }

    /**
     * Handles CodeOps-specific {@link AuthorizationException} by returning a 403 response.
     *
     * @param ex the thrown exception
     * @return a 403 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleLoggerAuth(AuthorizationException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());
        return ResponseEntity.status(403).body(new ErrorResponse(403, ex.getMessage()));
    }

    /**
     * Handles JPA {@link EntityNotFoundException} by returning a 404 response.
     *
     * @param ex the thrown exception
     * @return a 404 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorResponse(404, "Resource not found"));
    }

    /**
     * Handles {@link IllegalArgumentException} by returning a 400 response.
     *
     * @param ex the thrown exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new ErrorResponse(400, "Invalid request"));
    }

    /**
     * Handles Spring Security {@link AccessDeniedException} by returning a 403 response.
     *
     * @param ex the thrown exception
     * @return a 403 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(403).body(new ErrorResponse(403, "Access denied"));
    }

    /**
     * Handles Jakarta Bean Validation failures by returning a 400 response with field error details.
     *
     * @param ex the thrown exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", msg);
        return ResponseEntity.status(400).body(new ErrorResponse(400, msg));
    }

    /**
     * Handles malformed JSON or type-mismatch errors in request bodies.
     *
     * @param ex the thrown exception
     * @return a 400 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.status(400).body(new ErrorResponse(400, "Malformed request body"));
    }

    /**
     * Handles requests for unmapped paths.
     *
     * @param ex the thrown exception
     * @return a 404 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorResponse(404, "Resource not found"));
    }

    /**
     * Handles the base {@link LoggerException} by returning a 500 response with a generic message.
     *
     * @param ex the thrown exception
     * @return a 500 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(LoggerException.class)
    public ResponseEntity<ErrorResponse> handleLogger(LoggerException ex) {
        log.error("Application exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(500).body(new ErrorResponse(500, "An internal error occurred"));
    }

    /**
     * Catch-all handler for any unhandled exceptions. Returns a 500 response with a generic message.
     *
     * @param ex the unhandled exception
     * @return a 500 response with an {@link ErrorResponse} body
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(500).body(new ErrorResponse(500, "An internal error occurred"));
    }
}
