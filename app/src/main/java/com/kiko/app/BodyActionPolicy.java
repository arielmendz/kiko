package com.kiko.app;

import java.util.Objects;

final class BodyActionPolicy {
    enum Rejection {
        COUNT_OUT_OF_RANGE,
        ROUTINE_UNAVAILABLE,
        STOP_UNAVAILABLE
    }

    static final class Decision {
        private final BodyCommand command;
        private final Rejection rejection;

        private Decision(BodyCommand command, Rejection rejection) {
            this.command = command;
            this.rejection = rejection;
        }

        static Decision allowed(BodyCommand command) {
            return new Decision(command, null);
        }

        static Decision rejected(Rejection rejection) {
            return new Decision(null, rejection);
        }

        boolean isAllowed() {
            return command != null;
        }

        BodyCommand getCommand() {
            return command;
        }

        Rejection getRejection() {
            return rejection;
        }
    }

    private static final int MOVE_TIMEOUT_MS = 10_000;
    private static final int DANCE_TIMEOUT_MS = 5_000;
    private static final int STOP_TIMEOUT_MS = 100;

    Decision authorize(
            BodyActionRequest request,
            BodyCapabilities capabilities,
            String commandId
    ) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(capabilities);

        switch (request.getType()) {
            case MOVE_STEPS:
                int count = request.getStepCount() == null
                        ? 0
                        : request.getStepCount();
                if (count < 1 || count > capabilities.getMaxStepsPerCommand()) {
                    return Decision.rejected(Rejection.COUNT_OUT_OF_RANGE);
                }
                return Decision.allowed(new BodyCommand(
                        commandId,
                        BodyActionRequest.Type.MOVE_STEPS,
                        count,
                        null,
                        MOVE_TIMEOUT_MS
                ));
            case DANCE:
                if (!capabilities.supportsRoutine(request.getRoutineId())) {
                    return Decision.rejected(Rejection.ROUTINE_UNAVAILABLE);
                }
                return Decision.allowed(new BodyCommand(
                        commandId,
                        BodyActionRequest.Type.DANCE,
                        null,
                        request.getRoutineId(),
                        DANCE_TIMEOUT_MS
                ));
            case STOP:
            default:
                if (!capabilities.supportsStop()) {
                    return Decision.rejected(Rejection.STOP_UNAVAILABLE);
                }
                return Decision.allowed(new BodyCommand(
                        commandId,
                        BodyActionRequest.Type.STOP,
                        null,
                        null,
                        STOP_TIMEOUT_MS
                ));
        }
    }
}
