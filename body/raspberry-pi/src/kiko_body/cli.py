from __future__ import annotations

import sys
import time

from .controller import BodyController
from .gatt_service import GattBodyService
from .motion import SimulatedMotionDriver


def main() -> None:
    service = GattBodyService(BodyController(SimulatedMotionDriver()))
    for line in sys.stdin.buffer:
        payload = line.strip()
        if not payload:
            continue
        now_ms = time.monotonic_ns() // 1_000_000
        for event in service.command_written(payload, now_ms):
            sys.stdout.buffer.write(event + b"\n")
            sys.stdout.buffer.flush()
