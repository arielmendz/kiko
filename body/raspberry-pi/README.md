# Raspberry Pi body service

This directory is an independently testable and deployable Python project for
Kiko's two-servo body. The current bootstrap includes:

- strict parsing for the versioned body command protocol;
- capability, step, dance, heartbeat, and stop commands;
- idempotent command IDs;
- command deadlines and a 750 ms motion watchdog;
- bounded seal-like two-servo trajectories;
- a simulated servo-pair driver;
- a transport-facing GATT service boundary; and
- deterministic unit tests.

It does **not** yet advertise a BLE service through BlueZ or drive GPIO. Those
adapters must be added only after the Raspberry Pi model, servo model, GPIO
pins, neutral angles, power supply, and mechanical range have been recorded in
`hardware/`.

## Run the simulator

Python 3.11 or newer is required. From the repository root:

```sh
PYTHONPATH=body/raspberry-pi/src \
  python3 -m kiko_body
```

The equivalent launcher is `body/raspberry-pi/scripts/run-simulator.sh`.

Then enter one compact JSON command per line, for example:

```json
{"protocolVersion":1,"commandId":"demo-1","type":"GET_CAPABILITIES","arguments":{},"timeoutMs":1000}
```

The simulator prints the event that would be indicated to the Android BLE
central. It exercises parsing and controller behavior but does not open a radio
or move hardware.

## Test

```sh
PYTHONPATH=body/raspberry-pi/src \
  python3 -m unittest discover -s body/raspberry-pi/tests -v

PYTHONPATH=body/raspberry-pi/src \
  python3 -m unittest discover -s integration-tests -v
```

The package has no runtime dependency yet. A future BlueZ adapter and GPIO
driver should remain behind the existing `GattBodyService` and `MotionDriver`
boundaries so their dependencies do not enter the safety core.
