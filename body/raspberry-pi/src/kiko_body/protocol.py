from __future__ import annotations

import json
import re
from typing import Any

from .messages import Command, CommandType, Event, PROTOCOL_VERSION


MAX_MESSAGE_BYTES = 512
MIN_TIMEOUT_MS = 100
MAX_TIMEOUT_MS = 10_000
_COMMAND_ID = re.compile(r"^[A-Za-z0-9._:-]{1,64}$")
_COMMAND_FIELDS = {
    "protocolVersion",
    "commandId",
    "type",
    "arguments",
    "timeoutMs",
}


class ProtocolError(ValueError):
    """Raised when an untrusted wire message violates the v1 protocol."""


def decode_command(payload: bytes) -> Command:
    if len(payload) > MAX_MESSAGE_BYTES:
        raise ProtocolError("message_too_large")

    try:
        value = json.loads(payload.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ProtocolError("invalid_json") from error

    if not isinstance(value, dict):
        raise ProtocolError("command_must_be_an_object")
    if set(value) != _COMMAND_FIELDS:
        raise ProtocolError("unexpected_or_missing_fields")
    if (
        isinstance(value["protocolVersion"], bool)
        or not isinstance(value["protocolVersion"], int)
        or value["protocolVersion"] != PROTOCOL_VERSION
    ):
        raise ProtocolError("unsupported_protocol_version")

    command_id = value["commandId"]
    if not isinstance(command_id, str) or not _COMMAND_ID.fullmatch(command_id):
        raise ProtocolError("invalid_command_id")

    try:
        command_type = CommandType(value["type"])
    except (TypeError, ValueError) as error:
        raise ProtocolError("unknown_command_type") from error

    timeout_ms = value["timeoutMs"]
    if (
        isinstance(timeout_ms, bool)
        or not isinstance(timeout_ms, int)
        or not MIN_TIMEOUT_MS <= timeout_ms <= MAX_TIMEOUT_MS
    ):
        raise ProtocolError("invalid_timeout")

    arguments = value["arguments"]
    if not isinstance(arguments, dict):
        raise ProtocolError("arguments_must_be_an_object")
    _validate_arguments(command_type, arguments)

    return Command(command_id, command_type, arguments, timeout_ms)


def encode_event(event: Event) -> bytes:
    return json.dumps(
        event.as_dict(),
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")


def _validate_arguments(
    command_type: CommandType,
    arguments: dict[str, Any],
) -> None:
    if command_type is CommandType.MOVE_STEPS:
        if set(arguments) != {"count"}:
            raise ProtocolError("move_steps_requires_count")
        count = arguments["count"]
        if isinstance(count, bool) or not isinstance(count, int):
            raise ProtocolError("count_must_be_an_integer")
        return

    if command_type is CommandType.DANCE:
        if set(arguments) != {"routineId"}:
            raise ProtocolError("dance_requires_routine_id")
        routine_id = arguments["routineId"]
        if not isinstance(routine_id, str) or not _COMMAND_ID.fullmatch(routine_id):
            raise ProtocolError("invalid_routine_id")
        return

    if arguments:
        raise ProtocolError("command_does_not_accept_arguments")
