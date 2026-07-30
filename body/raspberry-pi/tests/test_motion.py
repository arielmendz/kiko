import unittest

from kiko_body.motion import (
    MAX_ANGLE_DEGREES,
    MIN_ANGLE_DEGREES,
    MotionPlanner,
)


class MotionPlannerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.planner = MotionPlanner()

    def test_step_count_repeats_one_bounded_seal_stride(self) -> None:
        plan = self.planner.steps(3)

        self.assertEqual(12, len(plan.keyframes))
        self.assertEqual(3000, plan.duration_ms)
        for frame in plan.keyframes:
            self.assertGreaterEqual(frame.left_degrees, MIN_ANGLE_DEGREES)
            self.assertLessEqual(frame.left_degrees, MAX_ANGLE_DEGREES)
            self.assertGreaterEqual(frame.right_degrees, MIN_ANGLE_DEGREES)
            self.assertLessEqual(frame.right_degrees, MAX_ANGLE_DEGREES)

    def test_only_allowlisted_dance_exists(self) -> None:
        with self.assertRaisesRegex(ValueError, "unknown_routine"):
            self.planner.dance("model_generated")


if __name__ == "__main__":
    unittest.main()
