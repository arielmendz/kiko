import json
from pathlib import Path
import unittest

from kiko_body.controller import BodyController
from kiko_body.motion import SimulatedMotionDriver
from kiko_body.protocol import decode_command


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
FIXTURES = REPOSITORY_ROOT / "protocol" / "fixtures"


class ProtocolFixtureTest(unittest.TestCase):
    def test_capability_fixture_matches_body_controller(self) -> None:
        command_payload = (FIXTURES / "get-capabilities.command.json").read_bytes()
        expected_event = json.loads(
            (FIXTURES / "capabilities.event.json").read_text(encoding="utf-8")
        )
        controller = BodyController(SimulatedMotionDriver())

        events = controller.handle(decode_command(command_payload), now_ms=0)

        self.assertEqual(expected_event, events[0].as_dict())

    def test_move_fixture_is_accepted_by_strict_parser(self) -> None:
        payload = (FIXTURES / "move-three-steps.command.json").read_bytes()

        command = decode_command(payload)

        self.assertEqual(3, command.arguments["count"])


if __name__ == "__main__":
    unittest.main()
