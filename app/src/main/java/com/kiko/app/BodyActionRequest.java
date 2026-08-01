package com.kiko.app;

import java.util.Objects;

final class BodyActionRequest {
    enum Type {
        MOVE_STEPS,
        DANCE,
        STOP
    }

    private final Type type;
    private final Integer stepCount;
    private final String routineId;

    private BodyActionRequest(Type type, Integer stepCount, String routineId) {
        this.type = Objects.requireNonNull(type);
        this.stepCount = stepCount;
        this.routineId = routineId;
    }

    static BodyActionRequest moveSteps(int count) {
        return new BodyActionRequest(Type.MOVE_STEPS, count, null);
    }

    static BodyActionRequest dance() {
        return new BodyActionRequest(Type.DANCE, null, "seal_wiggle");
    }

    static BodyActionRequest stop() {
        return new BodyActionRequest(Type.STOP, null, null);
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
}
