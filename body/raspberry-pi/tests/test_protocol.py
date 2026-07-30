import json
import unittest

from kiko_body.controller import BodyController
from kiko_body.gatt_service import GattBodyService
from kiko_body.messages import CommandType, Event
from kiko_body.motion import SimulatedMotionDriver
from kiko_body.protocol import ProtocolError, decode_command, encode_event


def command_payload(**overrides: object) -> bytes:
    value = {
        "protocolVersion": 1,
        "commandId": "test-1",
        "type": "GET_CAPABILITIES",
        "arguments": {},
        "timeoutMs": 1000,
    }
    value.update(overrides)
    return json.dumps(value).encode()


class ProtocolTest(unittest.TestCase):
    def test_decodes_exact_command_schema(self) -> None:
        command = decode_command(
            command_payload(
                type="MOVE_STEPS",
                arguments={"count": 3},
            )
        )

        self.assertEqual(CommandType.MOVE_STEPS, command.command_type)
        self.assertEqual(3, command.arguments["count"])

    def test_rejects_unknown_fields(self) -> None:
        with self.assertRaisesRegex(
            ProtocolError,
            "unexpected_or_missing_fields",
        ):
            decode_command(command_payload(surprise=True))

    def test_rejects_boolean_as_step_count(self) -> None:
        with self.assertRaisesRegex(ProtocolError, "count_must_be_an_integer"):
            decode_command(
                command_payload(
                    type="MOVE_STEPS",
                    arguments={"count": True},
                )
            )

    def test_encodes_compact_utf8_event(self) -> None:
        encoded = encode_event(Event("cmd-1", "STOPPED", {"reason": "solicitado"}))

        self.assertNotIn(b" ", encoded)
        self.assertEqual(
            "solicitado",
            json.loads(encoded)["data"]["reason"],
        )

    def test_gatt_boundary_rejects_invalid_payload(self) -> None:
        service = GattBodyService(BodyController(SimulatedMotionDriver()))

        events = service.command_written(b"not-json", now_ms=0)

        self.assertEqual("REJECTED", json.loads(events[0])["type"])
        self.assertEqual("invalid_json", json.loads(events[0])["data"]["reason"])


if __name__ == "__main__":
    unittest.main()
