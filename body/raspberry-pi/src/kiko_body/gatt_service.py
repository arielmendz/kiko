from __future__ import annotations

from .controller import BodyController
from .messages import Event
from .protocol import ProtocolError, decode_command, encode_event


BODY_SERVICE_UUID = "7f510000-2b7d-4f75-9d18-5f25b33c1000"
COMMAND_CHARACTERISTIC_UUID = "7f510001-2b7d-4f75-9d18-5f25b33c1000"
EVENT_CHARACTERISTIC_UUID = "7f510002-2b7d-4f75-9d18-5f25b33c1000"


class GattBodyService:
    """Pure command/event boundary for a future BlueZ GATT peripheral adapter."""

    def __init__(self, controller: BodyController) -> None:
        self._controller = controller

    def command_written(self, payload: bytes, now_ms: int) -> tuple[bytes, ...]:
        try:
            command = decode_command(payload)
            events = self._controller.handle(command, now_ms)
        except ProtocolError as error:
            events = (
                Event("invalid", "REJECTED", {"reason": str(error)}),
            )
        return tuple(encode_event(event) for event in events)

    def tick(self, now_ms: int) -> tuple[bytes, ...]:
        return tuple(
            encode_event(event) for event in self._controller.tick(now_ms)
        )

    def disconnected(self) -> tuple[bytes, ...]:
        return tuple(
            encode_event(event) for event in self._controller.disconnected()
        )
