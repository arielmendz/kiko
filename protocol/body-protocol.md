# Kiko BLE body protocol

## Status

Version 1 is a bootstrap contract. The Python parser/safety controller and the
Android strict codec/protocol loopback exist; Android BLE central, Raspberry Pi
BlueZ peripheral, and GPIO adapters do not.

The Android app is the BLE central. The Raspberry Pi body is the BLE peripheral.
The Pi advertises only while unpaired or explicitly placed into a future physical
pairing mode. Motion requires an encrypted, bonded connection.

## GATT service

| Element | UUID | Properties |
| --- | --- | --- |
| Kiko Body service | `7f510000-2b7d-4f75-9d18-5f25b33c1000` | Primary service |
| Command characteristic | `7f510001-2b7d-4f75-9d18-5f25b33c1000` | Write with response |
| Event characteristic | `7f510002-2b7d-4f75-9d18-5f25b33c1000` | Indicate |

Android must negotiate an ATT MTU large enough for each complete v1 message
before enabling body controls. V1 messages are UTF-8 JSON objects no larger than
512 bytes; it does not define application-level fragmentation. A device that
cannot carry the complete message is incompatible with v1 rather than silently
truncating it.

Commands use write-with-response for link-level delivery. Outcomes arrive as
indications so the sender acknowledges them. Link delivery is not action
completion: Android reports success only after `COMPLETED`.

## Command envelope

Every command contains exactly:

```json
{
  "protocolVersion": 1,
  "commandId": "01J...",
  "type": "MOVE_STEPS",
  "arguments": {"count": 3},
  "timeoutMs": 5000
}
```

- `commandId` is unique within the last 128 commands. Retrying the same ID
  returns its cached outcome and never repeats motion.
- `timeoutMs` is relative to body receipt and is constrained to `100..10000`.
- A motion timeout must be greater than the native plan's estimated duration.
- Unknown fields, commands, protocol versions, and argument shapes are rejected.
- The Android model never supplies raw servo angles, speed, PWM, or GPIO values.

Commands:

| Type | Arguments | Meaning |
| --- | --- | --- |
| `GET_CAPABILITIES` | `{}` | Read body limits, routines, and watchdog |
| `MOVE_STEPS` | `{"count": 1..maxStepsPerCommand}` | Run the native seal stride exactly `count` times |
| `DANCE` | `{"routineId":"seal_wiggle"}` | Run an allowlisted native routine |
| `HEARTBEAT` | `{}` | Keep an active motion session alive |
| `STOP` | `{}` | Bypass normal work and stop immediately |

During motion Android sends a heartbeat more frequently than the advertised
`linkWatchdogMs`. The Pi stops locally on missed heartbeat, BLE disconnect,
deadline expiry, invalid control state, or explicit `STOP`.

## Events

Every event contains `protocolVersion`, the related `commandId`, a `type`, and
structured `data`.

| Type | Meaning |
| --- | --- |
| `CAPABILITIES` | Version, two-servo body limits, routines, and watchdog |
| `ALIVE` | Heartbeat accepted and whether motion is active |
| `ACCEPTED` | Motion started; this is not completion |
| `COMPLETED` | Native body reports that the requested routine finished |
| `STOPPED` | Motion was interrupted with a machine-readable reason |
| `REJECTED` | Command was invalid, unsafe, unsupported, or impossible |

The schemas in `schemas/` describe the wire envelope. The Python parser is
intentionally stricter about command-specific argument fields. The Android codec
applies the same command-specific checks and also validates every event shape.
Shared fixtures in `fixtures/` cover every command and event type and are consumed
by both JVM and Python tests.

## BLE loss and reconnection

BLE reconnection never resumes a previous action. The Pi returns both servos to
the hardware-calibrated safe neutral position when the link watchdog or
disconnect fires. Android reconnects, reads capabilities again, and starts a new
command with a new ID.

Pairing, bond replacement, and owner reset need a physical control on the body;
voice, face recognition, and model output must not authorize them.
