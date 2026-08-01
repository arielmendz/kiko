package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class BodyProtocolFixtureTest {
    private final BodyProtocolCodec codec = new BodyProtocolCodec();

    @Test
    public void decodesSharedMoveAndCapabilitiesFixtures() throws Exception {
        BodyProtocolCommand move = codec.decodeCommand(
                fixture("move-three-steps.command.json")
        );
        BodyProtocolEvent capabilities = codec.decodeEvent(
                fixture("capabilities.event.json")
        );

        assertEquals(BodyProtocolCommand.Type.MOVE_STEPS, move.getType());
        assertEquals(Integer.valueOf(3), move.getStepCount());
        assertEquals(BodyProtocolEvent.Type.CAPABILITIES, capabilities.getType());
        assertEquals(
                6,
                capabilities.getCapabilities().getMaxStepsPerCommand()
        );
        assertEquals(750, capabilities.getCapabilities().getLinkWatchdogMs());
        assertEquals(2, capabilities.getCapabilities().getServoCount());
        assertTrue(capabilities.getCapabilities().supportsRoutine("seal_wiggle"));
    }

    @Test
    public void decodesSharedHeartbeatAndActionEvents() throws Exception {
        BodyProtocolCommand heartbeat = codec.decodeCommand(
                fixture("heartbeat.command.json")
        );
        BodyProtocolEvent alive = codec.decodeEvent(
                fixture("alive.event.json")
        );
        BodyProtocolEvent accepted = codec.decodeEvent(
                fixture("move-three-steps.accepted.event.json")
        );
        BodyProtocolEvent completed = codec.decodeEvent(
                fixture("move-three-steps.completed.event.json")
        );

        assertEquals(BodyProtocolCommand.Type.HEARTBEAT, heartbeat.getType());
        assertEquals(BodyProtocolEvent.Type.ALIVE, alive.getType());
        assertTrue(alive.isMoving());
        assertEquals(BodyProtocolEvent.Type.ACCEPTED, accepted.getType());
        assertEquals(Long.valueOf(3_000L), accepted.getEstimatedDurationMs());
        assertEquals(BodyProtocolEvent.Type.COMPLETED, completed.getType());
    }

    @Test
    public void decodesEveryRemainingCommandAndOutcomeFixture() throws Exception {
        BodyProtocolCommand dance = codec.decodeCommand(
                fixture("dance.command.json")
        );
        BodyProtocolCommand stop = codec.decodeCommand(
                fixture("stop.command.json")
        );
        BodyProtocolEvent stopped = codec.decodeEvent(
                fixture("stop.event.json")
        );
        BodyProtocolEvent rejected = codec.decodeEvent(
                fixture("move-seven-steps.rejected.event.json")
        );

        assertEquals(BodyProtocolCommand.Type.DANCE, dance.getType());
        assertEquals("seal_wiggle", dance.getRoutineId());
        assertEquals(BodyProtocolCommand.Type.STOP, stop.getType());
        assertEquals(BodyProtocolEvent.Type.STOPPED, stopped.getType());
        assertEquals("fixture-move-3", stopped.getStoppedCommandId());
        assertEquals(BodyProtocolEvent.Type.REJECTED, rejected.getType());
        assertEquals("count_out_of_range", rejected.getReason());
    }

    private static byte[] fixture(String name) throws Exception {
        InputStream stream = BodyProtocolFixtureTest.class
                .getClassLoader()
                .getResourceAsStream(name);
        assertNotNull("Missing shared protocol fixture " + name, stream);
        try (InputStream input = stream;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }
}
