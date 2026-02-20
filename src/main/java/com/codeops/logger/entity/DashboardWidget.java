package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.WidgetType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Individual widget within a dashboard. Positioned on a grid layout
 * and bound to a data query that feeds the visualization.
 */
@Entity
@Table(name = "dashboard_widgets", indexes = {
        @Index(name = "idx_widget_dashboard_id", columnList = "dashboard_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_type", nullable = false, length = 30)
    private WidgetType widgetType;

    /** JSON data query binding (log query, metric query, etc.). */
    @Column(name = "query_json", columnDefinition = "TEXT")
    private String queryJson;

    /** JSON widget-specific configuration (colors, thresholds, etc.). */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "grid_x", nullable = false)
    @Builder.Default
    private Integer gridX = 0;

    @Column(name = "grid_y", nullable = false)
    @Builder.Default
    private Integer gridY = 0;

    @Column(name = "grid_width", nullable = false)
    @Builder.Default
    private Integer gridWidth = 4;

    @Column(name = "grid_height", nullable = false)
    @Builder.Default
    private Integer gridHeight = 3;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
