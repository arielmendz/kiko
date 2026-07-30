import unittest

from kiko_body.controller import BodyController
from kiko_body.messages import Command, CommandType
from kiko_body.motion import SimulatedMotionDriver


def command(
    command_id: str,
    command_type: CommandType,
    arguments: dict[str, object],
    timeout_ms: int = 5000,
) -> Command:
    return Command(command_id, command_type, arguments, timeout_ms)


class BodyControllerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.driver = SimulatedMotionDriver()
        self.controller = BodyController(self.driver)

    def test_reports_two_servo_capabilities(self) -> None:
        events = self.controller.handle(
            command("caps-1", CommandType.GET_CAPABILITIES, {}),
            now_ms=0,
        )

        self.assertEqual("CAPABILITIES", events[0].event_type)
        self.assertEqual(2, events[0].data["servoCount"])
        self.assertEqual(6, events[0].data["maxStepsPerCommand"])

    def test_rejects_step_count_above_native_limit(self) -> None:
        events = self.controller.handle(
            command("move-1", CommandType.MOVE_STEPS, {"count": 7}),
            now_ms=0,
        )

        self.assertEqual("REJECTED", events[0].event_type)
        self.assertEqual("count_out_of_range", events[0].data["reason"])
        self.assertIsNone(self.driver.active_plan)

    def test_duplicate_command_does_not_start_motion_twice(self) -> None:
        request = command(
            "move-2",
            CommandType.MOVE_STEPS,
            {"count": 2},
        )

        first = self.controller.handle(request, now_ms=0)
        second = self.controller.handle(request, now_ms=10)

        self.assertEqual(first, second)
        self.assertEqual("steps:2", self.driver.active_plan.name)

    def test_stop_interrupts_active_motion(self) -> None:
        self.controller.handle(
            command("move-3", CommandType.MOVE_STEPS, {"count": 2}),
            now_ms=0,
        )

        events = self.controller.handle(
            command("stop-1", CommandType.STOP, {}, timeout_ms=100),
            now_ms=100,
        )

        self.assertEqual("STOPPED", events[0].event_type)
        self.assertEqual("move-3", events[0].data["stoppedCommandId"])
        self.assertIsNone(self.driver.active_plan)

    def test_watchdog_stops_motion_without_heartbeat(self) -> None:
        self.controller.handle(
            command("move-4", CommandType.MOVE_STEPS, {"count": 2}),
            now_ms=0,
        )

        events = self.controller.tick(now_ms=751)

        self.assertEqual("STOPPED", events[0].event_type)
        self.assertEqual("link_watchdog", events[0].data["reason"])
        self.assertIsNone(self.driver.active_plan)

    def test_ble_disconnect_stops_motion(self) -> None:
        self.controller.handle(
            command("move-disconnect", CommandType.MOVE_STEPS, {"count": 2}),
            now_ms=0,
        )

        events = self.controller.disconnected()

        self.assertEqual("STOPPED", events[0].event_type)
        self.assertEqual("ble_disconnected", events[0].data["reason"])
        self.assertIsNone(self.driver.active_plan)

    def test_heartbeat_keeps_motion_alive_until_completion(self) -> None:
        self.controller.handle(
            command("move-5", CommandType.MOVE_STEPS, {"count": 1}),
            now_ms=0,
        )
        self.controller.handle(
            command("heartbeat-1", CommandType.HEARTBEAT, {}),
            now_ms=500,
        )

        events = self.controller.tick(now_ms=1000)

        self.assertEqual("COMPLETED", events[0].event_type)
        self.assertIsNone(self.driver.active_plan)

    def test_deadline_stops_motion_even_with_heartbeat(self) -> None:
        self.controller.handle(
            command(
                "move-deadline",
                CommandType.MOVE_STEPS,
                {"count": 2},
                timeout_ms=2500,
            ),
            now_ms=0,
        )
        self.driver.finish_at_ms = 10_000
        self.controller.handle(
            command("heartbeat-deadline", CommandType.HEARTBEAT, {}),
            now_ms=2499,
        )

        events = self.controller.tick(now_ms=2500)

        self.assertEqual("STOPPED", events[0].event_type)
        self.assertEqual("deadline_expired", events[0].data["reason"])
        self.assertIsNone(self.driver.active_plan)

    def test_short_deadline_is_rejected_before_motion(self) -> None:
        events = self.controller.handle(
            command(
                "move-6",
                CommandType.MOVE_STEPS,
                {"count": 2},
                timeout_ms=1000,
            ),
            now_ms=0,
        )

        self.assertEqual("REJECTED", events[0].event_type)
        self.assertEqual("timeout_too_short", events[0].data["reason"])
        self.assertIsNone(self.driver.active_plan)


if __name__ == "__main__":
    unittest.main()
