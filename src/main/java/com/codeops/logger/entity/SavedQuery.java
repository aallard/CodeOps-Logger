package com.codeops.logger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * User-saved log query for quick reuse. Stores the query parameters
 * as a JSON string for flexible schema evolution.
 */
@Entity
@Table(name = "saved_queries", indexes = {
        @Index(name = "idx_saved_query_team_id", columnList = "team_id"),
        @Index(name = "idx_saved_query_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedQuery extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** JSON serialization of the query parameters. */
    @Column(name = "query_json", nullable = false, columnDefinition = "TEXT")
    private String queryJson;

    /** Optional DSL query string if using SQL-like syntax. */
    @Column(name = "query_dsl", columnDefinition = "TEXT")
    private String queryDsl;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private Boolean isShared = false;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "execution_count", nullable = false)
    @Builder.Default
    private Long executionCount = 0L;
}
