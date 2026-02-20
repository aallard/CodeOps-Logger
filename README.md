# CodeOps-Logger

Centralized logging, metrics, tracing, and alerting service for the CodeOps platform.

## Overview

CodeOps-Logger provides:
- **Log Ingestion** -- HTTP push and Kafka consumer for structured and raw log ingestion
- **Log Query** -- Structured filters, full-text search, and SQL-like DSL query language
- **Log Traps** -- Pattern-based alerting with regex, keyword, frequency, and absence detection
- **Alert System** -- Multi-channel notifications (Email, Webhook, Teams, Slack) with throttling
- **Metrics** -- Counter, gauge, histogram, and timer metrics with time-series storage
- **Distributed Tracing** -- Cross-service request tracing with waterfall visualization and root cause analysis
- **Dashboards** -- Configurable widget-based dashboards with template system
- **Retention** -- Configurable data lifecycle with scheduled purge/archive
- **Anomaly Detection** -- Statistical baseline learning with z-score deviation detection

## Architecture

| Component | Details |
|-----------|---------|
| Port | 8098 |
| Database | PostgreSQL (port 5437, container `codeops-logger-db`) |
| Message Queue | Kafka consumer (port 9094) |
| Auth | JWT validation only (tokens issued by CodeOps-Server) |
| Framework | Spring Boot 3.3.0 + Java 21+ |
| Schema Management | Hibernate `ddl-auto: update` |

## Quick Start

```bash
# Start infrastructure (PostgreSQL)
docker compose up -d

# Run application with dev profile (includes DataSeeder)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Verify health
curl http://localhost:8098/api/v1/logger/health

# Run tests
mvn test
```

## API Documentation

- **Swagger UI:** http://localhost:8098/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8098/v3/api-docs
- **OpenAPI YAML:** http://localhost:8098/v3/api-docs.yaml
- **Static spec:** `./openapi.yaml`

## API Endpoints

### Log Ingestion (`/api/v1/logger/logs`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/logs` | Ingest a single log entry |
| POST | `/logs/batch` | Batch ingest log entries |

### Log Query (`/api/v1/logger/logs`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/logs/query` | Structured query with filters |
| GET | `/logs/search?q=` | Full-text search |
| POST | `/logs/dsl` | DSL query language |
| GET | `/logs/{id}` | Get single log entry |
| POST | `/logs/queries/saved` | Save a query |
| GET | `/logs/queries/saved` | List saved queries |
| GET | `/logs/queries/saved/{id}` | Get saved query |
| PUT | `/logs/queries/saved/{id}` | Update saved query |
| DELETE | `/logs/queries/saved/{id}` | Delete saved query |
| POST | `/logs/queries/saved/{id}/execute` | Execute saved query |
| GET | `/logs/queries/history` | Query history |

### Log Sources (`/api/v1/logger/sources`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/sources` | Register a log source |
| GET | `/sources` | List sources for team |
| GET | `/sources/paged` | List sources (paginated) |
| GET | `/sources/{id}` | Get source |
| PUT | `/sources/{id}` | Update source |
| DELETE | `/sources/{id}` | Delete source |

### Log Traps (`/api/v1/logger/traps`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/traps` | Create a trap |
| GET | `/traps` | List traps for team |
| GET | `/traps/paged` | List traps (paginated) |
| GET | `/traps/{id}` | Get trap |
| PUT | `/traps/{id}` | Update trap |
| DELETE | `/traps/{id}` | Delete trap |
| POST | `/traps/{id}/toggle` | Toggle active status |
| POST | `/traps/{id}/test` | Test against historical logs |
| POST | `/traps/test` | Test trap definition |

### Alerts (`/api/v1/logger/alerts`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/alerts/channels` | Create channel |
| GET | `/alerts/channels` | List channels |
| GET | `/alerts/channels/paged` | List channels (paginated) |
| GET | `/alerts/channels/{id}` | Get channel |
| PUT | `/alerts/channels/{id}` | Update channel |
| DELETE | `/alerts/channels/{id}` | Delete channel |
| POST | `/alerts/rules` | Create rule |
| GET | `/alerts/rules` | List rules |
| GET | `/alerts/rules/paged` | List rules (paginated) |
| GET | `/alerts/rules/{id}` | Get rule |
| PUT | `/alerts/rules/{id}` | Update rule |
| DELETE | `/alerts/rules/{id}` | Delete rule |
| GET | `/alerts/history` | Alert history |
| GET | `/alerts/history/status/{status}` | History by status |
| GET | `/alerts/history/severity/{severity}` | History by severity |
| PUT | `/alerts/history/{id}/status` | Update alert status |
| GET | `/alerts/active-counts` | Active alert counts |

### Metrics (`/api/v1/logger/metrics`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/metrics` | Register a metric |
| GET | `/metrics` | List metrics for team |
| GET | `/metrics/paged` | List metrics (paginated) |
| GET | `/metrics/service/{name}` | Metrics by service |
| GET | `/metrics/service/{name}/summary` | Service metrics summary |
| GET | `/metrics/service/{name}/latest` | Latest values by service |
| GET | `/metrics/{id}` | Get metric |
| PUT | `/metrics/{id}` | Update metric |
| DELETE | `/metrics/{id}` | Delete metric |
| POST | `/metrics/data` | Push data points |
| GET | `/metrics/{id}/timeseries` | Raw time-series data |
| GET | `/metrics/{id}/timeseries/aggregated` | Aggregated time-series |
| GET | `/metrics/{id}/aggregation` | Statistical aggregation |
| GET | `/metrics/{id}/latest` | Latest value |

### Traces (`/api/v1/logger/traces`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/traces/spans` | Create a span |
| POST | `/traces/spans/batch` | Batch create spans |
| GET | `/traces/spans/{id}` | Get span |
| GET | `/traces/flow/{correlationId}` | Trace flow |
| GET | `/traces/flow/by-trace-id/{traceId}` | Trace flow by trace ID |
| GET | `/traces/waterfall/{correlationId}` | Waterfall visualization |
| GET | `/traces/rca/{correlationId}` | Root cause analysis |
| GET | `/traces` | List recent traces |
| GET | `/traces/service/{name}` | Traces by service |
| GET | `/traces/errors` | Error traces |
| GET | `/traces/{correlationId}/logs` | Related log entries |

### Dashboards (`/api/v1/logger/dashboards`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/dashboards` | Create dashboard |
| GET | `/dashboards` | List dashboards |
| GET | `/dashboards/paged` | List dashboards (paginated) |
| GET | `/dashboards/shared` | Shared dashboards |
| GET | `/dashboards/mine` | My dashboards |
| GET | `/dashboards/{id}` | Get dashboard |
| PUT | `/dashboards/{id}` | Update dashboard |
| DELETE | `/dashboards/{id}` | Delete dashboard |
| POST | `/dashboards/{id}/widgets` | Add widget |
| PUT | `/dashboards/{id}/widgets/{wId}` | Update widget |
| DELETE | `/dashboards/{id}/widgets/{wId}` | Remove widget |
| PUT | `/dashboards/{id}/widgets/reorder` | Reorder widgets |
| PUT | `/dashboards/{id}/layout` | Update layout |
| POST | `/dashboards/{id}/template` | Mark as template |
| DELETE | `/dashboards/{id}/template` | Unmark as template |
| GET | `/dashboards/templates` | List templates |
| POST | `/dashboards/from-template` | Create from template |
| POST | `/dashboards/{id}/duplicate` | Duplicate dashboard |

### Retention (`/api/v1/logger/retention`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/retention/policies` | Create policy |
| GET | `/retention/policies` | List policies |
| GET | `/retention/policies/{id}` | Get policy |
| PUT | `/retention/policies/{id}` | Update policy |
| DELETE | `/retention/policies/{id}` | Delete policy |
| PUT | `/retention/policies/{id}/toggle` | Toggle active |
| POST | `/retention/policies/{id}/execute` | Manual execution |
| GET | `/retention/storage` | Storage usage |

### Anomaly Detection (`/api/v1/logger/anomalies`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/anomalies/baselines` | Create/update baseline |
| GET | `/anomalies/baselines` | List baselines |
| GET | `/anomalies/baselines/service/{name}` | Baselines by service |
| GET | `/anomalies/baselines/{id}` | Get baseline |
| PUT | `/anomalies/baselines/{id}` | Update baseline |
| DELETE | `/anomalies/baselines/{id}` | Delete baseline |
| GET | `/anomalies/check` | Check single metric |
| GET | `/anomalies/report` | Full team report |

## Development

```bash
# Run tests
mvn test

# Run with dev profile (enables DataSeeder with sample data)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with custom database
DB_USERNAME=myuser DB_PASSWORD=mypass mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Dev Profile Features
- **DataSeeder** populates the database with realistic sample data on first startup
- Sample data includes: 5 log sources, 50 log entries, 3 traps, 3 alert channels, 2 alert rules, 6 metrics with time-series data, 2 dashboards with widgets, 3 retention policies, 2 anomaly baselines, 8 trace spans across 2 traces

## Project Structure

```
src/main/java/com/codeops/logger/
├── config/          Configuration (Security, Kafka, Scheduling, DataSeeder)
├── controller/      REST controllers (11 files including BaseController)
├── dto/
│   ├── mapper/      MapStruct mappers (13 mappers)
│   ├── request/     Request DTOs with validation (33 records)
│   └── response/    Response DTOs (30 records)
├── entity/
│   └── enums/       JPA entities (16 + BaseEntity) and enums (10)
├── exception/       Exception hierarchy (4 classes)
├── repository/      Spring Data JPA repositories (16 interfaces)
├── security/        JWT filter, token provider, security utils (4 classes)
└── service/         Business logic services (19 classes)
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8098 | HTTP server port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5437/codeops_logger` | Database URL |
| `spring.datasource.username` | `codeops` | Database username |
| `spring.datasource.password` | `codeops` | Database password |
| `spring.kafka.bootstrap-servers` | `localhost:9094` | Kafka broker |
| `codeops.jwt.secret` | (env var) | HMAC-SHA256 signing key |
| `codeops.jwt.expiration-hours` | 24 | Token lifetime |
