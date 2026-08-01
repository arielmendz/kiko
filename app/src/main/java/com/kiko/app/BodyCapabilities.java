package com.kiko.app;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class BodyCapabilities {
    static final int PROTOCOL_VERSION = 1;

    private final int protocolVersion;
    private final int maxStepsPerCommand;
    private final Set<String> routineIds;
    private final boolean supportsStop;
    private final int linkWatchdogMs;
    private final int servoCount;

    BodyCapabilities(
            int protocolVersion,
            int maxStepsPerCommand,
            Set<String> routineIds,
            boolean supportsStop,
            int linkWatchdogMs,
            int servoCount
    ) {
        this.protocolVersion = protocolVersion;
        this.maxStepsPerCommand = maxStepsPerCommand;
        this.routineIds = Collections.unmodifiableSet(
                new LinkedHashSet<>(routineIds)
        );
        this.supportsStop = supportsStop;
        this.linkWatchdogMs = linkWatchdogMs;
        this.servoCount = servoCount;
    }

    static BodyCapabilities loopback() {
        return new BodyCapabilities(
                PROTOCOL_VERSION,
                6,
                Collections.singleton("seal_wiggle"),
                true,
                750,
                2
        );
    }

    int getProtocolVersion() {
        return protocolVersion;
    }

    int getMaxStepsPerCommand() {
        return maxStepsPerCommand;
    }

    boolean supportsRoutine(String routineId) {
        return routineIds.contains(routineId);
    }

    Set<String> getRoutineIds() {
        return routineIds;
    }

    boolean supportsStop() {
        return supportsStop;
    }

    int getLinkWatchdogMs() {
        return linkWatchdogMs;
    }

    int getServoCount() {
        return servoCount;
    }
}
