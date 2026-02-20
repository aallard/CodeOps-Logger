package com.codeops.logger.config;

/**
 * Centralized constants for CodeOps-Logger. All magic numbers, limits, and configuration
 * defaults are defined here. No hardcoded values should exist outside this class.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // === API ===

    /** API path prefix for all Logger endpoints. */
    public static final String API_PREFIX = "/api/v1/logger";

    // === Pagination ===

    /** Default number of items per page for paginated queries. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Maximum number of items per page for paginated queries. */
    public static final int MAX_PAGE_SIZE = 100;

    // === Log Ingestion ===

    /** Maximum number of log entries in a single batch ingestion request. */
    public static final int MAX_BATCH_SIZE = 1000;

    /** Maximum length of a single log message in characters. */
    public static final int MAX_LOG_MESSAGE_LENGTH = 65_536;

    /** Maximum number of custom fields allowed on a log entry. */
    public static final int MAX_CUSTOM_FIELDS = 50;

    // === Kafka ===

    /** Kafka topic for log event ingestion. */
    public static final String KAFKA_LOG_TOPIC = "codeops-logs";

    /** Kafka topic for metric event ingestion. */
    public static final String KAFKA_METRICS_TOPIC = "codeops-metrics";

    /** Kafka consumer group identifier. */
    public static final String KAFKA_CONSUMER_GROUP = "codeops-logger";

    // === Retention ===

    /** Default retention period for log data in days. */
    public static final int DEFAULT_RETENTION_DAYS = 30;

    /** Maximum retention period for log data in days. */
    public static final int MAX_RETENTION_DAYS = 365;

    /** Minimum retention period for log data in days. */
    public static final int MIN_RETENTION_DAYS = 1;

    // === Query ===

    /** Maximum number of results returned by a single query. */
    public static final int MAX_QUERY_RESULTS = 10_000;

    /** Default result limit for queries. */
    public static final int DEFAULT_QUERY_LIMIT = 100;

    /** Maximum time range for a query in days. */
    public static final int MAX_QUERY_TIME_RANGE_DAYS = 90;

    // === Metrics ===

    /** Default metric resolution in seconds. */
    public static final int DEFAULT_METRIC_RESOLUTION_SECONDS = 60;

    /** Minimum metric resolution in seconds. */
    public static final int MIN_METRIC_RESOLUTION_SECONDS = 10;

    /** Maximum metric resolution in seconds. */
    public static final int MAX_METRIC_RESOLUTION_SECONDS = 3600;

    // === Dashboards ===

    /** Maximum number of widgets per dashboard. */
    public static final int MAX_WIDGETS_PER_DASHBOARD = 20;

    /** Maximum number of dashboards per team. */
    public static final int MAX_DASHBOARDS_PER_TEAM = 50;

    /** Default dashboard auto-refresh interval in seconds. */
    public static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 30;

    // === Traps ===

    /** Maximum number of traps per team. */
    public static final int MAX_TRAPS_PER_TEAM = 100;

    /** Maximum number of conditions per trap. */
    public static final int MAX_TRAP_CONDITIONS = 10;

    // === Alerts ===

    /** Default alert throttle interval in minutes. */
    public static final int DEFAULT_THROTTLE_MINUTES = 5;

    /** Maximum number of alert channels per team. */
    public static final int MAX_ALERT_CHANNELS = 20;

    // === Anomaly Detection ===

    /** Default baseline window for anomaly detection in hours (7 days). */
    public static final int DEFAULT_BASELINE_WINDOW_HOURS = 168;

    /** Default deviation threshold in standard deviations. */
    public static final double DEFAULT_DEVIATION_THRESHOLD = 2.0;

    // === Timeouts ===

    /** Default request timeout in seconds. */
    public static final int REQUEST_TIMEOUT_SECONDS = 30;
}
