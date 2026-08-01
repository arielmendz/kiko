package com.kiko.app;

import java.util.Objects;

final class BodyCommand {
    private final String commandId;
    private final BodyActionRequest.Type type;
    private final Integer stepCount;
    private final String routineId;
    private final int timeoutMs;

    BodyCommand(
            String commandId,
            BodyActionRequest.Type type,
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

    String getCommandId() {
        return commandId;
    }

    BodyActionRequest.Type getType() {
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
