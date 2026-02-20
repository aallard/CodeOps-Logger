package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.LogEntryMapper;
import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.LogSource;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.LogSourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Core log ingestion service handling both HTTP push and Kafka-consumed log entries.
 * Resolves or auto-creates log sources, validates entries, and persists to the database.
 */
@Service
@Slf4j
public class LogIngestionService {

    private final LogEntryRepository logEntryRepository;
    private final LogSourceRepository logSourceRepository;
    private final LogEntryMapper logEntryMapper;
    private final LogParsingService logParsingService;

    /**
     * Creates a new LogIngestionService.
     *
     * @param logEntryRepository  repository for log entry persistence
     * @param logSourceRepository repository for log source lookup and creation
     * @param logEntryMapper      mapper for entity/DTO conversion
     * @param logParsingService   parser for raw log strings
     */
    public LogIngestionService(LogEntryRepository logEntryRepository,
                               LogSourceRepository logSourceRepository,
                               LogEntryMapper logEntryMapper,
                               LogParsingService logParsingService) {
        this.logEntryRepository = logEntryRepository;
        this.logSourceRepository = logSourceRepository;
        this.logEntryMapper = logEntryMapper;
        this.logParsingService = logParsingService;
    }

    /**
     * Ingests a single log entry from HTTP push.
     * Resolves the log source (auto-creates if not found), sets teamId, persists.
     *
     * @param request the structured log entry request
     * @param teamId  the authenticated user's team ID
     * @return the persisted log entry response
     */
    @Transactional
    public LogEntryResponse ingest(IngestLogEntryRequest request, UUID teamId) {
        LogLevel level = validateLevel(request.level());
        LogSource source = resolveOrCreateSource(request.serviceName(), teamId);

        LogEntry entity = logEntryMapper.toEntity(request);
        entity.setLevel(level);
        entity.setSource(source);
        entity.setTeamId(teamId);
        entity.setTimestamp(request.timestamp() != null ? request.timestamp() : Instant.now());
        entity.setMessage(truncateMessage(request.message()));

        LogEntry saved = logEntryRepository.save(entity);

        source.setLastLogReceivedAt(Instant.now());
        source.setLogCount(source.getLogCount() + 1);
        logSourceRepository.save(source);

        return logEntryMapper.toResponse(saved);
    }

    /**
     * Ingests a batch of log entries from HTTP push.
     * All entries share the same teamId. Each entry independently resolves its source.
     * Individual entry failures are logged but do not fail the batch.
     *
     * @param requests the list of log entry requests
     * @param teamId   the authenticated user's team ID
     * @return count of successfully ingested entries
     */
    @Transactional
    public int ingestBatch(List<IngestLogEntryRequest> requests, UUID teamId) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        if (requests.size() > AppConstants.MAX_BATCH_SIZE) {
            throw new ValidationException(
                    "Batch size " + requests.size() + " exceeds maximum of " + AppConstants.MAX_BATCH_SIZE);
        }

        Map<String, LogSource> sourceCache = new HashMap<>();
        List<LogEntry> entriesToSave = new ArrayList<>();
        int successCount = 0;

        for (IngestLogEntryRequest request : requests) {
            try {
                LogLevel level = validateLevel(request.level());
                LogSource source = sourceCache.computeIfAbsent(
                        request.serviceName(),
                        sn -> resolveOrCreateSource(sn, teamId)
                );

                LogEntry entity = logEntryMapper.toEntity(request);
                entity.setLevel(level);
                entity.setSource(source);
                entity.setTeamId(teamId);
                entity.setTimestamp(request.timestamp() != null ? request.timestamp() : Instant.now());
                entity.setMessage(truncateMessage(request.message()));

                entriesToSave.add(entity);
                source.setLastLogReceivedAt(Instant.now());
                source.setLogCount(source.getLogCount() + 1);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to process batch entry: {}", e.getMessage());
            }
        }

        if (!entriesToSave.isEmpty()) {
            logEntryRepository.saveAll(entriesToSave);
        }
        sourceCache.values().forEach(logSourceRepository::save);

        return successCount;
    }

    /**
     * Ingests a raw log string (from Kafka or bulk import).
     * Parses the raw string into structured format, then delegates to ingest().
     *
     * @param rawLog             the raw log string
     * @param defaultServiceName the default service name if not parseable from the log
     * @param teamId             the team ID for scoping
     */
    public void ingestRaw(String rawLog, String defaultServiceName, UUID teamId) {
        try {
            IngestLogEntryRequest request = logParsingService.parse(rawLog, defaultServiceName);
            ingest(request, teamId);
        } catch (Exception e) {
            log.warn("Failed to ingest raw log: {}", e.getMessage());
        }
    }

    /**
     * Resolves or auto-creates a LogSource for the given service name and team.
     *
     * @param serviceName the service name to resolve
     * @param teamId      the team ID for scoping
     * @return the resolved or created LogSource
     */
    LogSource resolveOrCreateSource(String serviceName, UUID teamId) {
        return logSourceRepository.findByTeamIdAndName(teamId, serviceName)
                .orElseGet(() -> {
                    LogSource newSource = new LogSource();
                    newSource.setName(serviceName);
                    newSource.setTeamId(teamId);
                    newSource.setIsActive(true);
                    newSource.setLogCount(0L);
                    return logSourceRepository.save(newSource);
                });
    }

    /**
     * Validates the log level string and converts to LogLevel enum.
     *
     * @param level the level string
     * @return the LogLevel enum value
     * @throws ValidationException if the level is not valid
     */
    LogLevel validateLevel(String level) {
        if (level == null || level.isBlank()) {
            throw new ValidationException("Log level is required");
        }
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid log level: " + level);
        }
    }

    private String truncateMessage(String message) {
        if (message == null) return null;
        if (message.length() > AppConstants.MAX_LOG_MESSAGE_LENGTH) {
            return message.substring(0, AppConstants.MAX_LOG_MESSAGE_LENGTH);
        }
        return message;
    }
}
