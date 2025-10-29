package com.wakfu.domain.event;

import java.time.LocalDateTime;

public abstract class LogEvent {

    protected LocalDateTime timestamp;

    public LogEvent(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public abstract LogEventType getEventType();
}