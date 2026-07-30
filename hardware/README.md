# Kiko body hardware

The initial body is a Raspberry Pi controlling two servos that act as crude,
non-articulated legs. Alternating bounded servo strokes move the body with a
seal-like motion; an allowlisted routine produces a simple dance.

No physical wiring is approved yet. Before connecting hardware, record:

- Raspberry Pi model and operating-system image;
- exact servo model, stall current, voltage, and travel;
- dedicated regulated servo power supply and fuse;
- GPIO pins and pulse-width limits;
- measured neutral and safe mechanical angles;
- a physical pairing/reset control;
- an accessible hardware power cutoff; and
- cable strain relief and enclosure details.

The phone must not power the servos. Servo power noise and stall current can
brown out the Pi, damage hardware, or produce uncontrolled movement. Use a
properly sized external supply and follow the selected hardware manufacturers'
instructions.

The software defaults in the simulator are provisional test values, not approved
physical calibration. The future GPIO driver must clamp again at the hardware
boundary and return both servos to safe neutral when stopped.

See [`wiring/README.md`](wiring/README.md) and [`bom.md`](bom.md).
