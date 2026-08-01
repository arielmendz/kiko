package com.kiko.app;

import java.util.LinkedHashMap;
import java.util.Map;

final class LoopbackBodyPeer implements BodyWireLink {
    static final long STEP_DURATION_MS = 1_000L;
    static final long DANCE_DURATION_MS = 2_400L;
    private static final int RESPONSE_CACHE_SIZE = 128;

    private final BodyProtocolCodec codec;
    private final BodyCapabilities capabilities;
    private final LinkedHashMap<String, byte[]> responses = new LinkedHashMap<>(
            RESPONSE_CACHE_SIZE,
            0.75f,
            true
    );

    private BodyProtocolCommand activeCommand;
    private long finishAtMs;
    private long deadlineAtMs;
    private Long lastContactMs;

    LoopbackBodyPeer() {
        this(new BodyProtocolCodec(), BodyCapabilities.loopback());
    }

    LoopbackBodyPeer(
            BodyProtocolCodec codec,
            BodyCapabilities capabilities
    ) {
        this.codec = codec;
        this.capabilities = capabilities;
    }

    @Override
    public byte[] write(byte[] commandPayload, long nowMs) {
        BodyProtocolCommand command;
        try {
            command = codec.decodeCommand(commandPayload);
        } catch (BodyProtocolException error) {
            return encode(BodyProtocolEvent.rejected(
                    "invalid",
                    error.getMessage()
            ));
        }

        lastContactMs = nowMs;
        byte[] cached = responses.get(command.getCommandId());
        if (cached != null) {
            return cached.clone();
        }

        switch (command.getType()) {
            case GET_CAPABILITIES:
                return remember(BodyProtocolEvent.capabilities(
                        command.getCommandId(),
                        capabilities
                ));
            case HEARTBEAT:
                return remember(BodyProtocolEvent.alive(
                        command.getCommandId(),
                        activeCommand != null
                ));
            case STOP:
                String stoppedCommandId = activeCommand == null
                        ? null
                        : activeCommand.getCommandId();
                clearActive();
                return remember(BodyProtocolEvent.stopped(
                        command.getCommandId(),
                        "requested",
                        stoppedCommandId
                ));
            case MOVE_STEPS:
            case DANCE:
            default:
                return startMotion(command, nowMs);
        }
    }

    @Override
    public byte[] tick(long nowMs) {
        if (activeCommand == null) {
            return null;
        }

        String commandId = activeCommand.getCommandId();
        if (lastContactMs == null
                || nowMs - lastContactMs > capabilities.getLinkWatchdogMs()) {
            return stopActive("link_watchdog");
        }
        if (nowMs >= deadlineAtMs) {
            return stopActive("deadline_expired");
        }
        if (nowMs >= finishAtMs) {
            BodyProtocolCommand.Type action = activeCommand.getType();
            clearActive();
            return remember(BodyProtocolEvent.completed(commandId, action));
        }
        return null;
    }

    @Override
    public byte[] disconnect() {
        if (activeCommand == null) {
            return null;
        }
        return stopActive("ble_disconnected");
    }

    boolean isActive() {
        return activeCommand != null;
    }

    private byte[] startMotion(BodyProtocolCommand command, long nowMs) {
        if (activeCommand != null) {
            return remember(BodyProtocolEvent.rejected(
                    command.getCommandId(),
                    "body_busy"
            ));
        }

        long durationMs;
        if (command.getType() == BodyProtocolCommand.Type.MOVE_STEPS) {
            Integer count = command.getStepCount();
            if (count == null
                    || count < 1
                    || count > capabilities.getMaxStepsPerCommand()) {
                return remember(BodyProtocolEvent.rejected(
                        command.getCommandId(),
                        "count_out_of_range"
                ));
            }
            durationMs = STEP_DURATION_MS * count;
        } else if (command.getType() == BodyProtocolCommand.Type.DANCE) {
            if (!capabilities.supportsRoutine(command.getRoutineId())) {
                return remember(BodyProtocolEvent.rejected(
                        command.getCommandId(),
                        "unknown_routine"
                ));
            }
            durationMs = DANCE_DURATION_MS;
        } else {
            return remember(BodyProtocolEvent.rejected(
                    command.getCommandId(),
                    "unsupported_command"
            ));
        }

        if (command.getTimeoutMs() <= durationMs) {
            return remember(BodyProtocolEvent.rejected(
                    command.getCommandId(),
                    "timeout_too_short"
            ));
        }

        activeCommand = command;
        finishAtMs = nowMs + durationMs;
        deadlineAtMs = nowMs + command.getTimeoutMs();
        return remember(BodyProtocolEvent.accepted(
                command.getCommandId(),
                command.getType(),
                durationMs
        ));
    }

    private byte[] stopActive(String reason) {
        String commandId = activeCommand.getCommandId();
        clearActive();
        return remember(BodyProtocolEvent.stopped(
                commandId,
                reason,
                commandId
        ));
    }

    private void clearActive() {
        activeCommand = null;
        finishAtMs = 0L;
        deadlineAtMs = 0L;
    }

    private byte[] remember(BodyProtocolEvent event) {
        byte[] payload = encode(event);
        responses.put(event.getCommandId(), payload);
        while (responses.size() > RESPONSE_CACHE_SIZE) {
            String eldest = null;
            for (Map.Entry<String, byte[]> entry : responses.entrySet()) {
                eldest = entry.getKey();
                break;
            }
            if (eldest != null) {
                responses.remove(eldest);
            }
        }
        return payload.clone();
    }

    private byte[] encode(BodyProtocolEvent event) {
        try {
            return codec.encodeEvent(event);
        } catch (BodyProtocolException error) {
            throw new IllegalStateException("loopback_event_encoding_failed", error);
        }
    }
}
