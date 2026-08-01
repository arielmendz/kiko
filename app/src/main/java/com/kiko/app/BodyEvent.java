package com.kiko.app;

final class BodyEvent {
    enum Type {
        ACCEPTED,
        COMPLETED,
        STOPPED,
        REJECTED
    }

    private final Type type;
    private final String commandId;
    private final String reason;
    private final String stoppedCommandId;
    private final long estimatedDurationMs;

    private BodyEvent(
            Type type,
            String commandId,
            String reason,
            String stoppedCommandId,
            long estimatedDurationMs
    ) {
        this.type = type;
        this.commandId = commandId;
        this.reason = reason;
        this.stoppedCommandId = stoppedCommandId;
        this.estimatedDurationMs = estimatedDurationMs;
    }

    static BodyEvent accepted(String commandId, long durationMs) {
        return new BodyEvent(Type.ACCEPTED, commandId, null, null, durationMs);
    }

    static BodyEvent completed(String commandId) {
        return new BodyEvent(Type.COMPLETED, commandId, null, null, 0L);
    }

    static BodyEvent stopped(
            String commandId,
            String reason,
            String stoppedCommandId
    ) {
        return new BodyEvent(Type.STOPPED, commandId, reason, stoppedCommandId, 0L);
    }

    static BodyEvent rejected(String commandId, String reason) {
        return new BodyEvent(Type.REJECTED, commandId, reason, null, 0L);
    }

    Type getType() {
        return type;
    }

    String getCommandId() {
        return commandId;
    }

    String getReason() {
        return reason;
    }

    String getStoppedCommandId() {
        return stoppedCommandId;
    }

    long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }
}
