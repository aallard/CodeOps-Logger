package com.codeops.logger.entity.enums;

/**
 * Status of a trace span indicating success or failure.
 */
public enum SpanStatus {
    /** Span completed successfully. */
    OK,
    /** Span encountered an error. */
    ERROR
}
