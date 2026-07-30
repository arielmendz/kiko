# Integration tests

This directory will hold tests spanning Android, the shared BLE protocol, and
the Raspberry Pi body.

The initial integration gate should run Android's future `BodyBleTransport`
against a fake GATT peripheral using the messages in `protocol/fixtures/`. The
hardware gate should then repeat the same cases against a real Pi with the body
lifted and servo power independently controllable.

Required cases:

- capability negotiation before enabling movement;
- one through the advertised maximum number of steps;
- allowlisted dance and rejection of an unknown routine;
- duplicate command ID without repeated movement;
- explicit `STOP`;
- missed heartbeat and BLE disconnect;
- deadline expiry;
- malformed, oversized, and unknown-version messages;
- Android process/lifecycle loss; and
- reconnect without resuming the interrupted action.

No hardware integration test has run yet.
