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

    def test_all_command_fixtures_are_accepted_by_strict_parser(self) -> None:
        expected_types = {
            "dance.command.json": "DANCE",
            "heartbeat.command.json": "HEARTBEAT",
            "stop.command.json": "STOP",
            "move-seven-steps.command.json": "MOVE_STEPS",
        }

        for filename, expected_type in expected_types.items():
            with self.subTest(filename=filename):
                command = decode_command((FIXTURES / filename).read_bytes())
                self.assertEqual(expected_type, command.command_type.value)

    def test_action_event_fixtures_match_body_controller(self) -> None:
        controller = BodyController(SimulatedMotionDriver())
        move = decode_command(
            (FIXTURES / "move-three-steps.command.json").read_bytes()
        )
        heartbeat = decode_command(
            (FIXTURES / "heartbeat.command.json").read_bytes()
        )

        accepted = controller.handle(move, now_ms=0)[0].as_dict()
        alive = controller.handle(heartbeat, now_ms=500)[0].as_dict()
        controller.handle(heartbeat, now_ms=1000)
        controller.handle(heartbeat, now_ms=1500)
        controller.handle(heartbeat, now_ms=2000)
        controller.handle(heartbeat, now_ms=2500)
        completed = controller.tick(now_ms=3000)[0].as_dict()

        self.assertEqual(
            json.loads(
                (FIXTURES / "move-three-steps.accepted.event.json").read_text()
            ),
            accepted,
        )
        self.assertEqual(
            json.loads((FIXTURES / "alive.event.json").read_text()),
            alive,
        )
        self.assertEqual(
            json.loads(
                (FIXTURES / "move-three-steps.completed.event.json").read_text()
            ),
            completed,
        )

    def test_stop_and_rejection_fixtures_match_body_controller(self) -> None:
        controller = BodyController(SimulatedMotionDriver())
        move = decode_command(
            (FIXTURES / "move-three-steps.command.json").read_bytes()
        )
        stop = decode_command((FIXTURES / "stop.command.json").read_bytes())
        controller.handle(move, now_ms=0)

        stopped = controller.handle(stop, now_ms=100)[0].as_dict()
        rejected = controller.handle(
            decode_command(
                (FIXTURES / "move-seven-steps.command.json").read_bytes()
            ),
            now_ms=200,
        )[0].as_dict()

        self.assertEqual(
            json.loads((FIXTURES / "stop.event.json").read_text()),
            stopped,
        )
        self.assertEqual(
            json.loads(
                (FIXTURES / "move-seven-steps.rejected.event.json").read_text()
            ),
            rejected,
        )


if __name__ == "__main__":
    unittest.main()
