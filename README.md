# CodeOps-Logger

Centralized logging, metrics, alerting, trace correlation, dashboards, and anomaly detection service for the CodeOps platform.

## Quick Start

```bash
# Start database
docker compose up -d

# Run application
./mvnw spring-boot:run

# Run tests
./mvnw test
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| Server port | 8098 | HTTP server port |
| DB port | 5437 | PostgreSQL port |
| DB name | codeops_logger | Database name |
| Kafka | localhost:9094 | Kafka bootstrap servers |

## API Documentation

Swagger UI: http://localhost:8098/swagger-ui.html
