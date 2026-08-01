package com.kiko.app;

import java.util.Objects;

final class BodyProtocolEvent {
    enum Type {
        CAPABILITIES,
        ALIVE,
        ACCEPTED,
        COMPLETED,
        STOPPED,
        REJECTED
    }

    private final String commandId;
    private final Type type;
    private final BodyCapabilities capabilities;
    private final Boolean moving;
    private final BodyProtocolCommand.Type action;
    private final Long estimatedDurationMs;
    private final String reason;
    private final String stoppedCommandId;

    private BodyProtocolEvent(
            String commandId,
            Type type,
            BodyCapabilities capabilities,
            Boolean moving,
            BodyProtocolCommand.Type action,
            Long estimatedDurationMs,
            String reason,
            String stoppedCommandId
    ) {
        this.commandId = Objects.requireNonNull(commandId);
        this.type = Objects.requireNonNull(type);
        this.capabilities = capabilities;
        this.moving = moving;
        this.action = action;
        this.estimatedDurationMs = estimatedDurationMs;
        this.reason = reason;
        this.stoppedCommandId = stoppedCommandId;
    }

    static BodyProtocolEvent capabilities(
            String commandId,
            BodyCapabilities capabilities
    ) {
        return new BodyProtocolEvent(
                commandId,
                Type.CAPABILITIES,
                capabilities,
                null,
                null,
                null,
                null,
                null
        );
    }

    static BodyProtocolEvent alive(String commandId, boolean moving) {
        return new BodyProtocolEvent(
                commandId,
                Type.ALIVE,
                null,
                moving,
                null,
                null,
                null,
                null
        );
    }

    static BodyProtocolEvent accepted(
            String commandId,
            BodyProtocolCommand.Type action,
            long estimatedDurationMs
    ) {
        return new BodyProtocolEvent(
                commandId,
                Type.ACCEPTED,
                null,
                null,
                action,
                estimatedDurationMs,
                null,
                null
        );
    }

    static BodyProtocolEvent completed(
            String commandId,
            BodyProtocolCommand.Type action
    ) {
        return new BodyProtocolEvent(
                commandId,
                Type.COMPLETED,
                null,
                null,
                action,
                null,
                null,
                null
        );
    }

    static BodyProtocolEvent stopped(
            String commandId,
            String reason,
            String stoppedCommandId
    ) {
        return new BodyProtocolEvent(
                commandId,
                Type.STOPPED,
                null,
                null,
                null,
                null,
                reason,
                stoppedCommandId
        );
    }

    static BodyProtocolEvent rejected(String commandId, String reason) {
        return new BodyProtocolEvent(
                commandId,
                Type.REJECTED,
                null,
                null,
                null,
                null,
                reason,
                null
        );
    }

    String getCommandId() {
        return commandId;
    }

    Type getType() {
        return type;
    }

    BodyCapabilities getCapabilities() {
        return capabilities;
    }

    Boolean isMoving() {
        return moving;
    }

    BodyProtocolCommand.Type getAction() {
        return action;
    }

    Long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }

    String getReason() {
        return reason;
    }

    String getStoppedCommandId() {
        return stoppedCommandId;
    }
}
