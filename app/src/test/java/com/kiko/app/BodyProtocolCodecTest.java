package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public final class BodyProtocolCodecTest {
    private final BodyProtocolCodec codec = new BodyProtocolCodec();

    @Test
    public void roundTripsEveryCommandShape() throws Exception {
        BodyProtocolCommand move = BodyProtocolCommand.decoded(
                "move-1",
                BodyProtocolCommand.Type.MOVE_STEPS,
                3,
                null,
                5_000
        );
        BodyProtocolCommand dance = BodyProtocolCommand.decoded(
                "dance-1",
                BodyProtocolCommand.Type.DANCE,
                null,
                "seal_wiggle",
                5_000
        );

        BodyProtocolCommand decodedMove = codec.decodeCommand(
                codec.encodeCommand(move)
        );
        BodyProtocolCommand decodedDance = codec.decodeCommand(
                codec.encodeCommand(dance)
        );

        assertEquals(BodyProtocolCommand.Type.MOVE_STEPS, decodedMove.getType());
        assertEquals(Integer.valueOf(3), decodedMove.getStepCount());
        assertEquals(BodyProtocolCommand.Type.DANCE, decodedDance.getType());
        assertEquals("seal_wiggle", decodedDance.getRoutineId());
    }

    @Test
    public void rejectsUnknownFieldsFractionalCountsAndOversizePayloads() {
        String unknownField = "{\"protocolVersion\":1,\"commandId\":\"x\","
                + "\"type\":\"STOP\",\"arguments\":{},\"timeoutMs\":100,"
                + "\"surprise\":true}";
        String fractionalCount = "{\"protocolVersion\":1,"
                + "\"commandId\":\"x\",\"type\":\"MOVE_STEPS\","
                + "\"arguments\":{\"count\":1.0},\"timeoutMs\":5000}";
        byte[] oversize = new byte[BodyProtocolCodec.MAX_MESSAGE_BYTES + 1];

        BodyProtocolException unknown = assertThrows(
                BodyProtocolException.class,
                () -> codec.decodeCommand(bytes(unknownField))
        );
        BodyProtocolException fractional = assertThrows(
                BodyProtocolException.class,
                () -> codec.decodeCommand(bytes(fractionalCount))
        );
        BodyProtocolException tooLarge = assertThrows(
                BodyProtocolException.class,
                () -> codec.decodeCommand(oversize)
        );

        assertEquals("unexpected_or_missing_fields", unknown.getMessage());
        assertEquals("count_must_be_an_integer", fractional.getMessage());
        assertEquals("message_too_large", tooLarge.getMessage());
    }

    @Test
    public void rejectsInvalidUtf8AndWrongProtocolVersion() {
        byte[] invalidUtf8 = {(byte) 0xc3, (byte) 0x28};
        String wrongVersion = "{\"protocolVersion\":2,"
                + "\"commandId\":\"x\",\"type\":\"STOP\","
                + "\"arguments\":{},\"timeoutMs\":100}";

        BodyProtocolException invalid = assertThrows(
                BodyProtocolException.class,
                () -> codec.decodeCommand(invalidUtf8)
        );
        BodyProtocolException unsupported = assertThrows(
                BodyProtocolException.class,
                () -> codec.decodeCommand(bytes(wrongVersion))
        );

        assertEquals("invalid_json", invalid.getMessage());
        assertEquals("unsupported_protocol_version", unsupported.getMessage());
    }

    @Test
    public void compactEncodingStaysWithinSingleV1Message() throws Exception {
        byte[] encoded = codec.encodeEvent(BodyProtocolEvent.capabilities(
                "caps-1",
                BodyCapabilities.loopback()
        ));

        assertTrue(encoded.length <= BodyProtocolCodec.MAX_MESSAGE_BYTES);
        assertTrue(!new String(encoded, StandardCharsets.UTF_8).contains("\n"));
    }

    @Test
    public void stoppedEventAllowsNoActiveCommand() throws Exception {
        BodyProtocolEvent stopped = codec.decodeEvent(codec.encodeEvent(
                BodyProtocolEvent.stopped("stop-idle", "requested", null)
        ));

        assertEquals(BodyProtocolEvent.Type.STOPPED, stopped.getType());
        assertNull(stopped.getStoppedCommandId());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
