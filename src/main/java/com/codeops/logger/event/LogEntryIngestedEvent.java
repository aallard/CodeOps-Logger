package com.codeops.logger.event;

import com.codeops.logger.entity.LogEntry;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new log entry is successfully ingested.
 * Consumed by the trap evaluation pipeline to check for pattern matches
 * and trigger alerts.
 */
public class LogEntryIngestedEvent extends ApplicationEvent {

    private final LogEntry logEntry;

    /**
     * Creates a new log entry ingested event.
     *
     * @param source   the object that published the event
     * @param logEntry the ingested log entry
     */
    public LogEntryIngestedEvent(Object source, LogEntry logEntry) {
        super(source);
        this.logEntry = logEntry;
    }

    /**
     * Returns the ingested log entry.
     *
     * @return the log entry
     */
    public LogEntry getLogEntry() {
        return logEntry;
    }
}
