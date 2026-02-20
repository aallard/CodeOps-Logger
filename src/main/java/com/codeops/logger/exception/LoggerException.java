package com.codeops.logger.exception;

/**
 * Base exception for all CodeOps-Logger service exceptions.
 * Maps to HTTP 500 Internal Server Error when not caught by a more specific handler.
 */
public class LoggerException extends RuntimeException {

    /**
     * Creates a new LoggerException with the specified message.
     *
     * @param message the detail message
     */
    public LoggerException(String message) {
        super(message);
    }

    /**
     * Creates a new LoggerException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the root cause
     */
    public LoggerException(String message, Throwable cause) {
        super(message, cause);
    }
}
