# CodeOps-Logger — Codebase Audit

**Audit Date:** 2026-02-20T23:54:20Z
**Branch:** main
**Commit:** 0c7bcce1cad4772eec27c523f8709ce76a8fe83a CL-FIX-003: Wire trap pipeline and move misplaced classes
**Auditor:** Claude Code (Automated)
**Purpose:** Zero-context reference for AI-assisted development
**Audit File:** CodeOps-Logger-Audit.md
**Scorecard:** CodeOps-Logger-Scorecard.md
**OpenAPI Spec:** CodeOps-Logger-OpenAPI.yaml

> This audit is the single source of truth for the CodeOps-Logger codebase.
> The OpenAPI spec (CodeOps-Logger-OpenAPI.yaml) is the source of truth for all endpoints, DTOs, and API contracts.
> An AI reading this audit + the OpenAPI spec should be able to generate accurate code
> changes, new features, tests, and fixes without filesystem access.

---

## 1. Project Identity

```
Project Name: CodeOps-Logger
Repository URL: https://github.com/adamallard/CodeOps-Logger
Primary Language / Framework: Java / Spring Boot
Java Version: 21 (running on Java 25 with compatibility overrides)
Build Tool: Maven (Spring Boot 3.3.0 parent)
Current Branch: main
Latest Commit Hash: 0c7bcce1cad4772eec27c523f8709ce76a8fe83a
Latest Commit Message: CL-FIX-003: Wire trap pipeline and move misplaced classes
Audit Timestamp: 2026-02-20T23:54:20Z
```

---

## 2. Directory Structure

```
CodeOps-Logger/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── CONVENTIONS.md
├── src/main/java/com/codeops/logger/
│   ├── LoggerApplication.java
│   ├── config/
│   │   ├── AppConstants.java
│   │   ├── AsyncConfig.java
│   │   ├── CorsConfig.java
│   │   ├── DataSeeder.java
│   │   ├── JacksonConfig.java
│   │   ├── JwtProperties.java
│   │   ├── KafkaConsumerConfig.java
│   │   ├── LoggingInterceptor.java
│   │   ├── RequestCorrelationFilter.java
│   │   ├── RestTemplateConfig.java
│   │   ├── SchedulingConfig.java
│   │   └── WebMvcConfig.java
│   ├── controller/
│   │   ├── BaseController.java (abstract)
│   │   ├── HealthController.java
│   │   ├── AlertController.java
│   │   ├── AnomalyController.java
│   │   ├── DashboardController.java
│   │   ├── LogIngestionController.java
│   │   ├── LogQueryController.java
│   │   ├── LogSourceController.java
│   │   ├── LogTrapController.java
│   │   ├── MetricsController.java
│   │   ├── RetentionController.java
│   │   └── TraceController.java
│   ├── dto/
│   │   ├── mapper/ (13 MapStruct mappers)
│   │   ├── request/ (34 request DTOs)
│   │   └── response/ (30 response DTOs)
│   ├── entity/
│   │   ├── BaseEntity.java
│   │   ├── enums/ (10 enums)
│   │   └── (16 entity classes)
│   ├── event/
│   │   ├── LogEntryEventListener.java
│   │   └── LogEntryIngestedEvent.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── LoggerException.java
│   │   ├── NotFoundException.java
│   │   ├── ValidationException.java
│   │   └── AuthorizationException.java
│   ├── repository/ (16 repositories)
│   ├── security/
│   │   ├── JwtAuthFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── SecurityConfig.java
│   │   └── SecurityUtils.java
│   └── service/ (18 service classes)
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── application-test.yml
│   ├── application-integration.yml
│   └── logback-spring.xml
└── src/test/ (51 unit test files, 5 integration test files)
```

Single-module Maven project. Source under `src/main/java/com/codeops/logger/`. Standard Spring Boot package layout with config, controller, dto (mapper/request/response), entity (enums), event, exception, repository, security, and service packages.

---

## 3. Build & Dependency Manifest

**`pom.xml`** — path: `./pom.xml`

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-parent | 3.3.0 | Parent POM |
| spring-boot-starter-web | (managed) | REST API framework |
| spring-boot-starter-data-jpa | (managed) | JPA / Hibernate ORM |
| spring-boot-starter-security | (managed) | Security framework |
| spring-boot-starter-validation | (managed) | Jakarta Bean Validation |
| postgresql | (managed) | PostgreSQL JDBC driver |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT token validation |
| spring-kafka | (managed) | Kafka consumer integration |
| lombok | 1.18.42 (override) | Boilerplate reduction |
| mapstruct | 1.5.5.Final | Entity↔DTO mapping |
| jackson-datatype-jsr310 | (managed) | Java time serialization |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI / OpenAPI |
| logstash-logback-encoder | 7.4 | Structured JSON logging |
| spring-boot-starter-test | (managed) | Test framework |
| spring-security-test | (managed) | Security test utilities |
| testcontainers (postgresql) | 1.19.8 | Integration test containers |
| testcontainers (junit-jupiter) | 1.19.8 | JUnit 5 Testcontainers |
| testcontainers (kafka) | 1.19.8 | Kafka test containers |
| spring-kafka-test | (managed) | Kafka testing utilities |
| h2 | (managed) | In-memory test database |
| mockito | 5.21.0 (override) | Java 25 compatibility |
| byte-buddy | 1.18.4 (override) | Java 25 compatibility |

**Build plugins:**
- `spring-boot-maven-plugin` — Excludes Lombok from fat JAR
- `maven-compiler-plugin` — Java 21 source/target, annotation processor paths for Lombok + MapStruct
- `maven-surefire-plugin` — Includes `*Test.java` and `*IT.java`, `--add-opens` for Java 25 compatibility
- `jacoco-maven-plugin` 0.8.14 — Code coverage with prepare-agent + report in test phase

```
Build: mvn clean package -DskipTests
Test: mvn test
Run: mvn spring-boot:run
Package: mvn clean package
```

---

## 4. Configuration & Infrastructure Summary

- **`application.yml`** — `src/main/resources/application.yml` — App name `codeops-logger`, default profile `dev`, port **8098**
- **`application-dev.yml`** — `src/main/resources/application-dev.yml` — PostgreSQL at `localhost:5437/codeops_logger`, Hibernate `ddl-auto: update`, Kafka at `localhost:9094` (group `codeops-logger`), JWT dev default, CORS `localhost:3000,5173`, DEBUG logging
- **`application-prod.yml`** — `src/main/resources/application-prod.yml` — All from env vars (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`), `ddl-auto: validate`, Kafka offset `latest`, INFO/WARN logging
- **`application-test.yml`** — `src/main/resources/application-test.yml` — H2 in-memory, `create-drop`, Kafka listener disabled (`auto-startup: false`), WARN logging
- **`application-integration.yml`** — `src/main/resources/application-integration.yml` — PostgreSQL dialect, `create-drop` (for Testcontainers), embedded Kafka fallback, WARN logging
- **`logback-spring.xml`** — `src/main/resources/logback-spring.xml` — Dev: human-readable console with `[correlationId] [userId]`. Prod: JSON via LogstashEncoder (MDC keys: correlationId, userId, teamId, requestPath). Test: minimal WARN.
- **`docker-compose.yml`** — `./docker-compose.yml` — PostgreSQL 16 Alpine at `127.0.0.1:5437`, container `codeops-logger-db`, DB `codeops_logger`, user/pass `codeops/codeops`, healthcheck via `pg_isready`
- **`Dockerfile`** — `./Dockerfile` — `eclipse-temurin:21-jre-alpine`, non-root `appuser:appgroup`, port 8098

**Connection map:**
```
Database: PostgreSQL, localhost, 5437, codeops_logger
Cache: None
Message Broker: Kafka, localhost, 9094 (consumer only, topic: codeops-logs)
External APIs: Webhook notifications (AlertChannelService → user-configured URLs)
Cloud Services: None
```

**CI/CD:** None detected.

---

## 5. Startup & Runtime Behavior

- **Entry point:** `LoggerApplication.java` — `@SpringBootApplication` + `@EnableConfigurationProperties(JwtProperties.class)`
- **`@PostConstruct`:** `JwtTokenProvider.validateSecret()` — validates JWT secret ≥ 32 chars or throws `IllegalStateException`
- **Data seeder:** `DataSeeder` (`@Profile("dev")`) — seeds log entries, sources, traps, conditions, channels, rules, metrics, dashboards, traces, baselines, retention policies, saved queries
- **Scheduled tasks:**
  - `RetentionExecutor.executeAllActivePolicies()` — `@Scheduled(cron = "0 0 2 * * ?")` — Daily at 2:00 AM
  - `AnomalyDetectionService.recalculateAllBaselines()` — `@Scheduled(cron = "${codeops.anomaly.recalculation-cron:0 0 3 * * *}")` — Daily at 3:00 AM (configurable)
- **Health check:** `GET /api/v1/logger/health` (public) — returns `{"status": "UP", "service": "codeops-logger", "timestamp": "..."}`

---

## 6. Entity / Data Model Layer

### BaseEntity.java
```
@MappedSuperclass
Primary Key: id UUID (GenerationType.UUID)

Fields:
  - id: UUID @Id @GeneratedValue(strategy = GenerationType.UUID)
  - createdAt: Instant @Column(updatable = false)
  - updatedAt: Instant

Auditing: @PrePersist sets createdAt + updatedAt; @PreUpdate sets updatedAt
```

---

### LogEntry.java
```
Table: log_entries
Primary Key: id UUID (inherited from BaseEntity)

Fields:
  - level: LogLevel @Enumerated(STRING) (nullable = false)
  - message: String (columnDefinition = "TEXT", nullable = false)
  - timestamp: Instant (nullable = false)
  - serviceName: String (length = 200, nullable = false)
  - correlationId: String (length = 100)
  - traceId: String (length = 100)
  - spanId: String (length = 100)
  - loggerName: String (length = 500)
  - threadName: String (length = 200)
  - exceptionClass: String (length = 500)
  - exceptionMessage: String (columnDefinition = "TEXT")
  - stackTrace: String (columnDefinition = "TEXT")
  - customFields: String (columnDefinition = "TEXT")
  - hostName: String (length = 200)
  - ipAddress: String (length = 50)
  - teamId: UUID (nullable = false)

Relationships:
  - source: @ManyToOne(fetch = LAZY) → LogSource (JoinColumn = "source_id")

Indexes: idx_log_entry_team_id, idx_log_entry_service_name, idx_log_entry_level,
         idx_log_entry_timestamp, idx_log_entry_correlation_id, idx_log_entry_trace_id
```

---

### LogSource.java
```
Table: log_sources
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - description: String (columnDefinition = "TEXT")
  - environment: String (length = 50)
  - serviceId: UUID (cross-reference to CodeOps-Registry)
  - isActive: Boolean (nullable = false, default = true)
  - logCount: Long (nullable = false, default = 0)
  - lastLogReceivedAt: Instant
  - teamId: UUID (nullable = false)

Indexes: idx_log_source_team_id, idx_log_source_service_id
```

---

### LogTrap.java
```
Table: log_traps
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - description: String (columnDefinition = "TEXT")
  - trapType: TrapType @Enumerated(STRING) (nullable = false)
  - isActive: Boolean (nullable = false, default = true)
  - triggerCount: Long (nullable = false, default = 0)
  - lastTriggeredAt: Instant
  - teamId: UUID (nullable = false)
  - createdBy: UUID (nullable = false)

Relationships:
  - conditions: @OneToMany(mappedBy = "trap", cascade = ALL, orphanRemoval = true) → List<TrapCondition>

Indexes: idx_log_trap_team_id, idx_log_trap_is_active
```

---

### TrapCondition.java
```
Table: trap_conditions
Primary Key: id UUID (inherited)

Fields:
  - conditionType: ConditionType @Enumerated(STRING) (nullable = false) [REGEX, KEYWORD, FREQUENCY_THRESHOLD, ABSENCE]
  - field: String (length = 200)
  - pattern: String (length = 1000)
  - threshold: Integer
  - windowSeconds: Integer
  - serviceName: String (length = 200)
  - logLevel: LogLevel @Enumerated(STRING)

Relationships:
  - trap: @ManyToOne(fetch = LAZY) → LogTrap (JoinColumn = "trap_id", nullable = false)

Indexes: idx_trap_condition_trap_id
```

---

### AlertChannel.java
```
Table: alert_channels
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - channelType: AlertChannelType @Enumerated(STRING) (nullable = false) [EMAIL, WEBHOOK, TEAMS, SLACK]
  - configuration: String (columnDefinition = "TEXT", nullable = false)
  - isActive: Boolean (nullable = false, default = true)
  - teamId: UUID (nullable = false)
  - createdBy: UUID (nullable = false)

Indexes: idx_alert_channel_team_id, idx_alert_channel_type
```

---

### AlertRule.java
```
Table: alert_rules
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - severity: AlertSeverity @Enumerated(STRING) (nullable = false) [INFO, WARNING, CRITICAL]
  - isActive: Boolean (nullable = false, default = true)
  - throttleMinutes: Integer (nullable = false, default = 5)
  - teamId: UUID (nullable = false)

Relationships:
  - trap: @ManyToOne(fetch = LAZY) → LogTrap (JoinColumn = "trap_id", nullable = false)
  - channel: @ManyToOne(fetch = LAZY) → AlertChannel (JoinColumn = "channel_id", nullable = false)

Indexes: idx_alert_rule_team_id, idx_alert_rule_trap_id, idx_alert_rule_channel_id
```

---

### AlertHistory.java
```
Table: alert_history
Primary Key: id UUID (inherited)

Fields:
  - severity: AlertSeverity @Enumerated(STRING) (nullable = false)
  - status: AlertStatus @Enumerated(STRING) (nullable = false)
  - message: String (columnDefinition = "TEXT")
  - acknowledgedBy: UUID
  - acknowledgedAt: Instant
  - resolvedBy: UUID
  - resolvedAt: Instant
  - teamId: UUID (nullable = false)

Relationships:
  - rule: @ManyToOne(fetch = LAZY) → AlertRule (JoinColumn = "rule_id", nullable = false)
  - trap: @ManyToOne(fetch = LAZY) → LogTrap (JoinColumn = "trap_id", nullable = false)
  - channel: @ManyToOne(fetch = LAZY) → AlertChannel (JoinColumn = "channel_id", nullable = false)

Indexes: idx_alert_history_team_id, idx_alert_history_status, idx_alert_history_rule_id,
         idx_alert_history_created_at
```

---

### Metric.java
```
Table: metrics
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - metricType: MetricType @Enumerated(STRING) (nullable = false)
  - description: String (columnDefinition = "TEXT")
  - unit: String (length = 50)
  - serviceName: String (length = 200, nullable = false)
  - tags: String (length = 1000)
  - teamId: UUID (nullable = false)

Unique Constraints: uk_metric_name_service_team on (name, service_name, team_id)
Indexes: idx_metric_team_id, idx_metric_service_name
```

---

### MetricSeries.java
```
Table: metric_series
Primary Key: id UUID (inherited)

Fields:
  - timestamp: Instant (nullable = false)
  - value: Double (nullable = false)
  - tags: String (length = 1000)
  - resolution: Integer

Relationships:
  - metric: @ManyToOne(fetch = LAZY) → Metric (JoinColumn = "metric_id", nullable = false)

Indexes: idx_metric_series_metric_id, idx_metric_series_timestamp
```

---

### Dashboard.java
```
Table: dashboards
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - description: String (columnDefinition = "TEXT")
  - isShared: Boolean (nullable = false, default = false)
  - isTemplate: Boolean (nullable = false, default = false)
  - refreshIntervalSeconds: Integer (default = 30)
  - layoutJson: String (columnDefinition = "TEXT")
  - teamId: UUID (nullable = false)
  - createdBy: UUID (nullable = false)

Relationships:
  - widgets: @OneToMany(mappedBy = "dashboard", cascade = ALL, orphanRemoval = true) → List<DashboardWidget>

Indexes: idx_dashboard_team_id, idx_dashboard_created_by
```

---

### DashboardWidget.java
```
Table: dashboard_widgets
Primary Key: id UUID (inherited)

Fields:
  - title: String (length = 200, nullable = false)
  - widgetType: WidgetType @Enumerated(STRING) (nullable = false)
  - queryJson: String (columnDefinition = "TEXT")
  - configJson: String (columnDefinition = "TEXT")
  - gridX: Integer (default = 0)
  - gridY: Integer (default = 0)
  - gridWidth: Integer (default = 6)
  - gridHeight: Integer (default = 4)
  - sortOrder: Integer (default = 0)

Relationships:
  - dashboard: @ManyToOne(fetch = LAZY) → Dashboard (JoinColumn = "dashboard_id", nullable = false)

Indexes: idx_dashboard_widget_dashboard_id
```

---

### TraceSpan.java
```
Table: trace_spans
Primary Key: id UUID (inherited)

Fields:
  - correlationId: String (length = 100, nullable = false)
  - traceId: String (length = 100)
  - spanId: String (length = 100, nullable = false)
  - parentSpanId: String (length = 100)
  - serviceName: String (length = 200, nullable = false)
  - operationName: String (length = 500, nullable = false)
  - startTime: Instant (nullable = false)
  - endTime: Instant
  - durationMs: Long
  - status: SpanStatus @Enumerated(STRING) (nullable = false, default = OK)
  - statusMessage: String (columnDefinition = "TEXT")
  - tags: String (length = 1000)
  - teamId: UUID (nullable = false)

Indexes: idx_trace_span_team_id, idx_trace_span_correlation_id, idx_trace_span_trace_id,
         idx_trace_span_service_name, idx_trace_span_start_time

Note: String-based correlationId/traceId linking (no JPA FK to LogEntry)
```

---

### RetentionPolicy.java
```
Table: retention_policies
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - sourceName: String (length = 200)
  - logLevel: LogLevel @Enumerated(STRING)
  - retentionDays: Integer (nullable = false)
  - action: RetentionAction @Enumerated(STRING) (nullable = false)
  - archiveDestination: String (length = 500)
  - isActive: Boolean (nullable = false, default = true)
  - lastExecutedAt: Instant
  - teamId: UUID (nullable = false)
  - createdBy: UUID (nullable = false)

Indexes: idx_retention_policy_team_id, idx_retention_policy_is_active
```

---

### AnomalyBaseline.java
```
Table: anomaly_baselines
Primary Key: id UUID (inherited)

Fields:
  - serviceName: String (length = 200, nullable = false)
  - metricName: String (length = 200, nullable = false)
  - baselineValue: Double (nullable = false)
  - standardDeviation: Double (nullable = false)
  - sampleCount: Long (nullable = false)
  - windowStartTime: Instant (nullable = false)
  - windowEndTime: Instant (nullable = false)
  - deviationThreshold: Double (nullable = false, default = 2.0)
  - isActive: Boolean (nullable = false, default = true)
  - teamId: UUID (nullable = false)
  - lastComputedAt: Instant

Indexes: idx_anomaly_baseline_team_id, idx_anomaly_baseline_service_name,
         idx_anomaly_baseline_service_metric (composite: service_name, metric_name)
```

---

### SavedQuery.java
```
Table: saved_queries
Primary Key: id UUID (inherited)

Fields:
  - name: String (length = 200, nullable = false)
  - description: String (columnDefinition = "TEXT")
  - queryJson: String (columnDefinition = "TEXT", nullable = false)
  - queryDsl: String (columnDefinition = "TEXT")
  - teamId: UUID (nullable = false)
  - createdBy: UUID (nullable = false)
  - isShared: Boolean (nullable = false, default = false)
  - lastExecutedAt: Instant
  - executionCount: Long (nullable = false, default = 0)

Indexes: idx_saved_query_team_id, idx_saved_query_created_by
```

---

### QueryHistory.java
```
Table: query_history
Primary Key: id UUID (inherited)

Fields:
  - queryJson: String (columnDefinition = "TEXT", nullable = false)
  - queryDsl: String (columnDefinition = "TEXT")
  - resultCount: Long (nullable = false)
  - executionTimeMs: Long (nullable = false)
  - teamId: UUID (nullable = false)
  - createdBy: UUID (nullable = false)

Indexes: idx_query_history_team_id, idx_query_history_created_by, idx_query_history_created_at
```

---

### Entity Relationship Summary
```
LogEntry --[ManyToOne]--> LogSource (via source / source_id)
LogTrap --[OneToMany]--> TrapCondition (via conditions, cascade ALL, orphanRemoval)
TrapCondition --[ManyToOne]--> LogTrap (via trap / trap_id)
AlertRule --[ManyToOne]--> LogTrap (via trap / trap_id)
AlertRule --[ManyToOne]--> AlertChannel (via channel / channel_id)
AlertHistory --[ManyToOne]--> AlertRule (via rule / rule_id)
AlertHistory --[ManyToOne]--> LogTrap (via trap / trap_id)
AlertHistory --[ManyToOne]--> AlertChannel (via channel / channel_id)
MetricSeries --[ManyToOne]--> Metric (via metric / metric_id)
Dashboard --[OneToMany]--> DashboardWidget (via widgets, cascade ALL, orphanRemoval)
DashboardWidget --[ManyToOne]--> Dashboard (via dashboard / dashboard_id)
TraceSpan --[string link]--> LogEntry (via correlationId, no JPA FK)
```

---

## 7. Enum Definitions

### LogLevel.java
```
Values: TRACE, DEBUG, INFO, WARN, ERROR, FATAL
Used By: LogEntry.level, TrapCondition.logLevel, RetentionPolicy.logLevel
```

### AlertSeverity.java
```
Values: INFO, WARNING, CRITICAL
Used By: AlertRule.severity, AlertHistory.severity
```

### AlertStatus.java
```
Values: FIRED, ACKNOWLEDGED, RESOLVED
Used By: AlertHistory.status
```

### AlertChannelType.java
```
Values: EMAIL, WEBHOOK, TEAMS, SLACK
Used By: AlertChannel.channelType
```

### ConditionType.java
```
Values: REGEX, KEYWORD, FREQUENCY_THRESHOLD, ABSENCE
Used By: TrapCondition.conditionType
```

### MetricType.java
```
Values: COUNTER, GAUGE, HISTOGRAM, TIMER
Used By: Metric.metricType
```

### RetentionAction.java
```
Values: PURGE, ARCHIVE
Used By: RetentionPolicy.action
```

### SpanStatus.java
```
Values: OK, ERROR
Used By: TraceSpan.status
```

### TrapType.java
```
Values: PATTERN, FREQUENCY, ABSENCE
Used By: LogTrap.trapType
```

### WidgetType.java
```
Values: LOG_STREAM, TIME_SERIES_CHART, COUNTER, GAUGE, TABLE, HEATMAP, PIE_CHART, BAR_CHART
Used By: DashboardWidget.widgetType
```

---

## 8. Repository Layer

### LogEntryRepository.java
```
Extends: JpaRepository<LogEntry, UUID>

Paginated Methods:
  - Page<LogEntry> findByTeamId(UUID, Pageable)
  - Page<LogEntry> findByTeamIdAndServiceName(UUID, String, Pageable)
  - Page<LogEntry> findByTeamIdAndLevel(UUID, LogLevel, Pageable)
  - Page<LogEntry> findByTeamIdAndServiceNameAndLevel(UUID, String, LogLevel, Pageable)
  - Page<LogEntry> findByTeamIdAndTimestampBetween(UUID, Instant, Instant, Pageable)
  - Page<LogEntry> findByTeamIdAndServiceNameAndTimestampBetween(UUID, String, Instant, Instant, Pageable)
  - Page<LogEntry> findByTeamIdAndLevelAndTimestampBetween(UUID, LogLevel, Instant, Instant, Pageable)
  - Page<LogEntry> findByTeamIdAndCorrelationId(UUID, String, Pageable)

Derived Query Methods:
  - List<LogEntry> findByCorrelationIdOrderByTimestampAsc(String)

Count Methods:
  - long countByTeamIdAndLevel(UUID, LogLevel)
  - long countByTeamIdAndServiceNameAndLevelAndTimestampBetween(UUID, String, LogLevel, Instant, Instant)
  - long countByTeamIdAndTimestampBetween(UUID, Instant, Instant)
  - long countByTeamIdAndServiceNameAndTimestampBetween(UUID, String, Instant, Instant)

Delete Methods:
  - void deleteByTimestampBefore(Instant)
  - void deleteByTeamIdAndTimestampBefore(UUID, Instant)
  - void deleteByTeamIdAndServiceNameAndTimestampBefore(UUID, String, Instant)
  - @Modifying @Query void deleteByTeamIdAndTimestampBeforeAndLevelIn(UUID, Instant, List<LogLevel>)
  - @Modifying @Query void deleteByTeamIdAndServiceNameAndTimestampBeforeAndLevelIn(UUID, String, Instant, List<LogLevel>)

Aggregate Methods:
  - @Query List<Object[]> countGroupByServiceName()
  - @Query List<Object[]> countGroupByLevel()
  - @Query Optional<Instant> findOldestTimestamp()
  - @Query Optional<Instant> findNewestTimestamp()
```

### LogSourceRepository.java
```
Extends: JpaRepository<LogSource, UUID>

Methods:
  - List<LogSource> findByTeamId(UUID)
  - Page<LogSource> findByTeamId(UUID, Pageable)
  - List<LogSource> findByTeamIdAndIsActiveTrue(UUID)
  - Optional<LogSource> findByTeamIdAndName(UUID, String)
  - Optional<LogSource> findByServiceId(UUID)
  - boolean existsByTeamIdAndName(UUID, String)
  - long countByTeamId(UUID)
```

### LogTrapRepository.java
```
Extends: JpaRepository<LogTrap, UUID>

Methods:
  - List<LogTrap> findByTeamId(UUID)
  - Page<LogTrap> findByTeamId(UUID, Pageable)
  - List<LogTrap> findByTeamIdAndIsActiveTrue(UUID)
  - List<LogTrap> findByTrapTypeAndIsActiveTrue(TrapType)
  - long countByTeamId(UUID)
```

### TrapConditionRepository.java
```
Extends: JpaRepository<TrapCondition, UUID>

Methods:
  - List<TrapCondition> findByTrapId(UUID)
  - void deleteByTrapId(UUID)
```

### AlertChannelRepository.java
```
Extends: JpaRepository<AlertChannel, UUID>

Methods:
  - List<AlertChannel> findByTeamId(UUID)
  - Page<AlertChannel> findByTeamId(UUID, Pageable)
  - List<AlertChannel> findByTeamIdAndIsActiveTrue(UUID)
  - List<AlertChannel> findByTeamIdAndChannelType(UUID, AlertChannelType)
  - long countByTeamId(UUID)
```

### AlertRuleRepository.java
```
Extends: JpaRepository<AlertRule, UUID>

Methods:
  - List<AlertRule> findByTeamId(UUID)
  - Page<AlertRule> findByTeamId(UUID, Pageable)
  - List<AlertRule> findByTrapId(UUID)
  - List<AlertRule> findByTrapIdAndIsActiveTrue(UUID)
  - List<AlertRule> findByChannelId(UUID)
  - long countByTeamId(UUID)
```

### AlertHistoryRepository.java
```
Extends: JpaRepository<AlertHistory, UUID>

Methods:
  - Page<AlertHistory> findByTeamIdOrderByCreatedAtDesc(UUID, Pageable)
  - Page<AlertHistory> findByTeamIdAndStatus(UUID, AlertStatus, Pageable)
  - Page<AlertHistory> findByTeamIdAndSeverity(UUID, AlertSeverity, Pageable)
  - Page<AlertHistory> findByRuleId(UUID, Pageable)
  - List<AlertHistory> findByTeamIdAndStatusAndCreatedAtAfter(UUID, AlertStatus, Instant)
  - long countByTeamIdAndStatus(UUID, AlertStatus)
  - long countByTeamIdAndSeverityAndStatus(UUID, AlertSeverity, AlertStatus)
  - boolean existsByRuleIdAndCreatedAtAfter(UUID, Instant)
```

### MetricRepository.java
```
Extends: JpaRepository<Metric, UUID>

Methods:
  - List<Metric> findByTeamId(UUID)
  - Page<Metric> findByTeamId(UUID, Pageable)
  - List<Metric> findByTeamIdAndServiceName(UUID, String)
  - Optional<Metric> findByTeamIdAndNameAndServiceName(UUID, String, String)
  - List<Metric> findByTeamIdAndMetricType(UUID, MetricType)
  - long countByTeamId(UUID)
```

### MetricSeriesRepository.java
```
Extends: JpaRepository<MetricSeries, UUID>

Methods:
  - List<MetricSeries> findByMetricIdAndTimestampBetweenOrderByTimestampAsc(UUID, Instant, Instant)
  - Page<MetricSeries> findByMetricId(UUID, Pageable)
  - void deleteByTimestampBefore(Instant)
  - void deleteByMetricId(UUID)
  - long countByMetricId(UUID)
  - @Query Optional<Double> findAverageValueByMetricIdAndTimestampBetween(UUID, Instant, Instant)
  - @Query Optional<Double> findMaxValueByMetricIdAndTimestampBetween(UUID, Instant, Instant)
  - @Query Optional<Double> findMinValueByMetricIdAndTimestampBetween(UUID, Instant, Instant)
```

### DashboardRepository.java
```
Extends: JpaRepository<Dashboard, UUID>

Methods:
  - List<Dashboard> findByTeamId(UUID)
  - Page<Dashboard> findByTeamId(UUID, Pageable)
  - List<Dashboard> findByTeamIdAndIsSharedTrue(UUID)
  - List<Dashboard> findByTeamIdAndIsTemplateTrue(UUID)
  - List<Dashboard> findByCreatedBy(UUID)
  - long countByTeamId(UUID)
```

### DashboardWidgetRepository.java
```
Extends: JpaRepository<DashboardWidget, UUID>

Methods:
  - List<DashboardWidget> findByDashboardIdOrderBySortOrderAsc(UUID)
  - void deleteByDashboardId(UUID)
  - long countByDashboardId(UUID)
```

### TraceSpanRepository.java
```
Extends: JpaRepository<TraceSpan, UUID>

Methods:
  - List<TraceSpan> findByCorrelationIdOrderByStartTimeAsc(String)
  - List<TraceSpan> findByTraceIdOrderByStartTimeAsc(String)
  - Page<TraceSpan> findByTeamId(UUID, Pageable)
  - Page<TraceSpan> findByTeamIdAndServiceName(UUID, String, Pageable)
  - List<TraceSpan> findByTeamIdAndStartTimeBetween(UUID, Instant, Instant)
  - List<TraceSpan> findByTeamIdAndServiceNameAndStatus(UUID, String, SpanStatus)
  - void deleteByStartTimeBefore(Instant)
  - long countByTeamId(UUID)
```

### RetentionPolicyRepository.java
```
Extends: JpaRepository<RetentionPolicy, UUID>

Methods:
  - List<RetentionPolicy> findByTeamId(UUID)
  - Page<RetentionPolicy> findByTeamId(UUID, Pageable)
  - List<RetentionPolicy> findByIsActiveTrue()
  - List<RetentionPolicy> findByTeamIdAndIsActiveTrue(UUID)
  - long countByTeamId(UUID)
```

### AnomalyBaselineRepository.java
```
Extends: JpaRepository<AnomalyBaseline, UUID>

Methods:
  - List<AnomalyBaseline> findByTeamId(UUID)
  - Page<AnomalyBaseline> findByTeamId(UUID, Pageable)
  - List<AnomalyBaseline> findByTeamIdAndServiceName(UUID, String)
  - Optional<AnomalyBaseline> findByTeamIdAndServiceNameAndMetricName(UUID, String, String)
  - List<AnomalyBaseline> findByIsActiveTrue()
  - List<AnomalyBaseline> findByTeamIdAndIsActiveTrue(UUID)
  - long countByTeamId(UUID)
```

### SavedQueryRepository.java
```
Extends: JpaRepository<SavedQuery, UUID>

Methods:
  - List<SavedQuery> findByTeamId(UUID)
  - Page<SavedQuery> findByTeamId(UUID, Pageable)
  - List<SavedQuery> findByCreatedBy(UUID)
  - List<SavedQuery> findByTeamIdAndIsSharedTrue(UUID)
  - boolean existsByTeamIdAndName(UUID, String)
```

### QueryHistoryRepository.java
```
Extends: JpaRepository<QueryHistory, UUID>

Methods:
  - Page<QueryHistory> findByCreatedByOrderByCreatedAtDesc(UUID, Pageable)
  - Page<QueryHistory> findByTeamIdOrderByCreatedAtDesc(UUID, Pageable)
  - void deleteByCreatedAtBefore(Instant)
```

---

## 9. Service Layer

### LogIngestionService.java
```
Injected Dependencies: LogEntryRepository, LogSourceRepository, LogEntryMapper, LogParsingService, ApplicationEventPublisher

Methods:
  ─── ingest(IngestLogEntryRequest, UUID teamId) → LogEntryResponse
      Purpose: Ingest a single log entry
      Authorization: NONE (team scoping via parameter)
      Logic: Validates level → resolves/auto-creates LogSource → maps to entity → saves → publishes LogEntryIngestedEvent → updates source stats (logCount, lastLogReceivedAt)
      Throws: ValidationException (invalid level)
      @Transactional

  ─── ingestBatch(List<IngestLogEntryRequest>, UUID teamId) → int
      Purpose: Batch ingest log entries
      Authorization: NONE
      Logic: Validates MAX_BATCH_SIZE → processes each entry (per-entry failures logged at WARN, don't fail batch) → caches resolved sources → publishes events for all saved entries
      Throws: ValidationException (batch size exceeded)
      @Transactional

  ─── ingestRaw(String rawLog, String defaultServiceName, UUID teamId)
      Purpose: Parse and ingest a raw log line
      Authorization: NONE
      Logic: Delegates to LogParsingService for parsing → calls ingest(). Catches all exceptions at WARN.
```

### LogParsingService.java
```
Injected Dependencies: None (stateless)

Methods:
  ─── parse(String rawLog, String defaultServiceName) → IngestLogEntryRequest
      Purpose: Parse a raw log line into a structured IngestLogEntryRequest
      Authorization: NONE
      Logic: Tries multiple regex patterns (common formats), falls back to unparsed entry with level INFO
```

### LogQueryService.java
```
Injected Dependencies: LogEntryRepository, SavedQueryRepository, QueryHistoryRepository, LogEntryMapper, SavedQueryMapper, QueryHistoryMapper, ObjectMapper, EntityManager, LogQueryDslParser

Methods:
  ─── getLogEntry(UUID logEntryId) → LogEntryResponse
      Purpose: Single log entry lookup
      Throws: NotFoundException

  ─── query(LogQueryRequest, UUID teamId, UUID userId) → PageResponse<LogEntryResponse>
      Purpose: Structured query with field-level filters
      Logic: Builds JPA Criteria predicates for serviceName, level, timeRange, correlationId, loggerName, exceptionClass, hostName, free-text query → records query history
      @Transactional(readOnly = true)

  ─── search(String searchTerm, UUID teamId, Instant start, Instant end, int page, int size) → PageResponse<LogEntryResponse>
      Purpose: Full-text LIKE search across message, loggerName, exceptionClass, exceptionMessage, customFields
      Throws: ValidationException (empty search term)

  ─── executeDsl(String dslQuery, UUID teamId, UUID userId, int page, int size) → PageResponse<LogEntryResponse>
      Purpose: Parse and execute DSL query (SQL-like syntax)
      Logic: Delegates parsing to LogQueryDslParser → builds Criteria → records history
      Throws: ValidationException (syntax errors)

  ─── saveQuery(CreateSavedQueryRequest, UUID teamId, UUID userId) → SavedQueryResponse
  ─── getSavedQueries(UUID teamId, UUID userId) → List<SavedQueryResponse>
      Logic: Returns user's own + shared team queries (deduplicated)
  ─── getSavedQuery(UUID queryId) → SavedQueryResponse
  ─── updateSavedQuery(UUID queryId, UpdateSavedQueryRequest, UUID userId) → SavedQueryResponse
      Authorization: Only query owner can update (AuthorizationException)
  ─── deleteSavedQuery(UUID queryId, UUID userId)
      Authorization: Only query owner can delete (AuthorizationException)
  ─── executeSavedQuery(UUID queryId, UUID teamId, UUID userId, int page, int size) → PageResponse<LogEntryResponse>
      Logic: Executes saved DSL or structured query, updates executionCount + lastExecutedAt
  ─── getQueryHistory(UUID userId, int page, int size) → PageResponse<QueryHistoryResponse>
```

### LogQueryDslParser.java
```
Injected Dependencies: None (stateless)

Methods:
  ─── parse(String dslQuery) → List<Predicate>
      Purpose: Parse SQL-like DSL query string into JPA Criteria predicates
      Logic: Tokenizes query → parses AND/OR logic → maps field:value operators
```

### LogSourceService.java
```
Injected Dependencies: LogSourceRepository, LogSourceMapper

Methods:
  ─── createSource(CreateLogSourceRequest, UUID teamId) → LogSourceResponse
      Throws: ValidationException (duplicate team+name)
  ─── getSourcesByTeam(UUID teamId) → List<LogSourceResponse>
  ─── getSourcesByTeamPaged(UUID teamId, int page, int size) → PageResponse<LogSourceResponse>
  ─── getSource(UUID sourceId) → LogSourceResponse
  ─── updateSource(UUID sourceId, UpdateLogSourceRequest) → LogSourceResponse
  ─── deleteSource(UUID sourceId)
```

### LogTrapService.java
```
Injected Dependencies: LogTrapRepository, TrapConditionRepository, LogTrapMapper, TrapEvaluationEngine

Methods:
  ─── createTrap(CreateLogTrapRequest, UUID teamId, UUID userId) → LogTrapResponse
      @Transactional
  ─── getTrapsByTeam(UUID teamId) → List<LogTrapResponse>
  ─── getTrapsByTeamPaged(UUID teamId, int page, int size) → PageResponse<LogTrapResponse>
  ─── getTrap(UUID trapId) → LogTrapResponse
  ─── updateTrap(UUID trapId, UpdateLogTrapRequest) → LogTrapResponse
      @Transactional
  ─── deleteTrap(UUID trapId)
  ─── addCondition(UUID trapId, CreateTrapConditionRequest) → LogTrapResponse
      @Transactional
  ─── removeCondition(UUID trapId, UUID conditionId) → LogTrapResponse
      @Transactional
  ─── testTrap(UUID trapId, TestTrapRequest) → TrapTestResult
      Purpose: Test a trap against recent log entries
  ─── evaluateEntry(LogEntry entry) → List<UUID>
      Purpose: Evaluate a log entry against all active traps for the entry's team
      Logic: Finds active traps for team → evaluates each via TrapEvaluationEngine → updates trigger stats → returns IDs of fired traps
      @Transactional
```

### TrapEvaluationEngine.java
```
Injected Dependencies: LogEntryRepository

Methods:
  ─── evaluate(LogTrap, LogEntry) → boolean
      Purpose: Evaluate whether a log entry triggers a trap
      Logic: Routes by TrapType → SINGLE_MATCH: all conditions must match → AGGREGATION: delegates to evaluateAggregation
  ─── evaluateCondition(TrapCondition, LogEntry) → boolean
      Logic: PATTERN_MATCH: regex on field → THRESHOLD: count matching logs in window → FREQUENCY: similar to threshold → RATE: rate of matching logs in window
```

### AlertService.java
```
Injected Dependencies: AlertRuleRepository, AlertHistoryRepository, LogTrapRepository, AlertChannelRepository, AlertRuleMapper, AlertHistoryMapper, AlertChannelService

Methods:
  ─── createRule(CreateAlertRuleRequest, UUID teamId) → AlertRuleResponse
  ─── getRulesByTeam(UUID teamId) → List<AlertRuleResponse>
  ─── getRulesByTeamPaged(UUID teamId, int page, int size) → PageResponse<AlertRuleResponse>
  ─── getRulesByTrap(UUID trapId) → List<AlertRuleResponse>
  ─── getRule(UUID ruleId) → AlertRuleResponse
  ─── updateRule(UUID ruleId, UpdateAlertRuleRequest) → AlertRuleResponse
  ─── deleteRule(UUID ruleId)
  ─── fireAlerts(UUID trapId, String triggerMessage)
      Purpose: Fire alerts for a triggered trap
      Logic: Finds active rules for trap → applies throttle check → creates AlertHistory (FIRED) → dispatches via AlertChannelService
      @Transactional
  ─── acknowledgeAlert(UUID alertId, UUID userId) → AlertHistoryResponse
      Throws: ValidationException (already resolved)
  ─── resolveAlert(UUID alertId, UUID userId) → AlertHistoryResponse
      Logic: Auto-acknowledges if not already
  ─── updateAlertStatus(UUID alertId, UpdateAlertStatusRequest, UUID userId) → AlertHistoryResponse
  ─── getAlertHistory(UUID teamId, int page, int size) → PageResponse<AlertHistoryResponse>
  ─── getAlertHistoryByStatus(UUID teamId, AlertStatus, int page, int size) → PageResponse<AlertHistoryResponse>
  ─── getAlertHistoryBySeverity(UUID teamId, AlertSeverity, int page, int size) → PageResponse<AlertHistoryResponse>
  ─── getAlertHistoryByRule(UUID ruleId, int page, int size) → PageResponse<AlertHistoryResponse>
  ─── getActiveAlertCounts(UUID teamId) → Map<String, Long>
      Purpose: Count FIRED + ACKNOWLEDGED alerts by severity
```

### AlertChannelService.java
```
Injected Dependencies: AlertChannelRepository, AlertChannelMapper, RestTemplate

Methods:
  ─── createChannel(CreateAlertChannelRequest, UUID teamId, UUID userId) → AlertChannelResponse
      Logic: Validates WEBHOOK URLs via validateWebhookUrl()
  ─── getChannelsByTeam(UUID teamId) → List<AlertChannelResponse>
  ─── getChannelsByTeamPaged(UUID teamId, int page, int size) → PageResponse<AlertChannelResponse>
  ─── getChannel(UUID channelId) → AlertChannelResponse
  ─── updateChannel(UUID channelId, UpdateAlertChannelRequest) → AlertChannelResponse
  ─── deleteChannel(UUID channelId)
  ─── deliverNotification(AlertChannel, String message)
      Purpose: Dispatch notification to channel (EMAIL/WEBHOOK/SLACK/MICROSOFT_TEAMS)
      Logic: Routes by channelType → WEBHOOK: POST to validated URL → SLACK/TEAMS: POST to webhook → EMAIL: logs (not implemented)
      Authorization: SSRF protection via validateWebhookUrl() (blocks private, loopback, link-local ranges)
```

### MetricsService.java
```
Injected Dependencies: MetricRepository, MetricSeriesRepository, MetricMapper, MetricAggregationService

Methods:
  ─── registerMetric(RegisterMetricRequest, UUID teamId) → MetricResponse
      Logic: Idempotent — returns existing if same name/service/team
  ─── getMetricsByTeam/getMetricsByTeamPaged/getMetricsByService/getMetric
  ─── getServiceMetricsSummary(UUID teamId, String serviceName) → ServiceMetricsSummaryResponse
  ─── updateMetric(UUID metricId, UpdateMetricRequest) → MetricResponse
  ─── deleteMetric(UUID metricId) — deletes metric + all series data
  ─── pushMetricData(PushMetricDataRequest, UUID teamId) → int
      Authorization: Validates metric belongs to teamId (AuthorizationException)
      @Transactional
  ─── pushSingleValue(String metricName, String metricType, String serviceName, double value, UUID teamId)
      Logic: Auto-registers + pushes single point
  ─── getTimeSeries(UUID metricId, Instant start, Instant end) → MetricTimeSeriesResponse
  ─── getTimeSeriesAggregated(UUID metricId, Instant start, Instant end, int resolutionSeconds) → MetricTimeSeriesResponse
  ─── getAggregation(UUID metricId, Instant start, Instant end) → MetricAggregationResponse
      Logic: Full stats (sum, avg, min, max, p50, p95, p99, stddev)
  ─── getLatestValue(UUID metricId) → Optional<MetricDataPointResponse>
  ─── getLatestValuesByService(UUID teamId, String serviceName) → Map<String, Double>
  ─── purgeOldData(Instant cutoff) → long
```

### MetricAggregationService.java
```
Injected Dependencies: None (stateless)

Methods:
  ─── aggregate(List<Double>) → AggregationResult
      Purpose: Compute count, sum, avg, min, max, p50, p95, p99, stddev
  ─── aggregateByResolution(List<MetricSeries>, Instant start, Instant end, int resolutionSeconds) → List<DataPoint>
      Purpose: Bucket data points into time windows, compute average per bucket

Inner Record: AggregationResult(count, sum, avg, min, max, p50, p95, p99, stddev)
```

### TraceService.java
```
Injected Dependencies: TraceSpanRepository, LogEntryRepository, TraceSpanMapper, TraceAnalysisService

Methods:
  ─── createSpan(CreateTraceSpanRequest, UUID teamId) → TraceSpanResponse
  ─── createSpanBatch(List<CreateTraceSpanRequest>, UUID teamId) → List<TraceSpanResponse>
  ─── getSpan(UUID spanId) → TraceSpanResponse
  ─── getTraceFlow(String correlationId) → TraceFlowResponse
  ─── getTraceFlowByTraceId(String traceId) → TraceFlowResponse
  ─── getWaterfall(String correlationId) → TraceWaterfallResponse
      Logic: Includes related log entries mapped to spans
  ─── getRootCauseAnalysis(String correlationId) → Optional<RootCauseAnalysisResponse>
  ─── listRecentTraces(UUID teamId, int page, int size) → PageResponse<TraceListResponse>
      Logic: Groups spans by correlationId, builds summaries, manual pagination
  ─── listTracesByService(UUID teamId, String serviceName, int page, int size) → PageResponse<TraceListResponse>
  ─── listErrorTraces(UUID teamId, int limit) → List<TraceListResponse>
  ─── getRelatedLogEntries(String correlationId) → List<UUID>
  ─── purgeOldSpans(Instant cutoff)
```

### TraceAnalysisService.java
```
Injected Dependencies: TraceSpanMapper

Methods:
  ─── buildWaterfall(List<TraceSpan>, Map<String, List<UUID>>) → TraceWaterfallResponse
      Purpose: Sort spans, calculate depth via BFS, compute timing offsets from root
  ─── analyzeRootCause(List<TraceSpan>, List<UUID>) → RootCauseAnalysisResponse
      Purpose: Find earliest ERROR span, build error propagation chain via BFS, count impacted services
  ─── buildTraceSummary(List<TraceSpan>) → TraceListResponse
      Purpose: Lightweight summary for list views
```

### DashboardService.java
```
Injected Dependencies: DashboardRepository, DashboardWidgetRepository, DashboardMapper

Methods:
  ─── createDashboard(CreateDashboardRequest, UUID teamId, UUID userId) → DashboardResponse
      Logic: Validates MAX_DASHBOARDS_PER_TEAM
  ─── getDashboardsByTeam/getDashboardsByTeamPaged/getSharedDashboards/getDashboardsByUser/getDashboard
  ─── updateDashboard(UUID dashboardId, UpdateDashboardRequest) → DashboardResponse
  ─── deleteDashboard(UUID dashboardId) — cascade deletes widgets
  ─── addWidget(UUID dashboardId, CreateDashboardWidgetRequest) → DashboardWidgetResponse
      Logic: Validates MAX_WIDGETS_PER_DASHBOARD, auto-positions if grid coords not provided
  ─── updateWidget(UUID dashboardId, UUID widgetId, UpdateDashboardWidgetRequest) → DashboardWidgetResponse
  ─── removeWidget(UUID dashboardId, UUID widgetId) — re-numbers sort orders
  ─── reorderWidgets(UUID dashboardId, List<UUID> widgetIds) → DashboardResponse
      Logic: Validates IDs match exactly
  ─── updateLayout(UUID dashboardId, List<WidgetPositionUpdate>) → DashboardResponse
  ─── markAsTemplate(UUID dashboardId) / unmarkAsTemplate(UUID dashboardId) / getTemplates(UUID teamId)
  ─── createFromTemplate(UUID templateId, String name, UUID teamId, UUID userId) → DashboardResponse
      Logic: Deep clones dashboard + widgets
  ─── duplicateDashboard(UUID dashboardId, String newName, UUID teamId, UUID userId) → DashboardResponse
```

### RetentionService.java
```
Injected Dependencies: RetentionPolicyRepository, RetentionPolicyMapper, LogEntryRepository, MetricSeriesRepository, TraceSpanRepository
Class-level: @Transactional

Methods:
  ─── createPolicy(CreateRetentionPolicyRequest, UUID teamId, UUID userId) → RetentionPolicyResponse
      Throws: ValidationException (ARCHIVE action requires destination)
  ─── updatePolicy(UUID policyId, UpdateRetentionPolicyRequest, UUID teamId) → RetentionPolicyResponse
      Authorization: Validates teamId matches policy's team
  ─── getPolicy(UUID policyId, UUID teamId) → RetentionPolicyResponse
      Authorization: Validates teamId (throws NotFoundException to hide existence)
  ─── getPoliciesByTeam(UUID teamId) → List<RetentionPolicyResponse>
  ─── deletePolicy(UUID policyId, UUID teamId)
      Authorization: Validates teamId
  ─── togglePolicyActive(UUID policyId, UUID teamId, boolean active) → RetentionPolicyResponse
  ─── getStorageUsage() → StorageUsageResponse
      Logic: Counts logs, metrics, spans; groups logs by service and level
```

### RetentionExecutor.java
```
Injected Dependencies: RetentionPolicyRepository, LogEntryRepository, MetricSeriesRepository, TraceSpanRepository

Methods:
  ─── executeAllActivePolicies()
      @Scheduled(cron = "0 0 2 * * ?") — Daily at 2:00 AM
      Logic: Iterates all active policies, executes each independently. Per-policy exceptions caught and logged.
      @Transactional
  ─── executePolicy(RetentionPolicy)
      Logic: Computes cutoff from retentionDays. ARCHIVE falls back to PURGE (logs warning). Filters by optional sourceName and logLevel.
  ─── manualExecute(UUID policyId, UUID teamId)
      Authorization: Validates teamId
  ─── globalPurge(int retentionDays)
      Logic: Purges all data (logs, metrics, spans) older than N days. Validates MIN/MAX_RETENTION_DAYS bounds.
```

### AnomalyDetectionService.java
```
Injected Dependencies: AnomalyBaselineRepository, LogEntryRepository, MetricSeriesRepository, MetricRepository, AnomalyBaselineMapper, AnomalyBaselineCalculator
Class-level: @Transactional

Methods:
  ─── createOrUpdateBaseline(CreateBaselineRequest, UUID teamId) → AnomalyBaselineResponse
      Logic: Collects hourly data for window → computes stats. Creates or updates baseline.
      Throws: ValidationException (< 24 hourly data points)
  ─── getBaselinesByTeam / getBaselinesByService / getBaseline
  ─── updateBaseline(UUID baselineId, UpdateBaselineRequest) → AnomalyBaselineResponse
      Logic: Recalculates stats if windowHours changed
  ─── deleteBaseline(UUID baselineId)
  ─── checkAnomaly(UUID teamId, String serviceName, String metricName) → AnomalyCheckResponse
      Logic: Gets current value, computes z-score against baseline
  ─── runFullCheck(UUID teamId) → AnomalyReportResponse
      Logic: Checks all active baselines for team. Individual failures logged but don't stop.
  ─── recalculateAllBaselines()
      @Scheduled(cron = "${codeops.anomaly.recalculation-cron:0 0 3 * * *}") — Daily at 3:00 AM (configurable)
```

### AnomalyBaselineCalculator.java
```
Injected Dependencies: None (stateless @Component)

Methods:
  ─── computeBaseline(List<Double> hourlyValues) → Optional<BaselineStats>
      Logic: Requires minimum 24 data points. Uses STDDEV_EPSILON (0.001) instead of zero stddev.
  ─── calculateZScore(double value, double mean, double stddev) → double
  ─── isAnomaly(double value, double mean, double stddev, double threshold) → boolean
  ─── getDirection(double value, double mean) → String ("ABOVE"/"BELOW"/"NORMAL")

Inner Record: BaselineStats(mean, stddev, sampleCount)
```

### KafkaLogConsumer.java
```
Injected Dependencies: LogIngestionService

Methods:
  ─── consume(ConsumerRecord<String, String>)
      @KafkaListener(topics = "codeops-logs")
      Purpose: Consume log entries from Kafka topic
      Logic: Extracts X-Team-Id from Kafka headers → validates UUID format → deserializes JSON body → delegates to LogIngestionService.ingest()
      Authorization: Validates X-Team-Id header presence and UUID format
```

### LogEntryEventListener.java
```
Injected Dependencies: LogTrapService, AlertService

Methods:
  ─── onLogEntryIngested(LogEntryIngestedEvent)
      @Async @EventListener
      Purpose: Complete event-driven pipeline: Log Ingested → Trap Evaluation → Alert Firing → Notification
      Logic: Evaluates all active traps → for each fired trap, builds trigger message and calls alertService.fireAlerts()
      Exception Handling: Two-level — outer catch for trap evaluation, inner catch per-trap for alert firing
```

---

## 10. Security Architecture

**Authentication Flow:**
- JWT validation only (never issues tokens) — shared HMAC-SHA256 secret with CodeOps-Server
- Token claims extracted: `sub` (userId as UUID), `email` (String), `roles` (List<String>)
- `@PostConstruct` validates JWT secret ≥ 32 chars at startup
- No token revocation or blacklist — stateless JWT only

**Authorization Model:**
- Single role checked: `ADMIN` (via `@PreAuthorize("hasRole('ADMIN')")` on all 10 non-health controllers)
- Team scoping via `X-Team-ID` header (extracted in BaseController, passed to services)
- Service-level authorization: owner checks on saved queries, team checks on retention policies

**Security Filter Chain (order):**
1. `RequestCorrelationFilter` (`@Order(HIGHEST_PRECEDENCE)`) — generates/extracts correlation ID, sets MDC
2. `JwtAuthFilter` — extracts Bearer token, validates, sets SecurityContext (principal=UUID, credentials=email, authorities=ROLE_*)
3. Spring Security built-in filters
4. `LoggingInterceptor` (Spring MVC interceptor) — logs request/response on `/api/**`

**Public paths:** `/api/v1/logger/health`, `/swagger-ui/**`, `/v3/api-docs/**`
**Authenticated paths:** All `/api/**` and everything else

**CORS Configuration:** Via `CorsConfig` bean using `codeops.cors.allowed-origins` property. Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS. Headers: Authorization, Content-Type, X-Team-ID, X-Correlation-ID.

**Security Headers:**
- CSP: `default-src 'self'; frame-ancestors 'none'`
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- HSTS: 1 year, includeSubDomains

**Rate Limiting:** None. No RateLimitFilter or throttle mechanism.

---

## 11. Notification / Messaging Layer

**Webhook notifications:** `AlertChannelService.deliverNotification()` dispatches to EMAIL, WEBHOOK, SLACK, MICROSOFT_TEAMS channels.
- WEBHOOK: POST JSON payload to user-configured URL via RestTemplate
- SLACK: POST to Slack webhook URL (standard Slack payload format)
- MICROSOFT_TEAMS: POST to Teams webhook URL
- EMAIL: Logged only (not implemented — logs "Email delivery not yet implemented")
- **SSRF protection:** `validateWebhookUrl()` blocks private IP ranges (10.x, 172.16-31.x, 192.168.x), loopback (127.x), and link-local (169.254.x) addresses

**Async event dispatch:** `LogEntryEventListener` (`@Async` + `@EventListener`) — handles the complete pipeline asynchronously so log ingestion is never blocked by trap evaluation or alert firing.

**Kafka consumer:** `KafkaLogConsumer` (`@KafkaListener(topics = "codeops-logs")`) — consumes from `codeops-logs` topic, validates `X-Team-Id` header, deserializes JSON, delegates to `LogIngestionService.ingest()`.

---

## 12. Error Handling

**GlobalExceptionHandler** — `@RestControllerAdvice` with 15 handlers. All return `ErrorResponse(message, timestamp)`.

```
NotFoundException → 404 → Exception message
ValidationException → 400 → Exception message
AuthorizationException → 403 → Exception message
EntityNotFoundException (JPA) → 404 → "Resource not found"
IllegalArgumentException → 400 → "Invalid request"
AccessDeniedException (Spring Security) → 403 → "Access denied"
MethodArgumentNotValidException → 400 → Concatenated field errors
HttpMessageNotReadableException → 400 → "Malformed request body"
NoResourceFoundException → 404 → "Resource not found"
MissingServletRequestParameterException → 400 → "Missing required parameter: {name}"
MethodArgumentTypeMismatchException → 400 → "Invalid value for parameter {name}"
MissingRequestHeaderException → 400 → "Missing required header: {name}"
HttpRequestMethodNotSupportedException → 405 → "HTTP method '{method}' is not supported..."
LoggerException (base) → 500 → "An internal error occurred"
Exception (catch-all) → 500 → "An internal error occurred"
```

Business exception messages (400/403/404) pass through to clients. 500-level errors always masked with generic message. Internal details logged server-side at ERROR level.

---

## 13. Test Coverage

```
Unit Test Files: 51
Integration Test Files: 5
Unit @Test Methods: 559
Integration @Test Methods: 8
Total @Test Methods: 567
```

- **Framework:** JUnit 5, Mockito 5.21.0, Spring Boot Test, Spring Security Test
- **Infrastructure:** Testcontainers 1.19.8 (PostgreSQL + Kafka), H2 for unit tests
- **Test config:** `application-test.yml` in `src/main/resources/` (H2 in-memory), `application-integration.yml` in `src/main/resources/` (PostgreSQL dialect for Testcontainers)
- **Integration tests:** 5 files exist but have limited @Test methods (8 total). Testcontainers configured but underutilized.

---

## 14. Cross-Cutting Patterns & Conventions

- **Naming:** Controllers use `create/get/update/delete` prefix. Services mirror controller method names. DTOs follow `Create*Request`, `Update*Request`, `*Response` convention. Endpoints use RESTful paths.
- **Package structure:** Standard Spring Boot — config, controller, dto (mapper/request/response), entity (enums), event, exception, repository, security, service
- **Base classes:** `BaseEntity` (@MappedSuperclass — id, createdAt, updatedAt), `BaseController` (abstract — provides SecurityUtils helpers for extracting userId/teamId from SecurityContext and X-Team-ID header)
- **Audit logging:** No dedicated audit service. `LoggingInterceptor` logs all HTTP requests on `/api/**` with method, URI, status, duration, correlationId. Services use info-level logging for mutation operations.
- **Error handling:** Services throw `NotFoundException`, `ValidationException`, `AuthorizationException`. GlobalExceptionHandler translates to HTTP status codes. Controllers never catch exceptions.
- **Pagination:** `PageResponse<T>` wrapper (content, totalElements, totalPages, currentPage, pageSize). Services accept `int page, int size` params and construct `PageRequest`.
- **Validation:** Jakarta Bean Validation on request DTOs (`@NotBlank`, `@Size`, `@Min`, `@Max`). Additional business validation in services.
- **Constants:** `AppConstants` — `DEFAULT_PAGE_SIZE`, `MAX_PAGE_SIZE`, `MAX_BATCH_SIZE`, `MIN_RETENTION_DAYS`, `MAX_RETENTION_DAYS`, `MAX_DASHBOARDS_PER_TEAM`, `MAX_WIDGETS_PER_DASHBOARD`
- **Documentation comments:** No class-level Javadoc on services, controllers, config, or security classes. ~51% of public methods have Javadoc. Entities and repositories have partial Javadoc.
- **Mapping:** MapStruct 1.5.5 with `componentModel = "spring"` and `builder = @Builder(disableBuilder = true)`. 13 mapper interfaces. All use `@Mapping(target = "id", ignore = true)` for entity creation.
- **Multi-tenancy:** Every entity has `teamId` (UUID). Nearly every repository provides `findByTeamId` as the primary access pattern. Controllers extract teamId from `X-Team-ID` header via BaseController.

---

## 15. Known Issues, TODOs, and Technical Debt

No TODO, FIXME, HACK, or XXX comments found in source code.

---

## 16. OpenAPI Specification

See `CodeOps-Logger-OpenAPI.yaml` in the project root. Generated from source code analysis of all 12 controllers, 34 request DTOs, and 30 response DTOs. Covers all 105 endpoints.

---

## 17. DATABASE SCHEMA

Database not available for live audit. Schema documented from JPA entities only (Section 6).

**Summary:** 17 tables (16 entities + base entity is @MappedSuperclass). PostgreSQL 16 at `localhost:5437`, database `codeops_logger`, user `codeops`. Hibernate `ddl-auto: update` manages schema. All tables in `public` schema.

---

## 18. Kafka / Message Broker

**Kafka consumer detected.** Topic: `codeops-logs`. Consumer group: `codeops-logger`.

- **Consumer:** `KafkaLogConsumer.java` (`@KafkaListener(topics = "codeops-logs")`)
- **Payload:** JSON-serialized log entry with `X-Team-Id` header
- **Deserialization:** String key + String value (manual JSON deserialization via ObjectMapper)
- **Error handling:** Invalid team ID or deserialization failures logged at WARN, message skipped
- **Idempotency:** No deduplication — each consumed message creates a new LogEntry
- **Configuration:** `KafkaConsumerConfig.java` provides manual `ConsumerFactory` bean. Bootstrap servers from `spring.kafka.bootstrap-servers`.
- **No producers** — this service only consumes. Log producers are other CodeOps services.

---

## 19. Redis / Cache Layer

No Redis or caching layer detected in this project.

---

## 20. ENVIRONMENT VARIABLE INVENTORY

| Variable | Required | Default | Used By | Purpose |
|---|---|---|---|---|
| `DATABASE_URL` | Yes (prod) | None | application-prod.yml | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | Yes (prod) | None | application-prod.yml | Database username |
| `DATABASE_PASSWORD` | Yes (prod) | None | application-prod.yml | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes (prod) | None | application-prod.yml | Kafka broker addresses |
| `JWT_SECRET` | Yes (prod) | Dev default (dev profile) | application-dev/prod.yml | JWT HMAC-SHA256 signing key |
| `CORS_ALLOWED_ORIGINS` | Yes (prod) | None | application-prod.yml | Allowed CORS origins |
| `DB_USERNAME` | No | `codeops` | application-dev.yml | Dev database username |
| `DB_PASSWORD` | No | `codeops` | application-dev.yml | Dev database password |
| `codeops.anomaly.recalculation-cron` | No | `0 0 3 * * *` | AnomalyDetectionService | Anomaly baseline recalculation schedule |

**Warning:** `JWT_SECRET` has a hardcoded dev default (`dev-secret-key-minimum-32-characters-long-for-hs256`). Profile-gated to dev only — prod requires env var.

---

## 21. Inter-Service Communication Map

**Outbound HTTP:**
- `AlertChannelService` → User-configured webhook URLs via `RestTemplate` (SSRF-validated). Used for WEBHOOK, SLACK, and MICROSOFT_TEAMS alert channel types.
- RestTemplate injected via `RestTemplateConfig` bean (no `new RestTemplate()`).

**Inbound dependencies:**
- **CodeOps-Server:** Issues JWT tokens that this service validates (shared secret)
- **Other CodeOps services:** Produce log entries to Kafka topic `codeops-logs`, consumed by `KafkaLogConsumer`
- **CodeOps-Registry:** Cross-referenced via `LogSource.serviceId` (UUID reference, no direct API call)

**No direct service-to-service HTTP calls to other CodeOps services.** Communication is via shared JWT secret (auth) and Kafka (log ingestion).
