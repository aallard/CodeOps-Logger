package com.codeops.logger.service;

import com.codeops.logger.dto.request.IngestLogEntryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaLogConsumer}.
 */
@ExtendWith(MockitoExtension.class)
class KafkaLogConsumerTest {

    @Mock
    private LogIngestionService logIngestionService;

    private KafkaLogConsumer kafkaLogConsumer;

    private static final UUID TEAM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        kafkaLogConsumer = new KafkaLogConsumer(logIngestionService, mapper);
    }

    @Test
    void testConsume_validJsonMessage_ingestsSuccessfully() {
        String json = """
                {"level":"ERROR","message":"DB timeout","serviceName":"codeops-server",
                 "timestamp":"2026-01-15T10:30:45Z"}
                """;

        when(logIngestionService.ingest(any(IngestLogEntryRequest.class), any(UUID.class)))
                .thenReturn(null);

        kafkaLogConsumer.consume(json, "codeops-server", TEAM_ID.toString());

        ArgumentCaptor<IngestLogEntryRequest> captor = ArgumentCaptor.forClass(IngestLogEntryRequest.class);
        verify(logIngestionService).ingest(captor.capture(), eq(TEAM_ID));

        IngestLogEntryRequest captured = captor.getValue();
        assertThat(captured.level()).isEqualTo("ERROR");
        assertThat(captured.message()).isEqualTo("DB timeout");
        assertThat(captured.serviceName()).isEqualTo("codeops-server");
    }

    @Test
    void testConsume_rawTextMessage_parsesAndIngests() {
        String rawText = "Some non-JSON log line";

        kafkaLogConsumer.consume(rawText, "my-service", TEAM_ID.toString());

        verify(logIngestionService).ingestRaw(eq(rawText), eq("my-service"), eq(TEAM_ID));
    }

    @Test
    void testConsume_missingTeamIdHeader_skipsMessage() {
        kafkaLogConsumer.consume("{\"level\":\"INFO\",\"message\":\"test\"}", "svc", null);

        verifyNoInteractions(logIngestionService);
    }

    @Test
    void testConsume_messageKeyUsedAsServiceName() {
        String json = """
                {"level":"INFO","message":"hello"}
                """;

        when(logIngestionService.ingest(any(IngestLogEntryRequest.class), any(UUID.class)))
                .thenReturn(null);

        kafkaLogConsumer.consume(json, "key-service", TEAM_ID.toString());

        ArgumentCaptor<IngestLogEntryRequest> captor = ArgumentCaptor.forClass(IngestLogEntryRequest.class);
        verify(logIngestionService).ingest(captor.capture(), eq(TEAM_ID));
        assertThat(captor.getValue().serviceName()).isEqualTo("key-service");
    }

    @Test
    void testConsume_exceptionInIngestion_doesNotPropagate() {
        when(logIngestionService.ingest(any(IngestLogEntryRequest.class), any(UUID.class)))
                .thenThrow(new RuntimeException("DB down"));

        kafkaLogConsumer.consume(
                "{\"level\":\"INFO\",\"message\":\"test\",\"serviceName\":\"svc\"}",
                "svc", TEAM_ID.toString()
        );

        verify(logIngestionService).ingest(any(), any());
    }

    @Test
    void testConsume_nullPayload_handledGracefully() {
        kafkaLogConsumer.consume(null, "svc", TEAM_ID.toString());
        verifyNoInteractions(logIngestionService);
    }

    @Test
    void testConsume_emptyPayload_handledGracefully() {
        kafkaLogConsumer.consume("", "svc", TEAM_ID.toString());
        verifyNoInteractions(logIngestionService);
    }
}
