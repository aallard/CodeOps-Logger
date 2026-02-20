package com.codeops.logger.event;

import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.entity.LogSource;
import com.codeops.logger.entity.enums.LogLevel;
import com.codeops.logger.service.AlertService;
import com.codeops.logger.service.LogTrapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LogEntryEventListener}.
 * Verifies the trap evaluation → alert firing pipeline.
 */
@ExtendWith(MockitoExtension.class)
class LogEntryEventListenerTest {

    @Mock
    private LogTrapService logTrapService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private LogEntryEventListener listener;

    private LogEntry createEntry() {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID());
        entry.setTeamId(UUID.randomUUID());
        entry.setLevel(LogLevel.ERROR);
        entry.setMessage("NullPointerException in UserService.getUser()");
        entry.setTimestamp(Instant.now());
        LogSource source = new LogSource();
        source.setName("user-service");
        entry.setSource(source);
        return entry;
    }

    private LogEntryIngestedEvent createEvent(LogEntry entry) {
        return new LogEntryIngestedEvent(this, entry);
    }

    /**
     * Verifies that when no traps match, no alerts are fired.
     */
    @Test
    void testOnLogEntryIngested_noTrapsMatch_noAlertsFired() {
        LogEntry entry = createEntry();
        when(logTrapService.evaluateEntry(entry)).thenReturn(List.of());

        listener.onLogEntryIngested(createEvent(entry));

        verify(logTrapService).evaluateEntry(entry);
        verifyNoInteractions(alertService);
    }

    /**
     * Verifies that when a trap matches, alerts are fired with the correct trap ID.
     */
    @Test
    void testOnLogEntryIngested_trapMatches_alertsFired() {
        LogEntry entry = createEntry();
        UUID trapId = UUID.randomUUID();
        when(logTrapService.evaluateEntry(entry)).thenReturn(List.of(trapId));

        listener.onLogEntryIngested(createEvent(entry));

        verify(alertService).fireAlerts(eq(trapId), anyString());
    }

    /**
     * Verifies that when multiple traps fire, alerts are processed for each.
     */
    @Test
    void testOnLogEntryIngested_multipleTrapsFire_allAlertsProcessed() {
        LogEntry entry = createEntry();
        UUID trapId1 = UUID.randomUUID();
        UUID trapId2 = UUID.randomUUID();
        UUID trapId3 = UUID.randomUUID();
        when(logTrapService.evaluateEntry(entry)).thenReturn(List.of(trapId1, trapId2, trapId3));

        listener.onLogEntryIngested(createEvent(entry));

        verify(alertService).fireAlerts(eq(trapId1), anyString());
        verify(alertService).fireAlerts(eq(trapId2), anyString());
        verify(alertService).fireAlerts(eq(trapId3), anyString());
        verify(alertService, times(3)).fireAlerts(any(UUID.class), anyString());
    }

    /**
     * Verifies that trap evaluation failure is caught and does not throw.
     */
    @Test
    void testOnLogEntryIngested_trapEvaluationFails_doesNotThrow() {
        LogEntry entry = createEntry();
        when(logTrapService.evaluateEntry(entry)).thenThrow(new RuntimeException("DB connection lost"));

        listener.onLogEntryIngested(createEvent(entry));

        verifyNoInteractions(alertService);
    }

    /**
     * Verifies that alert firing failure for one trap does not stop processing other traps.
     */
    @Test
    void testOnLogEntryIngested_alertFiringFails_continuesOtherTraps() {
        LogEntry entry = createEntry();
        UUID trapId1 = UUID.randomUUID();
        UUID trapId2 = UUID.randomUUID();
        when(logTrapService.evaluateEntry(entry)).thenReturn(List.of(trapId1, trapId2));
        doThrow(new RuntimeException("Channel unavailable"))
                .when(alertService).fireAlerts(eq(trapId1), anyString());

        listener.onLogEntryIngested(createEvent(entry));

        verify(alertService).fireAlerts(eq(trapId1), anyString());
        verify(alertService).fireAlerts(eq(trapId2), anyString());
    }
}
