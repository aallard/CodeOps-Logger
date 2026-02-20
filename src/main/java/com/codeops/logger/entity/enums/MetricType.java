package com.codeops.logger.entity.enums;

/**
 * Types of metrics that can be collected and stored.
 */
public enum MetricType {
    /** Monotonically increasing value (e.g., total requests). */
    COUNTER,
    /** Point-in-time value that can go up or down (e.g., active connections). */
    GAUGE,
    /** Distribution of values with buckets (e.g., request size). */
    HISTOGRAM,
    /** Duration measurement (e.g., response time). */
    TIMER
}
