# Architecture

## Current design

The application is one Android app module with three runtime components:

- `MainActivity` owns microphone permission, foreground lifecycle, the Android
  `SpeechRecognizer`, and user-visible status.
- `WakeWordMatcher` is a platform-independent function that normalizes recognition
  hypotheses and detects the exact `kiko` token.
- `SpeechLanguageSelector` is a platform-independent function that prefers
  `es-US` and falls back to another Spanish model reported by the device.

Data flows in one direction:

```text
microphone
  -> device on-device SpeechRecognizer
  -> partial/final text hypotheses
  -> WakeWordMatcher
  -> MainActivity screen state
```

The activity creates the recognizer after permission is granted, requires the
device's on-device service, and never requests internet access. On Android 13 and
newer it checks installed Spanish support before listening. If the system supports
Spanish but its model is missing, the app asks the system speech service to
download that model and waits for the user to reopen Kiko after completion.
Recognition is started on `onStart`, stopped on `onStop`, and destroyed on
`onDestroy`.

Partial and final hypotheses are both sent to `WakeWordMatcher` and shown on screen
for diagnosis. Silence/no-match errors restart after a delay; busy errors use a
longer delay. Language, permission, and unexpected errors stop the blind retry
loop and display an actionable state.

## Platform and security choices

- Minimum Android version: Android 12 (API 31), where
  `createOnDeviceSpeechRecognizer` is available.
- Compile/target SDK: 37.
- Permission: `RECORD_AUDIO` only.
- Network permission: intentionally absent.
- Preferred recognition language: `es-US`, falling back to another installed
  Spanish language tag when the service reports one.
- Partial results: requested and inspected when the service supplies them.

On-device recognition availability is device-dependent. The app reports an
unavailable state instead of falling back to a recognizer that might send audio to
a server. This preserves the local-first invariant.

Android's `SpeechRecognizer` remains an utterance-oriented bootstrap dependency,
so microphone sessions can visibly cycle during silence. Reliable continuous
operation requires the planned app-owned streaming wake-word detector.

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
- A Redmi Note 10 Pro on Android 13 has produced `ki`, `kik`, and `Kiko` partial
  hypotheses plus `Kiko`/`Quico` final alternatives, confirming the current
  end-to-end wake-word path.
- A repeatable device test is still required before microphone behavior can be
  treated as regression-tested; USB integration remains untested.
