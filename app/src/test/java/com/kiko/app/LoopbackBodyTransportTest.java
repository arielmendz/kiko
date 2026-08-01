package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class LoopbackBodyTransportTest {
    private final BodyActionPolicy policy = new BodyActionPolicy();

    @Test
    public void negotiatesCapabilitiesBeforePolicyAndCompletesWithHeartbeats() {
        LoopbackBodyTransport transport = new LoopbackBodyTransport();
        assertEquals(1, transport.getCapabilities().getProtocolVersion());
        assertEquals(6, transport.getCapabilities().getMaxStepsPerCommand());
        assertEquals(750, transport.getCapabilities().getLinkWatchdogMs());

        BodyCommand command = policy.authorize(
                BodyActionRequest.moveSteps(3),
                transport.getCapabilities(),
                "move-1"
        ).getCommand();
        BodyEvent accepted = transport.send(command, 0L);

        assertEquals(BodyEvent.Type.ACCEPTED, accepted.getType());
        assertEquals(3_000L, accepted.getEstimatedDurationMs());
        for (long nowMs = 100L; nowMs < 3_000L; nowMs += 100L) {
            assertNull(transport.tick(nowMs));
        }
        assertEquals(BodyEvent.Type.COMPLETED, transport.tick(3_000L).getType());
        assertFalse(transport.isActive());
    }

    @Test
    public void emergencyStopCrossesWireAndReportsStoppedCommand() {
        LoopbackBodyTransport transport = new LoopbackBodyTransport();
        BodyCommand move = command(
                transport,
                BodyActionRequest.moveSteps(2),
                "move-2"
        );
        BodyCommand stop = command(
                transport,
                BodyActionRequest.stop(),
                "stop-1"
        );
        transport.send(move, 0L);

        BodyEvent stopped = transport.send(stop, 200L);

        assertEquals(BodyEvent.Type.STOPPED, stopped.getType());
        assertEquals("requested", stopped.getReason());
        assertEquals("move-2", stopped.getStoppedCommandId());
        assertFalse(transport.isActive());
    }

    @Test
    public void missedClientPollTriggersPeerWatchdogBeforeLateHeartbeat() {
        LoopbackBodyTransport transport = new LoopbackBodyTransport();
        transport.send(command(
                transport,
                BodyActionRequest.moveSteps(2),
                "move-watchdog"
        ), 0L);

        BodyEvent stopped = transport.tick(751L);

        assertEquals(BodyEvent.Type.STOPPED, stopped.getType());
        assertEquals("link_watchdog", stopped.getReason());
        assertFalse(transport.isActive());
    }

    @Test
    public void duplicateCommandIdDoesNotRestartLoopbackMotion() {
        LoopbackBodyTransport transport = new LoopbackBodyTransport();
        BodyCommand move = command(
                transport,
                BodyActionRequest.moveSteps(1),
                "move-duplicate"
        );

        BodyEvent first = transport.send(move, 0L);
        BodyEvent duplicate = transport.send(move, 300L);

        assertEquals(BodyEvent.Type.ACCEPTED, first.getType());
        assertEquals(first.getType(), duplicate.getType());
        assertTrue(transport.isActive());
    }

    @Test
    public void lifecycleDisconnectUsesProtocolDisconnectEvent() {
        LoopbackBodyTransport transport = new LoopbackBodyTransport();
        transport.send(command(
                transport,
                BodyActionRequest.dance(),
                "dance-1"
        ), 0L);

        BodyEvent stopped = transport.disconnect();

        assertEquals(BodyEvent.Type.STOPPED, stopped.getType());
        assertEquals("ble_disconnected", stopped.getReason());
        assertFalse(transport.isActive());
    }

    @Test
    public void invalidEventPayloadStopsActiveCommand() {
        BodyProtocolCodec codec = new BodyProtocolCodec();
        CorruptingLink link = new CorruptingLink();
        LoopbackBodyTransport transport = new LoopbackBodyTransport(codec, link);
        transport.send(command(
                transport,
                BodyActionRequest.moveSteps(2),
                "move-corrupt"
        ), 0L);
        link.corruptTicks = true;

        BodyEvent stopped = transport.tick(100L);

        assertEquals(BodyEvent.Type.STOPPED, stopped.getType());
        assertEquals("invalid_telemetry", stopped.getReason());
        assertFalse(transport.isActive());
    }

    @Test
    public void activeTransportUsesUniqueHeartbeatCommandIds() throws Exception {
        BodyProtocolCodec codec = new BodyProtocolCodec();
        RecordingLink link = new RecordingLink(codec);
        LoopbackBodyTransport transport = new LoopbackBodyTransport(codec, link);
        transport.send(command(
                transport,
                BodyActionRequest.moveSteps(2),
                "move-heartbeats"
        ), 0L);

        assertNull(transport.tick(400L));
        assertNull(transport.tick(800L));

        assertEquals(2, link.heartbeatIds.size());
        assertFalse(link.heartbeatIds.get(0).equals(link.heartbeatIds.get(1)));
    }

    private BodyCommand command(
            LoopbackBodyTransport transport,
            BodyActionRequest request,
            String commandId
    ) {
        return policy.authorize(
                request,
                transport.getCapabilities(),
                commandId
        ).getCommand();
    }

    private static final class CorruptingLink implements BodyWireLink {
        private final LoopbackBodyPeer peer = new LoopbackBodyPeer();
        private boolean corruptTicks;

        @Override
        public byte[] write(byte[] commandPayload, long nowMs) {
            return peer.write(commandPayload, nowMs);
        }

        @Override
        public byte[] tick(long nowMs) {
            if (corruptTicks) {
                return new byte[]{(byte) 0xff};
            }
            return peer.tick(nowMs);
        }

        @Override
        public byte[] disconnect() {
            return peer.disconnect();
        }
    }

    private static final class RecordingLink implements BodyWireLink {
        private final BodyProtocolCodec codec;
        private final LoopbackBodyPeer peer = new LoopbackBodyPeer();
        private final List<String> heartbeatIds = new ArrayList<>();

        private RecordingLink(BodyProtocolCodec codec) {
            this.codec = codec;
        }

        @Override
        public byte[] write(byte[] commandPayload, long nowMs) {
            try {
                BodyProtocolCommand command = codec.decodeCommand(commandPayload);
                if (command.getType() == BodyProtocolCommand.Type.HEARTBEAT) {
                    heartbeatIds.add(command.getCommandId());
                }
            } catch (BodyProtocolException error) {
                throw new AssertionError(error);
            }
            return peer.write(commandPayload, nowMs);
        }

        @Override
        public byte[] tick(long nowMs) {
            return peer.tick(nowMs);
        }

        @Override
        public byte[] disconnect() {
            return peer.disconnect();
        }
    }
}
