package com.codeops.logger.exception;

/**
 * Thrown when request validation fails.
 * Maps to HTTP 400 Bad Request.
 */
public class ValidationException extends LoggerException {

    /**
     * Creates a new ValidationException with the specified message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
    }
}
