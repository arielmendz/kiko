package com.kiko.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LoopbackBodyPeerTest {
    private final BodyProtocolCodec codec = new BodyProtocolCodec();

    @Test
    public void negotiatesCapabilitiesThroughWirePayloads() throws Exception {
        LoopbackBodyPeer peer = new LoopbackBodyPeer();

        BodyProtocolEvent event = write(
                peer,
                BodyProtocolCommand.capabilities("caps-1"),
                0L
        );

        assertEquals(BodyProtocolEvent.Type.CAPABILITIES, event.getType());
        assertEquals(6, event.getCapabilities().getMaxStepsPerCommand());
        assertEquals(750, event.getCapabilities().getLinkWatchdogMs());
        assertEquals(2, event.getCapabilities().getServoCount());
    }

    @Test
    public void heartbeatKeepsMotionAliveUntilWireCompletion() throws Exception {
        LoopbackBodyPeer peer = new LoopbackBodyPeer();
        BodyProtocolCommand move = BodyProtocolCommand.decoded(
                "move-1",
                BodyProtocolCommand.Type.MOVE_STEPS,
                1,
                null,
                5_000
        );

        BodyProtocolEvent accepted = write(peer, move, 0L);
        BodyProtocolEvent alive = write(
                peer,
                BodyProtocolCommand.heartbeat("hb-1"),
                500L
        );
        BodyProtocolEvent completed = codec.decodeEvent(peer.tick(1_000L));

        assertEquals(BodyProtocolEvent.Type.ACCEPTED, accepted.getType());
        assertEquals(BodyProtocolEvent.Type.ALIVE, alive.getType());
        assertTrue(alive.isMoving());
        assertEquals(BodyProtocolEvent.Type.COMPLETED, completed.getType());
        assertFalse(peer.isActive());
    }

    @Test
    public void missedHeartbeatStopsAtAdvertisedWatchdog() throws Exception {
        LoopbackBodyPeer peer = new LoopbackBodyPeer();
        write(peer, BodyProtocolCommand.decoded(
                "move-watchdog",
                BodyProtocolCommand.Type.MOVE_STEPS,
                2,
                null,
                5_000
        ), 0L);

        assertNull(peer.tick(750L));
        BodyProtocolEvent stopped = codec.decodeEvent(peer.tick(751L));

        assertEquals(BodyProtocolEvent.Type.STOPPED, stopped.getType());
        assertEquals("link_watchdog", stopped.getReason());
        assertFalse(peer.isActive());
    }

    @Test
    public void duplicateIdReturnsCachedWireOutcomeWithoutRestart() throws Exception {
        LoopbackBodyPeer peer = new LoopbackBodyPeer();
        BodyProtocolCommand move = BodyProtocolCommand.decoded(
                "move-duplicate",
                BodyProtocolCommand.Type.MOVE_STEPS,
                1,
                null,
                5_000
        );
        byte[] payload = codec.encodeCommand(move);

        byte[] first = peer.write(payload, 0L);
        byte[] duplicate = peer.write(payload, 500L);

        assertArrayEquals(first, duplicate);
        assertTrue(peer.isActive());
    }

    @Test
    public void stopAndDisconnectReportInterruptedCommand() throws Exception {
        LoopbackBodyPeer peer = new LoopbackBodyPeer();
        write(peer, BodyProtocolCommand.decoded(
                "move-stop",
                BodyProtocolCommand.Type.MOVE_STEPS,
                2,
                null,
                5_000
        ), 0L);

        BodyProtocolEvent stopped = write(
                peer,
                BodyProtocolCommand.decoded(
                        "stop-1",
                        BodyProtocolCommand.Type.STOP,
                        null,
                        null,
                        100
                ),
                100L
        );

        assertEquals("requested", stopped.getReason());
        assertEquals("move-stop", stopped.getStoppedCommandId());
        assertFalse(peer.isActive());

        write(peer, BodyProtocolCommand.decoded(
                "move-disconnect",
                BodyProtocolCommand.Type.MOVE_STEPS,
                2,
                null,
                5_000
        ), 200L);
        BodyProtocolEvent disconnected = codec.decodeEvent(peer.disconnect());

        assertEquals("ble_disconnected", disconnected.getReason());
        assertEquals("move-disconnect", disconnected.getStoppedCommandId());
    }

    @Test
    public void busyAndDeadlineFailuresMatchPiReasons() throws Exception {
        LoopbackBodyPeer peer = new LoopbackBodyPeer();
        BodyProtocolCommand dance = BodyProtocolCommand.decoded(
                "dance-deadline",
                BodyProtocolCommand.Type.DANCE,
                null,
                "seal_wiggle",
                2_500
        );
        write(peer, dance, 0L);

        BodyProtocolEvent busy = write(peer, BodyProtocolCommand.decoded(
                "move-busy",
                BodyProtocolCommand.Type.MOVE_STEPS,
                1,
                null,
                5_000
        ), 100L);
        assertEquals(BodyProtocolEvent.Type.REJECTED, busy.getType());
        assertEquals("body_busy", busy.getReason());

        for (int nowMs = 500; nowMs < 2_500; nowMs += 500) {
            write(
                    peer,
                    BodyProtocolCommand.heartbeat("deadline-hb-" + nowMs),
                    nowMs
            );
        }
        BodyProtocolEvent deadline = codec.decodeEvent(peer.tick(2_500L));

        assertEquals(BodyProtocolEvent.Type.STOPPED, deadline.getType());
        assertEquals("deadline_expired", deadline.getReason());
        assertFalse(peer.isActive());
    }

    @Test
    public void malformedWireCommandReturnsProtocolRejection() throws Exception {
        LoopbackBodyPeer peer = new LoopbackBodyPeer();

        BodyProtocolEvent rejected = codec.decodeEvent(peer.write(
                new byte[]{(byte) 0xff},
                0L
        ));

        assertEquals(BodyProtocolEvent.Type.REJECTED, rejected.getType());
        assertEquals("invalid", rejected.getCommandId());
        assertEquals("invalid_json", rejected.getReason());
        assertFalse(peer.isActive());
    }

    private BodyProtocolEvent write(
            LoopbackBodyPeer peer,
            BodyProtocolCommand command,
            long nowMs
    ) throws Exception {
        return codec.decodeEvent(peer.write(codec.encodeCommand(command), nowMs));
    }
}
