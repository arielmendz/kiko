# Wiring

A wiring diagram will be added after the Raspberry Pi, servos, driver strategy,
and power supplies are selected.

Required electrical invariants:

- Never draw servo power from the phone.
- Do not assume a Raspberry Pi GPIO pin can supply servo current.
- Size the servo rail for measured stall current with an appropriate fuse.
- Establish the required signal-ground reference according to the selected
  driver and power design.
- Keep a physical power cutoff reachable while testing.
- Boot and test with the body mechanically lifted so an unexpected stroke cannot
  propel it.

GPIO pin numbers, pulse widths, and voltage levels must remain absent from code
until this document records the verified physical design.
