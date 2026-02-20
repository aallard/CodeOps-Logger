package com.codeops.logger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Record of an executed log query for history and replay.
 */
@Entity
@Table(name = "query_history", indexes = {
        @Index(name = "idx_query_history_team_id", columnList = "team_id"),
        @Index(name = "idx_query_history_created_by", columnList = "created_by"),
        @Index(name = "idx_query_history_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryHistory extends BaseEntity {

    /** JSON serialization of the query parameters. */
    @Column(name = "query_json", nullable = false, columnDefinition = "TEXT")
    private String queryJson;

    /** Optional DSL string if SQL-like query was used. */
    @Column(name = "query_dsl", columnDefinition = "TEXT")
    private String queryDsl;

    @Column(name = "result_count", nullable = false)
    private Long resultCount;

    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
