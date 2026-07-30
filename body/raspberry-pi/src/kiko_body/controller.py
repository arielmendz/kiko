from __future__ import annotations

from collections import OrderedDict
from dataclasses import dataclass

from .messages import Command, CommandType, Event, PROTOCOL_VERSION
from .motion import MotionDriver, MotionPlan, MotionPlanner


@dataclass(frozen=True)
class ActiveMotion:
    command_id: str
    command_type: CommandType
    deadline_ms: int


class BodyController:
    """Transport-independent safety authority for physical movement."""

    MAX_STEPS_PER_COMMAND = 6
    LINK_WATCHDOG_MS = 750
    RESPONSE_CACHE_SIZE = 128

    def __init__(
        self,
        motion_driver: MotionDriver,
        motion_planner: MotionPlanner | None = None,
    ) -> None:
        self._driver = motion_driver
        self._planner = motion_planner or MotionPlanner()
        self._active: ActiveMotion | None = None
        self._last_contact_ms: int | None = None
        self._responses: OrderedDict[str, tuple[Event, ...]] = OrderedDict()

    def handle(self, command: Command, now_ms: int) -> tuple[Event, ...]:
        self._last_contact_ms = now_ms

        cached = self._responses.get(command.command_id)
        if cached is not None:
            self._responses.move_to_end(command.command_id)
            return cached

        if command.command_type is CommandType.GET_CAPABILITIES:
            return self._remember(
                command.command_id,
                Event(
                    command.command_id,
                    "CAPABILITIES",
                    {
                        "protocolVersion": PROTOCOL_VERSION,
                        "maxStepsPerCommand": self.MAX_STEPS_PER_COMMAND,
                        "routineIds": list(self._planner.ROUTINE_IDS),
                        "supportsStop": True,
                        "linkWatchdogMs": self.LINK_WATCHDOG_MS,
                        "servoCount": 2,
                    },
                ),
            )

        if command.command_type is CommandType.HEARTBEAT:
            return self._remember(
                command.command_id,
                Event(
                    command.command_id,
                    "ALIVE",
                    {"moving": self._active is not None},
                ),
            )

        if command.command_type is CommandType.STOP:
            stopped_command_id = (
                self._active.command_id if self._active is not None else None
            )
            self._stop_driver()
            return self._remember(
                command.command_id,
                Event(
                    command.command_id,
                    "STOPPED",
                    {
                        "reason": "requested",
                        "stoppedCommandId": stopped_command_id,
                    },
                ),
            )

        if self._active is not None:
            return self._reject(command.command_id, "body_busy")

        if command.command_type is CommandType.MOVE_STEPS:
            count = command.arguments["count"]
            if not 1 <= count <= self.MAX_STEPS_PER_COMMAND:
                return self._reject(command.command_id, "count_out_of_range")
            plan = self._planner.steps(count)
            return self._start(command, plan, now_ms)

        if command.command_type is CommandType.DANCE:
            routine_id = command.arguments["routineId"]
            try:
                plan = self._planner.dance(routine_id)
            except ValueError:
                return self._reject(command.command_id, "unknown_routine")
            return self._start(command, plan, now_ms)

        return self._reject(command.command_id, "unsupported_command")

    def tick(self, now_ms: int) -> tuple[Event, ...]:
        active = self._active
        if active is None:
            return ()

        if (
            self._last_contact_ms is None
            or now_ms - self._last_contact_ms > self.LINK_WATCHDOG_MS
        ):
            return self._stop_active("link_watchdog")

        if now_ms >= active.deadline_ms:
            return self._stop_active("deadline_expired")

        if self._driver.is_complete(now_ms):
            self._driver.finish()
            self._active = None
            return self._remember(
                active.command_id,
                Event(
                    active.command_id,
                    "COMPLETED",
                    {"action": active.command_type.value},
                ),
            )

        return ()

    def disconnected(self) -> tuple[Event, ...]:
        if self._active is None:
            return ()
        return self._stop_active("ble_disconnected")

    def _start(
        self,
        command: Command,
        plan: MotionPlan,
        now_ms: int,
    ) -> tuple[Event, ...]:
        if command.timeout_ms <= plan.duration_ms:
            return self._reject(command.command_id, "timeout_too_short")

        self._driver.start(plan, now_ms)
        self._active = ActiveMotion(
            command.command_id,
            command.command_type,
            now_ms + command.timeout_ms,
        )
        return self._remember(
            command.command_id,
            Event(
                command.command_id,
                "ACCEPTED",
                {
                    "action": command.command_type.value,
                    "estimatedDurationMs": plan.duration_ms,
                },
            ),
        )

    def _stop_active(self, reason: str) -> tuple[Event, ...]:
        active = self._active
        if active is None:
            return ()
        self._stop_driver()
        return self._remember(
            active.command_id,
            Event(
                active.command_id,
                "STOPPED",
                {"reason": reason, "stoppedCommandId": active.command_id},
            ),
        )

    def _stop_driver(self) -> None:
        self._driver.stop()
        self._active = None

    def _reject(self, command_id: str, reason: str) -> tuple[Event, ...]:
        return self._remember(
            command_id,
            Event(command_id, "REJECTED", {"reason": reason}),
        )

    def _remember(
        self,
        command_id: str,
        event: Event,
    ) -> tuple[Event, ...]:
        events = (event,)
        self._responses[command_id] = events
        self._responses.move_to_end(command_id)
        while len(self._responses) > self.RESPONSE_CACHE_SIZE:
            self._responses.popitem(last=False)
        return events
