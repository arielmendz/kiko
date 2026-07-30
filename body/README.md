# Kiko body

The body is a separately deployed Raspberry Pi service inside the Kiko
monorepo. It receives semantic commands over Bluetooth Low Energy (BLE), applies
native safety limits, and drives a pair of servos as crude, non-articulated
legs.

The Android application never sends raw angles or pulse widths. The body owns
the tested seal-like step and dance trajectories, connection watchdog, command
deadlines, duplicate-command handling, and emergency stop.

See [`raspberry-pi/README.md`](raspberry-pi/README.md) for the executable
transport-independent scaffold. The BlueZ GATT binding and physical GPIO driver
remain future work.
