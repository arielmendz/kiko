# Kiko body

The target body is a separately deployed Raspberry Pi service inside the Kiko
monorepo. When physical integration is complete, it will receive semantic
commands over Bluetooth Low Energy (BLE), apply native safety limits, and drive a
pair of servos as crude, non-articulated legs. Today only the
transport-independent safety core and simulated servo driver exist.

The Android application never sends raw angles or pulse widths. The body owns
the tested seal-like step and dance trajectories, connection watchdog, command
deadlines, duplicate-command handling, and emergency stop.

See [`raspberry-pi/README.md`](raspberry-pi/README.md) for the executable
transport-independent scaffold. The BlueZ GATT binding and physical GPIO driver
remain future work.
