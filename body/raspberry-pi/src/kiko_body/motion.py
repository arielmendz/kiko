from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


MIN_ANGLE_DEGREES = -35
MAX_ANGLE_DEGREES = 35
NEUTRAL_ANGLE_DEGREES = 0


@dataclass(frozen=True)
class Keyframe:
    duration_ms: int
    left_degrees: int
    right_degrees: int


@dataclass(frozen=True)
class MotionPlan:
    name: str
    keyframes: tuple[Keyframe, ...]

    @property
    def duration_ms(self) -> int:
        return sum(frame.duration_ms for frame in self.keyframes)


class MotionDriver(Protocol):
    def start(self, plan: MotionPlan, now_ms: int) -> None: ...

    def stop(self) -> None: ...

    def is_complete(self, now_ms: int) -> bool: ...

    def finish(self) -> None: ...


class MotionPlanner:
    """Produces only body-owned, bounded two-servo trajectories."""

    ROUTINE_IDS = ("seal_wiggle",)

    def steps(self, count: int) -> MotionPlan:
        stride = (
            Keyframe(250, 35, -15),
            Keyframe(250, NEUTRAL_ANGLE_DEGREES, NEUTRAL_ANGLE_DEGREES),
            Keyframe(250, -15, 35),
            Keyframe(250, NEUTRAL_ANGLE_DEGREES, NEUTRAL_ANGLE_DEGREES),
        )
        return self._validated_plan(f"steps:{count}", stride * count)

    def dance(self, routine_id: str) -> MotionPlan:
        if routine_id not in self.ROUTINE_IDS:
            raise ValueError("unknown_routine")
        frames = (
            Keyframe(300, 30, 30),
            Keyframe(300, -30, -30),
            Keyframe(300, 30, -30),
            Keyframe(300, -30, 30),
            Keyframe(300, 35, 10),
            Keyframe(300, 10, 35),
            Keyframe(300, -25, -25),
            Keyframe(300, NEUTRAL_ANGLE_DEGREES, NEUTRAL_ANGLE_DEGREES),
        )
        return self._validated_plan(f"dance:{routine_id}", frames)

    @staticmethod
    def _validated_plan(
        name: str,
        frames: tuple[Keyframe, ...],
    ) -> MotionPlan:
        if not frames:
            raise ValueError("empty_motion_plan")
        for frame in frames:
            if frame.duration_ms <= 0:
                raise ValueError("invalid_frame_duration")
            if not MIN_ANGLE_DEGREES <= frame.left_degrees <= MAX_ANGLE_DEGREES:
                raise ValueError("left_angle_out_of_range")
            if not MIN_ANGLE_DEGREES <= frame.right_degrees <= MAX_ANGLE_DEGREES:
                raise ValueError("right_angle_out_of_range")
        return MotionPlan(name, frames)


class SimulatedMotionDriver:
    """Clock-driven test double; it never touches GPIO."""

    def __init__(self) -> None:
        self.active_plan: MotionPlan | None = None
        self.finish_at_ms: int | None = None
        self.stop_count = 0

    def start(self, plan: MotionPlan, now_ms: int) -> None:
        if self.active_plan is not None:
            raise RuntimeError("motion_already_active")
        self.active_plan = plan
        self.finish_at_ms = now_ms + plan.duration_ms

    def stop(self) -> None:
        self.active_plan = None
        self.finish_at_ms = None
        self.stop_count += 1

    def is_complete(self, now_ms: int) -> bool:
        return self.finish_at_ms is not None and now_ms >= self.finish_at_ms

    def finish(self) -> None:
        self.active_plan = None
        self.finish_at_ms = None
