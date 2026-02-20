package com.codeops.logger.entity;

import com.codeops.logger.entity.enums.ConditionType;
import com.codeops.logger.entity.enums.LogLevel;
import jakarta.persistence.*;
import lombok.*;

/**
 * Individual condition within a log trap. Multiple conditions form an AND-based evaluation
 * — all conditions must match for the trap to fire.
 */
@Entity
@Table(name = "trap_conditions", indexes = {
        @Index(name = "idx_trap_condition_trap_id", columnList = "trap_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrapCondition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trap_id", nullable = false)
    private LogTrap trap;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 30)
    private ConditionType conditionType;

    /** Which log field to evaluate (e.g., "message", "logger_name", "exception_class"). */
    @Column(name = "field", nullable = false, length = 100)
    private String field;

    /** Regex or keyword pattern for REGEX/KEYWORD types. */
    @Column(name = "pattern", columnDefinition = "TEXT")
    private String pattern;

    /** Count threshold for FREQUENCY_THRESHOLD type. */
    @Column(name = "threshold")
    private Integer threshold;

    /** Time window in seconds for FREQUENCY_THRESHOLD and ABSENCE types. */
    @Column(name = "window_seconds")
    private Integer windowSeconds;

    /** Optional service name filter — condition only applies to logs from this service. */
    @Column(name = "service_name", length = 200)
    private String serviceName;

    /** Optional log level filter — condition only applies to logs at or above this level. */
    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", length = 10)
    private LogLevel logLevel;
}
