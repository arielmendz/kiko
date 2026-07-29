# Architecture

## Current design

The application is one Android app module with two runtime components:

- `MainActivity` owns microphone permission, foreground lifecycle, the Android
  `SpeechRecognizer`, and user-visible status.
- `WakeWordMatcher` is a platform-independent function that normalizes recognition
  hypotheses and detects the exact `kiko` token.

Data flows in one direction:

```text
microphone
  -> device on-device SpeechRecognizer
  -> partial/final text hypotheses
  -> WakeWordMatcher
  -> MainActivity screen state
```

The activity creates the recognizer after permission is granted, prefers the
device's on-device service, and never requests internet access. Recognition is
started on `onStart`, stopped on `onStop`, and destroyed on `onDestroy`. After a
result or recoverable error, listening restarts with a short delay while the
activity remains active.

## Platform and security choices

- Minimum Android version: Android 12 (API 31), where
  `createOnDeviceSpeechRecognizer` is available.
- Compile/target SDK: 37.
- Permission: `RECORD_AUDIO` only.
- Network permission: intentionally absent.
- Recognition language: Spanish, with partial results requested.

On-device recognition availability is device-dependent. The app reports an
unavailable state instead of falling back to a recognizer that might send audio to
a server. This preserves the local-first invariant.

## Intended boundaries

Future work should retain replaceable boundaries even if the concrete classes
change:

```text
AudioSource -> WakeWordDetector -> LocalInferenceEngine -> ActionPolicy
                                                        -> UsbBodyTransport
UsbBodyTransport -> BodyTelemetry ----------------------^
```

- `AudioSource` owns microphone frames and buffering.
- `WakeWordDetector` consumes audio locally and emits activation events.
- `LocalInferenceEngine` owns model loading, token generation, and device resource
  limits.
- `ActionPolicy` converts model intent into an allowlisted physical command set.
- `UsbBodyTransport` owns discovery, permissions, framing, version negotiation,
  reconnects, and telemetry.

No UI class should eventually contain model inference or USB protocol logic.

## Verification strategy

- Plain JVM unit tests cover normalization and wake-word boundaries.
- Android build and lint validate manifest/API integration when an SDK is present.
- A physical-device checklist will be added with the first repeatable microphone
  and USB integration test.
