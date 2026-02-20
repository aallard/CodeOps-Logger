package com.codeops.logger.exception;

/**
 * Thrown when the authenticated user lacks permission for the requested operation.
 * Maps to HTTP 403 Forbidden.
 */
public class AuthorizationException extends LoggerException {

    /**
     * Creates a new AuthorizationException with the specified message.
     *
     * @param message the detail message
     */
    public AuthorizationException(String message) {
        super(message);
    }
}
