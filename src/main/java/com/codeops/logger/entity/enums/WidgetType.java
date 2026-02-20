package com.codeops.logger.entity.enums;

/**
 * Available widget types for dashboard composition.
 */
public enum WidgetType {
    /** Live-streaming log entries. */
    LOG_STREAM,
    /** Time-series line/area chart. */
    TIME_SERIES_CHART,
    /** Single numeric counter display. */
    COUNTER,
    /** Gauge visualization (radial or linear). */
    GAUGE,
    /** Tabular data display. */
    TABLE,
    /** Heatmap grid visualization. */
    HEATMAP,
    /** Pie or donut chart. */
    PIE_CHART,
    /** Vertical or horizontal bar chart. */
    BAR_CHART
}
