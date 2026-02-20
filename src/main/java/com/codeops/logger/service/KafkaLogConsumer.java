package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer for high-throughput log ingestion.
 * Subscribes to the codeops-logs topic and delegates to {@link LogIngestionService}.
 * Messages can be structured JSON or raw text — the parsing service handles both.
 */
@Component
@Slf4j
public class KafkaLogConsumer {

    private final LogIngestionService logIngestionService;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new KafkaLogConsumer.
     *
     * @param logIngestionService the ingestion service for processing log entries
     * @param objectMapper        the Jackson ObjectMapper for JSON parsing
     */
    public KafkaLogConsumer(LogIngestionService logIngestionService,
                            ObjectMapper objectMapper) {
        this.logIngestionService = logIngestionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes log messages from the codeops-logs Kafka topic.
     * Each message is expected to be either structured JSON or raw text.
     * The Kafka message key is used as the service name if the message doesn't contain one.
     *
     * @param message      the Kafka message payload
     * @param key          the Kafka message key (typically the service name)
     * @param teamIdHeader the team ID from message headers
     */
    @KafkaListener(
            topics = AppConstants.KAFKA_LOG_TOPIC,
            groupId = AppConstants.KAFKA_CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload String message,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(value = "X-Team-Id", required = false) String teamIdHeader
    ) {
        try {
            if (message == null || message.isBlank()) {
                log.warn("Received null or empty Kafka message, skipping");
                return;
            }

            if (teamIdHeader == null || teamIdHeader.isBlank()) {
                log.warn("Kafka message missing X-Team-Id header, skipping");
                return;
            }

            UUID teamId;
            try {
                teamId = UUID.fromString(teamIdHeader);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid X-Team-Id header value: {}", teamIdHeader);
                return;
            }

            String defaultServiceName = (key != null && !key.isBlank()) ? key : "unknown";

            if (message.trim().startsWith("{")) {
                try {
                    IngestLogEntryRequest request = objectMapper.readValue(message, IngestLogEntryRequest.class);
                    if (request.level() != null && request.message() != null) {
                        String serviceName = request.serviceName() != null ? request.serviceName() : defaultServiceName;
                        IngestLogEntryRequest withService = new IngestLogEntryRequest(
                                request.level(), request.message(), request.timestamp(),
                                serviceName, request.correlationId(), request.traceId(),
                                request.spanId(), request.loggerName(), request.threadName(),
                                request.exceptionClass(), request.exceptionMessage(),
                                request.stackTrace(), request.customFields(),
                                request.hostName(), request.ipAddress()
                        );
                        logIngestionService.ingest(withService, teamId);
                        return;
                    }
                } catch (JsonProcessingException e) {
                    log.debug("Kafka message not structured JSON, falling back to raw parsing");
                }
            }

            logIngestionService.ingestRaw(message, defaultServiceName, teamId);

        } catch (Exception e) {
            log.error("Error processing Kafka log message: {}", e.getMessage(), e);
        }
    }
}
