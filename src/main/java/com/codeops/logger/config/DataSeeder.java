package com.codeops.logger.config;

import com.codeops.logger.entity.*;
import com.codeops.logger.entity.enums.*;
import com.codeops.logger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Development data seeder that populates the database with sample data on startup.
 * Only runs when the {@code dev} profile is active and the database is empty.
 * Provides realistic test data for all Logger entities to support development and demos.
 */
@Component
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final LogSourceRepository logSourceRepository;
    private final LogEntryRepository logEntryRepository;
    private final LogTrapRepository logTrapRepository;
    private final TrapConditionRepository trapConditionRepository;
    private final AlertChannelRepository alertChannelRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final MetricRepository metricRepository;
    private final MetricSeriesRepository metricSeriesRepository;
    private final DashboardRepository dashboardRepository;
    private final DashboardWidgetRepository dashboardWidgetRepository;
    private final RetentionPolicyRepository retentionPolicyRepository;
    private final AnomalyBaselineRepository anomalyBaselineRepository;
    private final TraceSpanRepository traceSpanRepository;

    private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Random RANDOM = new Random(42);

    @Override
    @Transactional
    public void run(String... args) {
        if (logSourceRepository.count() > 0) {
            log.info("Database already seeded — skipping DataSeeder");
            return;
        }

        log.info("Seeding development data...");
        long start = System.currentTimeMillis();

        List<LogSource> sources = seedLogSources();
        seedLogEntries(sources);
        List<LogTrap> traps = seedLogTraps();
        List<AlertChannel> channels = seedAlertChannels();
        seedAlertRules(traps, channels);
        List<Metric> metrics = seedMetrics();
        seedMetricSeries(metrics);
        seedDashboards();
        seedRetentionPolicies();
        seedAnomalyBaselines();
        seedTraceSpans();

        long duration = System.currentTimeMillis() - start;
        log.info("Development data seeded in {}ms", duration);
    }

    /**
     * Seeds five log sources representing CodeOps microservices.
     *
     * @return the persisted log sources
     */
    List<LogSource> seedLogSources() {
        List<LogSource> sources = new ArrayList<>();
        String[][] data = {
                {"codeops-server", "Main API server"},
                {"codeops-registry", "Service registry"},
                {"codeops-vault", "Secrets management"},
                {"codeops-courier", "Notification service"},
                {"codeops-logger", "Logging service"}
        };
        for (String[] d : data) {
            LogSource s = LogSource.builder()
                    .name(d[0])
                    .description(d[1])
                    .environment("development")
                    .isActive(true)
                    .teamId(TEAM_ID)
                    .build();
            sources.add(logSourceRepository.save(s));
        }
        log.debug("Seeded {} log sources", sources.size());
        return sources;
    }

    /**
     * Seeds fifty log entries spread across sources and log levels with realistic data.
     *
     * @param sources the available log sources
     */
    void seedLogEntries(List<LogSource> sources) {
        Instant now = Instant.now();
        List<LogEntry> entries = new ArrayList<>();

        String[][] infoMessages = {
                {"Application started successfully", "com.codeops.server.Application", "main"},
                {"Request processed: GET /api/v1/projects", "com.codeops.server.controller.ProjectController", "http-nio-8090-exec-1"},
                {"Cache refreshed for team settings", "com.codeops.server.service.CacheService", "scheduler-1"},
                {"User login successful: adam@allard.com", "com.codeops.server.service.AuthService", "http-nio-8090-exec-3"},
                {"Health check passed", "com.codeops.server.config.HealthController", "http-nio-8090-exec-5"},
                {"Kafka consumer started for topic: codeops-logs", "com.codeops.logger.service.KafkaLogConsumer", "kafka-consumer-1"},
                {"Database connection pool initialized: 10 connections", "com.zaxxer.hikari.HikariDataSource", "main"},
                {"Metrics collection completed for codeops-server", "com.codeops.logger.service.MetricsService", "scheduler-2"},
                {"Dashboard widget data refreshed", "com.codeops.logger.service.DashboardService", "scheduler-3"},
                {"Service discovery completed: 5 services registered", "com.codeops.registry.service.DiscoveryService", "scheduler-1"},
                {"Retention policy executed: 0 entries purged", "com.codeops.logger.service.RetentionExecutor", "scheduler-4"},
                {"Anomaly baseline recalculated for codeops-server", "com.codeops.logger.service.AnomalyDetectionService", "scheduler-5"},
                {"WebSocket connection established for team monitoring", "com.codeops.server.config.WebSocketConfig", "http-nio-8090-exec-7"},
                {"JWT token validated for user: adam@allard.com", "com.codeops.logger.security.JwtAuthFilter", "http-nio-8098-exec-2"},
                {"Batch log ingestion completed: 25 entries", "com.codeops.logger.service.LogIngestionService", "http-nio-8098-exec-4"},
                {"Template dashboard created: Operations Overview", "com.codeops.logger.service.DashboardService", "http-nio-8098-exec-6"},
                {"Alert rule evaluated: no matches", "com.codeops.logger.service.AlertService", "scheduler-6"},
                {"Trace span correlated: 5 spans linked", "com.codeops.logger.service.TraceService", "http-nio-8098-exec-8"},
                {"Log query DSL parsed: level = ERROR AND service = codeops-server", "com.codeops.logger.service.LogQueryDslParser", "http-nio-8098-exec-9"},
                {"Metric data pushed: 24 data points for http_requests_total", "com.codeops.logger.service.MetricsService", "http-nio-8098-exec-10"}
        };
        for (int i = 0; i < 20; i++) {
            String[] m = infoMessages[i];
            entries.add(LogEntry.builder()
                    .source(sources.get(i % sources.size()))
                    .level(LogLevel.INFO)
                    .message(m[0])
                    .timestamp(now.minus(24 - i, ChronoUnit.HOURS))
                    .serviceName(sources.get(i % sources.size()).getName())
                    .loggerName(m[1])
                    .threadName(m[2])
                    .teamId(TEAM_ID)
                    .build());
        }

        String[][] debugMessages = {
                {"SQL: SELECT * FROM projects WHERE team_id = ?", "org.hibernate.SQL", "http-nio-8090-exec-1"},
                {"Cache lookup: key=team-settings-001, hit=true", "com.codeops.server.service.CacheService", "http-nio-8090-exec-2"},
                {"Request headers: X-Team-Id=00000001, Authorization=Bearer ***", "com.codeops.logger.config.LoggingInterceptor", "http-nio-8098-exec-1"},
                {"Kafka message received: partition=0, offset=1523", "com.codeops.logger.service.KafkaLogConsumer", "kafka-consumer-1"},
                {"Log parsing result: level=INFO, structured=true", "com.codeops.logger.service.LogParsingService", "http-nio-8098-exec-3"},
                {"Trap evaluation: 3 traps checked, 0 matched", "com.codeops.logger.service.TrapEvaluationEngine", "http-nio-8098-exec-4"},
                {"Metric aggregation: window=60s, points=15", "com.codeops.logger.service.MetricAggregationService", "scheduler-2"},
                {"Trace flow built: correlationId=abc-123, spans=4", "com.codeops.logger.service.TraceAnalysisService", "http-nio-8098-exec-5"},
                {"Dashboard query executed: widgets=4, duration=12ms", "com.codeops.logger.service.DashboardService", "http-nio-8098-exec-6"},
                {"Baseline calculation: samples=168, mean=150.2, stddev=24.8", "com.codeops.logger.service.AnomalyBaselineCalculator", "scheduler-5"}
        };
        for (int i = 0; i < 10; i++) {
            String[] m = debugMessages[i];
            entries.add(LogEntry.builder()
                    .source(sources.get(i % sources.size()))
                    .level(LogLevel.DEBUG)
                    .message(m[0])
                    .timestamp(now.minus(23 - i, ChronoUnit.HOURS).minus(30, ChronoUnit.MINUTES))
                    .serviceName(sources.get(i % sources.size()).getName())
                    .loggerName(m[1])
                    .threadName(m[2])
                    .teamId(TEAM_ID)
                    .build());
        }

        String[][] warnMessages = {
                {"Slow query detected: SELECT * FROM log_entries took 2500ms", "com.codeops.logger.service.LogQueryService"},
                {"Retry attempt 2/3 for Kafka connection", "com.codeops.logger.config.KafkaConsumerConfig"},
                {"Connection pool near limit: 8/10 connections in use", "com.zaxxer.hikari.pool.HikariPool"},
                {"JWT token expires in 5 minutes, consider refresh", "com.codeops.logger.security.JwtTokenProvider"},
                {"Dashboard widget query returned empty result set", "com.codeops.logger.service.DashboardService"},
                {"Metric data gap detected: no data for 2 hours", "com.codeops.logger.service.MetricsService"},
                {"Alert throttled: rule 'High Error Rate' fired within cooldown", "com.codeops.logger.service.AlertService"},
                {"Trace span missing endTime, using current time", "com.codeops.logger.service.TraceService"},
                {"Retention policy archive destination unreachable, will retry", "com.codeops.logger.service.RetentionExecutor"},
                {"Anomaly detection baseline has low sample count: 12", "com.codeops.logger.service.AnomalyDetectionService"}
        };
        for (int i = 0; i < 10; i++) {
            String[] m = warnMessages[i];
            entries.add(LogEntry.builder()
                    .source(sources.get(i % sources.size()))
                    .level(LogLevel.WARN)
                    .message(m[0])
                    .timestamp(now.minus(12 - i, ChronoUnit.HOURS))
                    .serviceName(sources.get(i % sources.size()).getName())
                    .loggerName(m[1])
                    .threadName("http-nio-exec-" + (i + 1))
                    .teamId(TEAM_ID)
                    .build());
        }

        String[][] errorMessages = {
                {"Connection timeout to database after 30000ms", "com.zaxxer.hikari.pool.HikariPool", "java.sql.SQLException", "Connection timeout", "at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:188)"},
                {"Authentication failed for user: unknown@example.com", "com.codeops.server.service.AuthService", "com.codeops.server.exception.AuthenticationException", "Invalid credentials", null},
                {"Resource not found: Project with id 999 does not exist", "com.codeops.server.service.ProjectService", "com.codeops.server.exception.NotFoundException", "Project not found", null},
                {"Failed to send webhook notification to https://hooks.example.com", "com.codeops.logger.service.AlertChannelService", "java.net.ConnectException", "Connection refused", "at java.net.PlainSocketImpl.connect(PlainSocketImpl.java:196)"},
                {"Kafka producer send failed: topic codeops-metrics", "com.codeops.logger.service.MetricsService", "org.apache.kafka.common.errors.TimeoutException", "Expiring 1 record(s)", null},
                {"Invalid DSL query syntax: unexpected token 'BETWEEN'", "com.codeops.logger.service.LogQueryDslParser", "com.codeops.logger.exception.ValidationException", "Parse error at position 15", null},
                {"MapStruct mapping failed: null source for MetricMapper", "com.codeops.logger.dto.mapper.MetricMapper", "java.lang.NullPointerException", "Cannot invoke method on null", "at com.codeops.logger.dto.mapper.MetricMapperImpl.toResponse(MetricMapperImpl.java:42)"}
        };
        for (int i = 0; i < 7; i++) {
            String[] m = errorMessages[i];
            entries.add(LogEntry.builder()
                    .source(sources.get(i % sources.size()))
                    .level(LogLevel.ERROR)
                    .message(m[0])
                    .timestamp(now.minus(6 - i, ChronoUnit.HOURS))
                    .serviceName(sources.get(i % sources.size()).getName())
                    .loggerName(m[1])
                    .threadName("http-nio-exec-" + (i + 10))
                    .exceptionClass(m[2])
                    .exceptionMessage(m[3])
                    .stackTrace(m[4])
                    .correlationId("error-corr-" + (i + 1))
                    .teamId(TEAM_ID)
                    .build());
        }

        String[][] fatalMessages = {
                {"Out of memory: Java heap space", "java.lang.OutOfMemoryError"},
                {"Database unreachable: all connection attempts exhausted", "com.zaxxer.hikari.pool.HikariPool"},
                {"Critical service down: codeops-vault is not responding", "com.codeops.registry.service.HealthMonitor"}
        };
        for (int i = 0; i < 3; i++) {
            String[] m = fatalMessages[i];
            entries.add(LogEntry.builder()
                    .source(sources.get(i % sources.size()))
                    .level(LogLevel.FATAL)
                    .message(m[0])
                    .timestamp(now.minus(2 - i, ChronoUnit.HOURS))
                    .serviceName(sources.get(i % sources.size()).getName())
                    .loggerName(m[1])
                    .threadName("main")
                    .correlationId("fatal-corr-" + (i + 1))
                    .teamId(TEAM_ID)
                    .build());
        }

        logEntryRepository.saveAll(entries);

        for (LogSource source : sources) {
            long count = entries.stream()
                    .filter(e -> e.getSource() != null && e.getSource().getId().equals(source.getId()))
                    .count();
            source.setLogCount(count);
            source.setLastLogReceivedAt(now);
            logSourceRepository.save(source);
        }
        log.debug("Seeded {} log entries", entries.size());
    }

    /**
     * Seeds three log traps with conditions for error detection, fatal alerts, and auth failures.
     *
     * @return the persisted log traps
     */
    List<LogTrap> seedLogTraps() {
        LogTrap trap1 = LogTrap.builder()
                .name("High Error Rate")
                .description("Catches connection errors and timeouts")
                .trapType(TrapType.PATTERN)
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();
        trap1 = logTrapRepository.save(trap1);

        TrapCondition cond1a = TrapCondition.builder()
                .trap(trap1)
                .conditionType(ConditionType.REGEX)
                .field("message")
                .pattern("(?i)(timeout|connection refused|unreachable)")
                .build();
        TrapCondition cond1b = TrapCondition.builder()
                .trap(trap1)
                .conditionType(ConditionType.KEYWORD)
                .field("level")
                .pattern("ERROR")
                .build();
        trapConditionRepository.saveAll(List.of(cond1a, cond1b));

        LogTrap trap2 = LogTrap.builder()
                .name("Fatal Alert")
                .description("Triggers on any FATAL level log entry")
                .trapType(TrapType.PATTERN)
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();
        trap2 = logTrapRepository.save(trap2);

        TrapCondition cond2 = TrapCondition.builder()
                .trap(trap2)
                .conditionType(ConditionType.KEYWORD)
                .field("level")
                .pattern("FATAL")
                .build();
        trapConditionRepository.save(cond2);

        LogTrap trap3 = LogTrap.builder()
                .name("Auth Failures")
                .description("Detects repeated authentication failures")
                .trapType(TrapType.FREQUENCY)
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();
        trap3 = logTrapRepository.save(trap3);

        TrapCondition cond3 = TrapCondition.builder()
                .trap(trap3)
                .conditionType(ConditionType.FREQUENCY_THRESHOLD)
                .field("message")
                .pattern("authentication failed")
                .threshold(5)
                .windowSeconds(300)
                .build();
        trapConditionRepository.save(cond3);

        log.debug("Seeded 3 log traps with conditions");
        return List.of(trap1, trap2, trap3);
    }

    /**
     * Seeds three alert channels for email, webhook, and Slack notifications.
     *
     * @return the persisted alert channels
     */
    List<AlertChannel> seedAlertChannels() {
        AlertChannel email = AlertChannel.builder()
                .name("Dev Email")
                .channelType(AlertChannelType.EMAIL)
                .configuration("{\"recipients\":[\"dev@codeops.io\"]}")
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();

        AlertChannel webhook = AlertChannel.builder()
                .name("Ops Webhook")
                .channelType(AlertChannelType.WEBHOOK)
                .configuration("{\"url\":\"https://hooks.example.com/codeops\"}")
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();

        AlertChannel slack = AlertChannel.builder()
                .name("Slack Alerts")
                .channelType(AlertChannelType.SLACK)
                .configuration("{\"webhook_url\":\"https://hooks.slack.com/services/T00/B00/xxx\"}")
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();

        List<AlertChannel> channels = alertChannelRepository.saveAll(List.of(email, webhook, slack));
        log.debug("Seeded {} alert channels", channels.size());
        return channels;
    }

    /**
     * Seeds two alert rules connecting traps to notification channels.
     *
     * @param traps    the available log traps
     * @param channels the available alert channels
     */
    void seedAlertRules(List<LogTrap> traps, List<AlertChannel> channels) {
        AlertRule rule1 = AlertRule.builder()
                .name("High Error Rate → Slack")
                .trap(traps.get(0))
                .channel(channels.get(2))
                .severity(AlertSeverity.WARNING)
                .isActive(true)
                .throttleMinutes(15)
                .teamId(TEAM_ID)
                .build();

        AlertRule rule2 = AlertRule.builder()
                .name("Fatal Alert → Email")
                .trap(traps.get(1))
                .channel(channels.get(0))
                .severity(AlertSeverity.CRITICAL)
                .isActive(true)
                .throttleMinutes(5)
                .teamId(TEAM_ID)
                .build();

        alertRuleRepository.saveAll(List.of(rule1, rule2));
        log.debug("Seeded 2 alert rules");
    }

    /**
     * Seeds six metrics covering counters, gauges, timers, and histograms.
     *
     * @return the persisted metrics
     */
    List<Metric> seedMetrics() {
        Metric m1 = Metric.builder().name("http_requests_total").metricType(MetricType.COUNTER)
                .description("Total HTTP requests").unit("requests").serviceName("codeops-server").teamId(TEAM_ID).build();
        Metric m2 = Metric.builder().name("active_connections").metricType(MetricType.GAUGE)
                .description("Active database connections").unit("connections").serviceName("codeops-server").teamId(TEAM_ID).build();
        Metric m3 = Metric.builder().name("response_time_ms").metricType(MetricType.TIMER)
                .description("HTTP response time").unit("ms").serviceName("codeops-server").teamId(TEAM_ID).build();
        Metric m4 = Metric.builder().name("request_size_bytes").metricType(MetricType.HISTOGRAM)
                .description("HTTP request body size").unit("bytes").serviceName("codeops-server").teamId(TEAM_ID).build();
        Metric m5 = Metric.builder().name("cache_hit_ratio").metricType(MetricType.GAUGE)
                .description("Cache hit percentage").unit("percent").serviceName("codeops-registry").teamId(TEAM_ID).build();
        Metric m6 = Metric.builder().name("queue_depth").metricType(MetricType.GAUGE)
                .description("Message queue depth").unit("messages").serviceName("codeops-courier").teamId(TEAM_ID).build();

        List<Metric> metrics = metricRepository.saveAll(List.of(m1, m2, m3, m4, m5, m6));
        log.debug("Seeded {} metrics", metrics.size());
        return metrics;
    }

    /**
     * Seeds 24 hourly data points for each metric with realistic values.
     *
     * @param metrics the available metrics
     */
    void seedMetricSeries(List<Metric> metrics) {
        Instant now = Instant.now();
        List<MetricSeries> allPoints = new ArrayList<>();

        for (int hour = 23; hour >= 0; hour--) {
            Instant ts = now.minus(hour, ChronoUnit.HOURS);
            for (Metric m : metrics) {
                double value = generateMetricValue(m.getName(), hour);
                allPoints.add(MetricSeries.builder()
                        .metric(m)
                        .timestamp(ts)
                        .value(value)
                        .resolution(60)
                        .build());
            }
        }
        metricSeriesRepository.saveAll(allPoints);
        log.debug("Seeded {} metric data points", allPoints.size());
    }

    /**
     * Seeds two dashboards with widgets: an operations overview and a service health template.
     */
    void seedDashboards() {
        Dashboard ops = Dashboard.builder()
                .name("Operations Overview")
                .description("Real-time overview of system health and performance")
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .isShared(true)
                .isTemplate(false)
                .refreshIntervalSeconds(30)
                .build();
        ops = dashboardRepository.save(ops);

        dashboardWidgetRepository.saveAll(List.of(
                DashboardWidget.builder().dashboard(ops).title("Error Rate")
                        .widgetType(WidgetType.TIME_SERIES_CHART).gridX(0).gridY(0).gridWidth(6).gridHeight(3).sortOrder(0).build(),
                DashboardWidget.builder().dashboard(ops).title("Active Connections")
                        .widgetType(WidgetType.GAUGE).gridX(6).gridY(0).gridWidth(3).gridHeight(3).sortOrder(1).build(),
                DashboardWidget.builder().dashboard(ops).title("Request Volume")
                        .widgetType(WidgetType.COUNTER).gridX(9).gridY(0).gridWidth(3).gridHeight(3).sortOrder(2).build(),
                DashboardWidget.builder().dashboard(ops).title("Recent Errors")
                        .widgetType(WidgetType.LOG_STREAM).gridX(0).gridY(3).gridWidth(12).gridHeight(4).sortOrder(3).build()
        ));

        Dashboard template = Dashboard.builder()
                .name("Service Health Template")
                .description("Template for per-service health dashboards")
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .isShared(true)
                .isTemplate(true)
                .refreshIntervalSeconds(60)
                .build();
        template = dashboardRepository.save(template);

        dashboardWidgetRepository.saveAll(List.of(
                DashboardWidget.builder().dashboard(template).title("Response Time P95")
                        .widgetType(WidgetType.TIME_SERIES_CHART).gridX(0).gridY(0).gridWidth(6).gridHeight(3).sortOrder(0).build(),
                DashboardWidget.builder().dashboard(template).title("Error Count")
                        .widgetType(WidgetType.COUNTER).gridX(6).gridY(0).gridWidth(6).gridHeight(3).sortOrder(1).build(),
                DashboardWidget.builder().dashboard(template).title("Recent Logs")
                        .widgetType(WidgetType.TABLE).gridX(0).gridY(3).gridWidth(12).gridHeight(4).sortOrder(2).build()
        ));

        log.debug("Seeded 2 dashboards with 7 widgets");
    }

    /**
     * Seeds three retention policies for debug cleanup, error archiving, and default retention.
     */
    void seedRetentionPolicies() {
        RetentionPolicy p1 = RetentionPolicy.builder()
                .name("Debug Log Cleanup")
                .logLevel(LogLevel.DEBUG)
                .retentionDays(7)
                .action(RetentionAction.PURGE)
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();

        RetentionPolicy p2 = RetentionPolicy.builder()
                .name("Error Log Archive")
                .logLevel(LogLevel.ERROR)
                .retentionDays(90)
                .action(RetentionAction.ARCHIVE)
                .archiveDestination("s3://codeops-logs/error-archive/")
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();

        RetentionPolicy p3 = RetentionPolicy.builder()
                .name("Default Retention")
                .retentionDays(30)
                .action(RetentionAction.PURGE)
                .isActive(true)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();

        retentionPolicyRepository.saveAll(List.of(p1, p2, p3));
        log.debug("Seeded 3 retention policies");
    }

    /**
     * Seeds two anomaly baselines for log volume and error rate monitoring.
     */
    void seedAnomalyBaselines() {
        Instant now = Instant.now();

        AnomalyBaseline b1 = AnomalyBaseline.builder()
                .serviceName("codeops-server")
                .metricName("log_volume")
                .baselineValue(150.0)
                .standardDeviation(25.0)
                .sampleCount(168L)
                .windowStartTime(now.minus(7, ChronoUnit.DAYS))
                .windowEndTime(now)
                .deviationThreshold(2.0)
                .isActive(true)
                .teamId(TEAM_ID)
                .lastComputedAt(now)
                .build();

        AnomalyBaseline b2 = AnomalyBaseline.builder()
                .serviceName("codeops-server")
                .metricName("error_rate")
                .baselineValue(2.5)
                .standardDeviation(1.0)
                .sampleCount(168L)
                .windowStartTime(now.minus(7, ChronoUnit.DAYS))
                .windowEndTime(now)
                .deviationThreshold(2.5)
                .isActive(true)
                .teamId(TEAM_ID)
                .lastComputedAt(now)
                .build();

        anomalyBaselineRepository.saveAll(List.of(b1, b2));
        log.debug("Seeded 2 anomaly baselines");
    }

    /**
     * Seeds two realistic distributed traces: one successful and one with an error.
     */
    void seedTraceSpans() {
        Instant now = Instant.now();
        Instant t1Start = now.minus(5, ChronoUnit.MINUTES);

        List<TraceSpan> spans = new ArrayList<>();

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-001").traceId("trace-001").spanId("span-001").parentSpanId(null)
                .serviceName("codeops-server").operationName("POST /api/v1/jobs")
                .startTime(t1Start).endTime(t1Start.plusMillis(200)).durationMs(200L)
                .status(SpanStatus.OK).teamId(TEAM_ID).build());

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-001").traceId("trace-001").spanId("span-002").parentSpanId("span-001")
                .serviceName("codeops-server").operationName("validateProject")
                .startTime(t1Start.plusMillis(10)).endTime(t1Start.plusMillis(50)).durationMs(40L)
                .status(SpanStatus.OK).teamId(TEAM_ID).build());

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-001").traceId("trace-001").spanId("span-003").parentSpanId("span-001")
                .serviceName("codeops-server").operationName("createJobRecord")
                .startTime(t1Start.plusMillis(50)).endTime(t1Start.plusMillis(100)).durationMs(50L)
                .status(SpanStatus.OK).teamId(TEAM_ID).build());

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-001").traceId("trace-001").spanId("span-004").parentSpanId("span-001")
                .serviceName("codeops-server").operationName("notifyTeam")
                .startTime(t1Start.plusMillis(100)).endTime(t1Start.plusMillis(180)).durationMs(80L)
                .status(SpanStatus.OK).teamId(TEAM_ID).build());

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-001").traceId("trace-001").spanId("span-005").parentSpanId("span-004")
                .serviceName("codeops-courier").operationName("sendTeamsWebhook")
                .startTime(t1Start.plusMillis(120)).endTime(t1Start.plusMillis(170)).durationMs(50L)
                .status(SpanStatus.OK).teamId(TEAM_ID).build());

        Instant t2Start = now.minus(3, ChronoUnit.MINUTES);

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-002").traceId("trace-002").spanId("span-006").parentSpanId(null)
                .serviceName("codeops-server").operationName("GET /api/v1/projects/999")
                .startTime(t2Start).endTime(t2Start.plusMillis(150)).durationMs(150L)
                .status(SpanStatus.ERROR).statusMessage("Project not found: 999").teamId(TEAM_ID).build());

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-002").traceId("trace-002").spanId("span-007").parentSpanId("span-006")
                .serviceName("codeops-server").operationName("findProjectById")
                .startTime(t2Start.plusMillis(10)).endTime(t2Start.plusMillis(60)).durationMs(50L)
                .status(SpanStatus.OK).teamId(TEAM_ID).build());

        spans.add(TraceSpan.builder()
                .correlationId("seed-trace-002").traceId("trace-002").spanId("span-008").parentSpanId("span-006")
                .serviceName("codeops-server").operationName("handleNotFound")
                .startTime(t2Start.plusMillis(60)).endTime(t2Start.plusMillis(140)).durationMs(80L)
                .status(SpanStatus.ERROR).statusMessage("NotFoundException: Project with id 999 does not exist")
                .teamId(TEAM_ID).build());

        traceSpanRepository.saveAll(spans);
        log.debug("Seeded {} trace spans across 2 traces", spans.size());
    }

    /**
     * Generates a realistic metric value based on metric name and hour offset.
     *
     * @param metricName the metric name
     * @param hourOffset hours from now (0 = now, 23 = 23 hours ago)
     * @return the generated value
     */
    private double generateMetricValue(String metricName, int hourOffset) {
        double noise = (RANDOM.nextDouble() - 0.5) * 10;
        return switch (metricName) {
            case "http_requests_total" -> 1000 + (23 - hourOffset) * 60 + noise;
            case "active_connections" -> 25 + Math.sin(hourOffset * 0.5) * 15 + noise;
            case "response_time_ms" -> 150 + Math.sin(hourOffset * 0.3) * 100 + Math.abs(noise) * 5;
            case "request_size_bytes" -> 1024 + Math.sin(hourOffset * 0.4) * 512 + noise * 50;
            case "cache_hit_ratio" -> 92 + Math.sin(hourOffset * 0.2) * 5 + noise * 0.3;
            case "queue_depth" -> Math.max(0, 10 + Math.sin(hourOffset * 0.6) * 8 + noise * 0.5);
            default -> 100 + noise;
        };
    }
}
