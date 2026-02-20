package com.codeops.logger.service;

import com.codeops.logger.config.AppConstants;
import com.codeops.logger.dto.mapper.LogEntryMapper;
import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.codeops.logger.dto.response.LogEntryResponse;
import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.LogSource;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.event.LogEntryIngestedEvent;
import com.codeops.logger.exception.ValidationException;
import com.codeops.logger.repository.LogEntryRepository;
import com.codeops.logger.repository.LogSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LogIngestionService}.
 */
@ExtendWith(MockitoExtension.class)
class LogIngestionServiceTest {

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private LogSourceRepository logSourceRepository;

    @Mock
    private LogEntryMapper logEntryMapper;

    @Mock
    private LogParsingService logParsingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LogIngestionService logIngestionService;

    private static final UUID TEAM_ID = UUID.randomUUID();

    private IngestLogEntryRequest createRequest(String level, String message, String serviceName) {
        return new IngestLogEntryRequest(
                level, message, Instant.now(), serviceName,
                null, null, null, null, null,
                null, null, null, null, null, null
        );
    }

    private LogSource createSource(String name) {
        LogSource source = new LogSource();
        source.setId(UUID.randomUUID());
        source.setName(name);
        source.setTeamId(TEAM_ID);
        source.setIsActive(true);
        source.setLogCount(0L);
        return source;
    }

    @Test
    void testIngest_validEntry_persists() {
        IngestLogEntryRequest request = createRequest("INFO", "Test message", "test-service");
        LogSource source = createSource("test-service");
        LogEntry entity = new LogEntry();
        LogEntryResponse expectedResponse = new LogEntryResponse(
                UUID.randomUUID(), source.getId(), "test-service", "INFO", "Test message",
                Instant.now(), "test-service", null, null, null, null, null,
                null, null, null, null, null, null, TEAM_ID, Instant.now()
        );

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "test-service"))
                .thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(request)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(expectedResponse);

        LogEntryResponse result = logIngestionService.ingest(request, TEAM_ID);

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("INFO");
        verify(logEntryRepository).save(any(LogEntry.class));
    }

    @Test
    void testIngest_autoCreatesSource_whenNotFound() {
        IngestLogEntryRequest request = createRequest("INFO", "Test", "new-service");
        LogSource newSource = createSource("new-service");
        LogEntry entity = new LogEntry();

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "new-service"))
                .thenReturn(Optional.empty());
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(newSource);
        when(logEntryMapper.toEntity(request)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingest(request, TEAM_ID);

        ArgumentCaptor<LogSource> sourceCaptor = ArgumentCaptor.forClass(LogSource.class);
        verify(logSourceRepository, atLeastOnce()).save(sourceCaptor.capture());
        List<LogSource> savedSources = sourceCaptor.getAllValues();
        assertThat(savedSources).anyMatch(s -> "new-service".equals(s.getName()));
    }

    @Test
    void testIngest_existingSource_reusesIt() {
        IngestLogEntryRequest request = createRequest("DEBUG", "msg", "existing-svc");
        LogSource existing = createSource("existing-svc");
        existing.setLogCount(10L);
        LogEntry entity = new LogEntry();

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "existing-svc"))
                .thenReturn(Optional.of(existing));
        when(logEntryMapper.toEntity(request)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(existing);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingest(request, TEAM_ID);

        assertThat(existing.getLogCount()).isEqualTo(11L);
    }

    @Test
    void testIngest_nullTimestamp_defaultsToNow() {
        IngestLogEntryRequest request = new IngestLogEntryRequest(
                "INFO", "msg", null, "svc",
                null, null, null, null, null,
                null, null, null, null, null, null
        );
        LogSource source = createSource("svc");
        LogEntry entity = new LogEntry();

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(request)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingest(request, TEAM_ID);

        assertThat(entity.getTimestamp()).isNotNull();
        assertThat(entity.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void testIngest_longMessage_truncated() {
        String longMsg = "X".repeat(AppConstants.MAX_LOG_MESSAGE_LENGTH + 500);
        IngestLogEntryRequest request = createRequest("INFO", longMsg, "svc");
        LogSource source = createSource("svc");
        LogEntry entity = new LogEntry();

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(request)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingest(request, TEAM_ID);

        assertThat(entity.getMessage()).hasSize(AppConstants.MAX_LOG_MESSAGE_LENGTH);
    }

    @Test
    void testIngest_invalidLevel_throwsValidation() {
        IngestLogEntryRequest request = createRequest("BOGUS", "msg", "svc");

        assertThatThrownBy(() -> logIngestionService.ingest(request, TEAM_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid log level");
    }

    @Test
    void testIngest_updatesSourceStats() {
        IngestLogEntryRequest request = createRequest("INFO", "msg", "svc");
        LogSource source = createSource("svc");
        source.setLogCount(5L);
        LogEntry entity = new LogEntry();

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(request)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingest(request, TEAM_ID);

        assertThat(source.getLogCount()).isEqualTo(6L);
        assertThat(source.getLastLogReceivedAt()).isNotNull();
    }

    @Test
    void testIngestBatch_allValid_returnsFullCount() {
        List<IngestLogEntryRequest> requests = List.of(
                createRequest("INFO", "msg1", "svc"),
                createRequest("WARN", "msg2", "svc"),
                createRequest("ERROR", "msg3", "svc"),
                createRequest("DEBUG", "msg4", "svc"),
                createRequest("TRACE", "msg5", "svc")
        );
        LogSource source = createSource("svc");

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(any(IngestLogEntryRequest.class))).thenReturn(new LogEntry());
        when(logEntryRepository.saveAll(anyList())).thenReturn(List.of());
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);

        int count = logIngestionService.ingestBatch(requests, TEAM_ID);

        assertThat(count).isEqualTo(5);
        verify(logEntryRepository).saveAll(anyList());
    }

    @Test
    void testIngestBatch_someInvalid_returnsPartialCount() {
        List<IngestLogEntryRequest> requests = new ArrayList<>();
        requests.add(createRequest("INFO", "valid1", "svc"));
        requests.add(createRequest("WARN", "valid2", "svc"));
        requests.add(createRequest("BOGUS", "invalid", "svc"));
        requests.add(createRequest("ERROR", "valid3", "svc"));
        requests.add(createRequest("NOPE", "invalid2", "svc"));

        LogSource source = createSource("svc");
        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(any(IngestLogEntryRequest.class))).thenReturn(new LogEntry());
        when(logEntryRepository.saveAll(anyList())).thenReturn(List.of());
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);

        int count = logIngestionService.ingestBatch(requests, TEAM_ID);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void testIngestBatch_emptyList_returnsZero() {
        int count = logIngestionService.ingestBatch(List.of(), TEAM_ID);
        assertThat(count).isEqualTo(0);
        verifyNoInteractions(logEntryRepository);
    }

    @Test
    void testIngestBatch_exceedsMaxSize_throwsValidation() {
        List<IngestLogEntryRequest> requests = new ArrayList<>();
        for (int i = 0; i < AppConstants.MAX_BATCH_SIZE + 1; i++) {
            requests.add(createRequest("INFO", "msg" + i, "svc"));
        }

        assertThatThrownBy(() -> logIngestionService.ingestBatch(requests, TEAM_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    void testIngestBatch_cachesSourceLookup() {
        List<IngestLogEntryRequest> requests = List.of(
                createRequest("INFO", "msg1", "same-svc"),
                createRequest("INFO", "msg2", "same-svc"),
                createRequest("INFO", "msg3", "same-svc")
        );
        LogSource source = createSource("same-svc");

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "same-svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(any(IngestLogEntryRequest.class))).thenReturn(new LogEntry());
        when(logEntryRepository.saveAll(anyList())).thenReturn(List.of());
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);

        logIngestionService.ingestBatch(requests, TEAM_ID);

        verify(logSourceRepository, times(1)).findByTeamIdAndName(TEAM_ID, "same-svc");
    }

    @Test
    void testIngestRaw_jsonInput_parsesAndIngests() {
        String rawJson = "{\"level\":\"INFO\",\"message\":\"test\"}";
        IngestLogEntryRequest parsed = createRequest("INFO", "test", "svc");
        LogSource source = createSource("svc");
        LogEntry entity = new LogEntry();

        when(logParsingService.parse(rawJson, "svc")).thenReturn(parsed);
        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(parsed)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingestRaw(rawJson, "svc", TEAM_ID);

        verify(logParsingService).parse(rawJson, "svc");
        verify(logEntryRepository).save(any(LogEntry.class));
    }

    @Test
    void testIngestRaw_plainTextInput_parsesAndIngests() {
        String rawText = "Some plain text log";
        IngestLogEntryRequest parsed = createRequest("INFO", rawText, "default-svc");
        LogSource source = createSource("default-svc");
        LogEntry entity = new LogEntry();

        when(logParsingService.parse(rawText, "default-svc")).thenReturn(parsed);
        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "default-svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(parsed)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingestRaw(rawText, "default-svc", TEAM_ID);

        verify(logParsingService).parse(rawText, "default-svc");
    }

    @Test
    void testIngestRaw_exceptionCaught_doesNotPropagate() {
        when(logParsingService.parse(anyString(), anyString()))
                .thenThrow(new RuntimeException("Parse error"));

        logIngestionService.ingestRaw("bad data", "svc", TEAM_ID);

        verifyNoInteractions(logEntryRepository);
    }

    @Test
    void testIngest_publishesEvent() {
        IngestLogEntryRequest request = createRequest("INFO", "Test event", "svc");
        LogSource source = createSource("svc");
        LogEntry entity = new LogEntry();
        entity.setId(UUID.randomUUID());

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(request)).thenReturn(entity);
        when(logEntryRepository.save(any(LogEntry.class))).thenReturn(entity);
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);
        when(logEntryMapper.toResponse(any(LogEntry.class))).thenReturn(mock(LogEntryResponse.class));

        logIngestionService.ingest(request, TEAM_ID);

        ArgumentCaptor<LogEntryIngestedEvent> eventCaptor = ArgumentCaptor.forClass(LogEntryIngestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getLogEntry()).isEqualTo(entity);
    }

    @Test
    void testIngestBatch_publishesEventForEach() {
        List<IngestLogEntryRequest> requests = List.of(
                createRequest("INFO", "msg1", "svc"),
                createRequest("WARN", "msg2", "svc")
        );
        LogSource source = createSource("svc");
        LogEntry entry1 = new LogEntry();
        entry1.setId(UUID.randomUUID());
        LogEntry entry2 = new LogEntry();
        entry2.setId(UUID.randomUUID());

        when(logSourceRepository.findByTeamIdAndName(TEAM_ID, "svc")).thenReturn(Optional.of(source));
        when(logEntryMapper.toEntity(any(IngestLogEntryRequest.class))).thenReturn(new LogEntry());
        when(logEntryRepository.saveAll(anyList())).thenReturn(List.of(entry1, entry2));
        when(logSourceRepository.save(any(LogSource.class))).thenReturn(source);

        logIngestionService.ingestBatch(requests, TEAM_ID);

        ArgumentCaptor<LogEntryIngestedEvent> eventCaptor = ArgumentCaptor.forClass(LogEntryIngestedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<LogEntryIngestedEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getLogEntry()).isEqualTo(entry1);
        assertThat(events.get(1).getLogEntry()).isEqualTo(entry2);
    }
}
