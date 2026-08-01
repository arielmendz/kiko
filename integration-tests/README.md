# Integration tests

This directory currently verifies that the Python Raspberry Pi parser accepts
the same versioned JSON fixtures used by Android's protocol-codec tests. In other
words, both sides agree on the shape and meaning of the sealed messages before a
Bluetooth link exists.

Run the delivered cross-language fixture checks with:

```sh
PYTHONPATH=body/raspberry-pi/src \
  python3 -m unittest discover -s integration-tests -v
```

No BLE radio, Raspberry Pi, GPIO, or servo is used by these tests.

The next integration gate should run Android's future `BodyBleTransport`
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

The protocol compatibility gate exists; no Bluetooth or hardware integration
test has run yet.
