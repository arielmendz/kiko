package com.kiko.app;

import java.util.Objects;

final class BodyProtocolCommand {
    enum Type {
        GET_CAPABILITIES,
        MOVE_STEPS,
        DANCE,
        HEARTBEAT,
        STOP
    }

    private final String commandId;
    private final Type type;
    private final Integer stepCount;
    private final String routineId;
    private final int timeoutMs;

    private BodyProtocolCommand(
            String commandId,
            Type type,
            Integer stepCount,
            String routineId,
            int timeoutMs
    ) {
        this.commandId = Objects.requireNonNull(commandId);
        this.type = Objects.requireNonNull(type);
        this.stepCount = stepCount;
        this.routineId = routineId;
        this.timeoutMs = timeoutMs;
    }

    static BodyProtocolCommand capabilities(String commandId) {
        return new BodyProtocolCommand(
                commandId,
                Type.GET_CAPABILITIES,
                null,
                null,
                1_000
        );
    }

    static BodyProtocolCommand heartbeat(String commandId) {
        return new BodyProtocolCommand(
                commandId,
                Type.HEARTBEAT,
                null,
                null,
                1_000
        );
    }

    static BodyProtocolCommand fromBodyCommand(BodyCommand command) {
        Type protocolType;
        switch (command.getType()) {
            case MOVE_STEPS:
                protocolType = Type.MOVE_STEPS;
                break;
            case DANCE:
                protocolType = Type.DANCE;
                break;
            case STOP:
            default:
                protocolType = Type.STOP;
                break;
        }
        return new BodyProtocolCommand(
                command.getCommandId(),
                protocolType,
                command.getStepCount(),
                command.getRoutineId(),
                command.getTimeoutMs()
        );
    }

    static BodyProtocolCommand decoded(
            String commandId,
            Type type,
            Integer stepCount,
            String routineId,
            int timeoutMs
    ) {
        return new BodyProtocolCommand(
                commandId,
                type,
                stepCount,
                routineId,
                timeoutMs
        );
    }

    String getCommandId() {
        return commandId;
    }

    Type getType() {
        return type;
    }

    Integer getStepCount() {
        return stepCount;
    }

    String getRoutineId() {
        return routineId;
    }

    int getTimeoutMs() {
        return timeoutMs;
    }
}
