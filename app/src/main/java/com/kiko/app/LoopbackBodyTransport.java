package com.kiko.app;

final class LoopbackBodyTransport implements BodyTransport {
    private static final String CAPABILITIES_COMMAND_ID = "loopback-capabilities";

    private final BodyProtocolCodec codec;
    private final BodyWireLink link;
    private final BodyCapabilities capabilities;
    private final long heartbeatIntervalMs;

    private String activeCommandId;
    private BodyProtocolCommand.Type activeAction;
    private long nextHeartbeatMs;
    private long heartbeatSequence;

    LoopbackBodyTransport() {
        this(new BodyProtocolCodec(), new LoopbackBodyPeer());
    }

    LoopbackBodyTransport(BodyProtocolCodec codec, BodyWireLink link) {
        this.codec = codec;
        this.link = link;
        capabilities = negotiateCapabilities();
        heartbeatIntervalMs = Math.max(
                1L,
                capabilities.getLinkWatchdogMs() / 2L
        );
    }

    @Override
    public BodyCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public BodyEvent send(BodyCommand command, long nowMs) {
        BodyProtocolCommand protocolCommand =
                BodyProtocolCommand.fromBodyCommand(command);
        BodyProtocolEvent event;
        try {
            event = roundTrip(protocolCommand, nowMs);
        } catch (BodyProtocolException error) {
            return wireFailure(command.getCommandId());
        }

        if (!event.getCommandId().equals(command.getCommandId())) {
            return wireFailure(command.getCommandId());
        }
        if (event.getType() != BodyProtocolEvent.Type.ACCEPTED
                && event.getType() != BodyProtocolEvent.Type.COMPLETED
                && event.getType() != BodyProtocolEvent.Type.STOPPED
                && event.getType() != BodyProtocolEvent.Type.REJECTED) {
            return wireFailure(command.getCommandId());
        }
        if (event.getType() == BodyProtocolEvent.Type.ACCEPTED) {
            if (event.getAction() != protocolCommand.getType()) {
                return wireFailure(command.getCommandId());
            }
            activeCommandId = command.getCommandId();
            activeAction = protocolCommand.getType();
            nextHeartbeatMs = nowMs + heartbeatIntervalMs;
        } else if (event.getType() == BodyProtocolEvent.Type.COMPLETED) {
            clearActive();
        } else if (event.getType() == BodyProtocolEvent.Type.STOPPED) {
            clearActive();
        }
        return toBodyEvent(event, command.getCommandId());
    }

    @Override
    public BodyEvent tick(long nowMs) {
        if (activeCommandId == null) {
            return null;
        }

        byte[] peerEvent = link.tick(nowMs);
        if (peerEvent != null) {
            return consumeActiveEvent(peerEvent);
        }

        if (nowMs >= nextHeartbeatMs) {
            String heartbeatId = "hb-" + (++heartbeatSequence);
            BodyProtocolEvent heartbeat;
            try {
                heartbeat = roundTrip(
                        BodyProtocolCommand.heartbeat(heartbeatId),
                        nowMs
                );
            } catch (BodyProtocolException error) {
                return wireFailure(activeCommandId);
            }
            if (!heartbeatId.equals(heartbeat.getCommandId())
                    || heartbeat.getType() != BodyProtocolEvent.Type.ALIVE
                    || !Boolean.TRUE.equals(heartbeat.isMoving())) {
                return wireFailure(activeCommandId);
            }
            nextHeartbeatMs = nowMs + heartbeatIntervalMs;
        }
        return null;
    }

    @Override
    public BodyEvent disconnect() {
        String interruptedCommandId = activeCommandId;
        byte[] peerEvent = link.disconnect();
        clearActive();
        if (peerEvent == null) {
            return null;
        }
        try {
            BodyProtocolEvent event = codec.decodeEvent(peerEvent);
            if (interruptedCommandId != null
                    && !interruptedCommandId.equals(event.getCommandId())) {
                return BodyEvent.stopped(
                        interruptedCommandId,
                        "invalid_telemetry",
                        interruptedCommandId
                );
            }
            return toBodyEvent(event, interruptedCommandId);
        } catch (BodyProtocolException error) {
            return interruptedCommandId == null
                    ? BodyEvent.rejected("invalid", "invalid_telemetry")
                    : BodyEvent.stopped(
                            interruptedCommandId,
                            "invalid_telemetry",
                            interruptedCommandId
                    );
        }
    }

    @Override
    public boolean isActive() {
        return activeCommandId != null;
    }

    private BodyCapabilities negotiateCapabilities() {
        try {
            BodyProtocolEvent event = roundTrip(
                    BodyProtocolCommand.capabilities(CAPABILITIES_COMMAND_ID),
                    0L
            );
            if (event.getType() != BodyProtocolEvent.Type.CAPABILITIES
                    || !CAPABILITIES_COMMAND_ID.equals(event.getCommandId())
                    || event.getCapabilities() == null) {
                throw new IllegalStateException("invalid_loopback_capabilities");
            }
            return event.getCapabilities();
        } catch (BodyProtocolException error) {
            throw new IllegalStateException("loopback_capability_negotiation_failed", error);
        }
    }

    private BodyProtocolEvent roundTrip(
            BodyProtocolCommand command,
            long nowMs
    ) throws BodyProtocolException {
        byte[] commandPayload = codec.encodeCommand(command);
        byte[] eventPayload = link.write(commandPayload, nowMs);
        if (eventPayload == null) {
            throw new BodyProtocolException("missing_event");
        }
        return codec.decodeEvent(eventPayload);
    }

    private BodyEvent consumeActiveEvent(byte[] payload) {
        String expectedCommandId = activeCommandId;
        try {
            BodyProtocolEvent event = codec.decodeEvent(payload);
            if (!expectedCommandId.equals(event.getCommandId())) {
                return wireFailure(expectedCommandId);
            }
            if (event.getType() != BodyProtocolEvent.Type.COMPLETED
                    && event.getType() != BodyProtocolEvent.Type.STOPPED) {
                return wireFailure(expectedCommandId);
            }
            if (event.getType() == BodyProtocolEvent.Type.COMPLETED
                    && event.getAction() != activeAction) {
                return wireFailure(expectedCommandId);
            }
            BodyEvent bodyEvent = toBodyEvent(event, expectedCommandId);
            if (bodyEvent != null) {
                clearActive();
            }
            return bodyEvent;
        } catch (BodyProtocolException error) {
            return wireFailure(expectedCommandId);
        }
    }

    private BodyEvent wireFailure(String commandId) {
        link.disconnect();
        boolean wasActive = activeCommandId != null;
        String failedCommandId = wasActive ? activeCommandId : commandId;
        clearActive();
        if (wasActive) {
            return BodyEvent.stopped(
                    failedCommandId,
                    "invalid_telemetry",
                    failedCommandId
            );
        }
        return BodyEvent.rejected(failedCommandId, "invalid_telemetry");
    }

    private static BodyEvent toBodyEvent(
            BodyProtocolEvent event,
            String fallbackCommandId
    ) {
        switch (event.getType()) {
            case ACCEPTED:
                return BodyEvent.accepted(
                        event.getCommandId(),
                        event.getEstimatedDurationMs()
                );
            case COMPLETED:
                return BodyEvent.completed(event.getCommandId());
            case STOPPED:
                return BodyEvent.stopped(
                        event.getCommandId(),
                        event.getReason(),
                        event.getStoppedCommandId()
                );
            case REJECTED:
                return BodyEvent.rejected(
                        event.getCommandId(),
                        event.getReason()
                );
            case CAPABILITIES:
            case ALIVE:
            default:
                return BodyEvent.rejected(
                        fallbackCommandId == null ? "invalid" : fallbackCommandId,
                        "unexpected_event"
                );
        }
    }

    private void clearActive() {
        activeCommandId = null;
        activeAction = null;
        nextHeartbeatMs = 0L;
    }
}
