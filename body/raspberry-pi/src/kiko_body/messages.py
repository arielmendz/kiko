from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from typing import Any


PROTOCOL_VERSION = 1


class CommandType(str, Enum):
    GET_CAPABILITIES = "GET_CAPABILITIES"
    MOVE_STEPS = "MOVE_STEPS"
    DANCE = "DANCE"
    HEARTBEAT = "HEARTBEAT"
    STOP = "STOP"


@dataclass(frozen=True)
class Command:
    command_id: str
    command_type: CommandType
    arguments: dict[str, Any]
    timeout_ms: int


@dataclass(frozen=True)
class Event:
    command_id: str
    event_type: str
    data: dict[str, Any]

    def as_dict(self) -> dict[str, Any]:
        return {
            "protocolVersion": PROTOCOL_VERSION,
            "commandId": self.command_id,
            "type": self.event_type,
            "data": self.data,
        }
