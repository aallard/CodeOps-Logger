package com.codeops.logger.event;

import com.codeops.logger.entity.LogEntry;
import com.codeops.logger.service.AlertService;
import com.codeops.logger.service.LogTrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Listens for log entry ingestion events and triggers trap evaluation
 * and alert firing. This wires together the complete pipeline:
 * Log Ingested → Trap Evaluates → Alert Fires → Notification Sent.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LogEntryEventListener {

    private final LogTrapService logTrapService;
    private final AlertService alertService;

    /**
     * Handles a log entry ingestion event by evaluating all active traps
     * for the entry's team and firing any matching alerts.
     *
     * <p>Runs asynchronously so trap evaluation never blocks log ingestion.
     * Failures are caught and logged — they never propagate back to the publisher.</p>
     *
     * @param event the log entry ingested event
     */
    @Async
    @EventListener
    public void onLogEntryIngested(LogEntryIngestedEvent event) {
        LogEntry entry = event.getLogEntry();
        try {
            List<UUID> firedTrapIds = logTrapService.evaluateEntry(entry);

            if (!firedTrapIds.isEmpty()) {
                log.info("Log entry {} triggered {} trap(s): {}",
                        entry.getId(), firedTrapIds.size(), firedTrapIds);
                for (UUID trapId : firedTrapIds) {
                    try {
                        String triggerMessage = String.format(
                                "Log entry [%s] %s: %s",
                                entry.getLevel(),
                                entry.getSource() != null ? entry.getSource().getName() : "unknown",
                                truncateForAlert(entry.getMessage()));
                        alertService.fireAlerts(trapId, triggerMessage);
                    } catch (Exception e) {
                        log.error("Failed to fire alerts for trap {}: {}",
                                trapId, e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Trap evaluation failed for log entry {}: {}",
                    entry.getId(), e.getMessage(), e);
        }
    }

    /**
     * Truncates a log message for use in alert notifications.
     *
     * @param message the original message
     * @return the truncated message (max 200 chars)
     */
    private String truncateForAlert(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }
        return message;
    }
}
