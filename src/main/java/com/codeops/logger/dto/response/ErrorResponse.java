package com.codeops.logger.dto.response;

/**
 * Standard error response body returned by all exception handlers.
 *
 * @param status  the HTTP status code
 * @param message the human-readable error message
 */
public record ErrorResponse(int status, String message) {}
