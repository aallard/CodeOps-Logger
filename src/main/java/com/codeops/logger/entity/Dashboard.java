package com.codeops.logger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard definition containing a configurable grid of widgets.
 * Dashboards are team-visible by default and can be shared as templates.
 */
@Entity
@Table(name = "dashboards", indexes = {
        @Index(name = "idx_dashboard_team_id", columnList = "team_id"),
        @Index(name = "idx_dashboard_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dashboard extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private Boolean isShared = true;

    @Column(name = "is_template", nullable = false)
    @Builder.Default
    private Boolean isTemplate = false;

    @Column(name = "refresh_interval_seconds")
    @Builder.Default
    private Integer refreshIntervalSeconds = 30;

    /** JSON grid layout configuration (row/column definitions). */
    @Column(name = "layout_json", columnDefinition = "TEXT")
    private String layoutJson;

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DashboardWidget> widgets = new ArrayList<>();
}
