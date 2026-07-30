# Architecture

## Current design

The application is one Android app module with wake-word and model-management
areas.

Wake-word components:

- `MainActivity` owns microphone permission, foreground lifecycle, the Android
  `SpeechRecognizer`, and user-visible status.
- `WakeWordMatcher` is a platform-independent function that normalizes recognition
  hypotheses and detects the exact `kiko` token.
- `SpeechLanguageSelector` is a platform-independent function that prefers
  `es-US` and falls back to another Spanish model reported by the device.

Model-management components:

- `ModelCatalog` defines immutable upstream GGUF artifacts, expected sizes,
  SHA-256 hashes, licenses, and gating requirements.
- `ModelLibraryActivity` renders catalog state and handles explicit user actions.
- `ModelDownloadStore` delegates durable transfer to Android `DownloadManager`,
  persists download identifiers, reports progress, and finalizes verified files.
- `HuggingFaceTokenStore` encrypts the optional Gemma read token with an AES-GCM
  key held by Android Keystore.

Data flows in one direction:

```text
microphone
  -> device on-device SpeechRecognizer
  -> partial/final text hypotheses
  -> WakeWordMatcher
  -> MainActivity screen state
```

The activity creates the recognizer after permission is granted and requires the
device's on-device service. The recognition path never makes a network request.
On Android 13 and newer it checks installed Spanish support before listening. If
the system supports Spanish but its model is missing, the app asks the system
speech service to download that model and waits for the user to reopen Kiko after
completion.
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
- Permissions: `RECORD_AUDIO` for the wake word and `INTERNET` for explicit model
  artifact downloads.
- Network activity is forbidden for recognition, prompts, inference, analytics,
  and silent catalog fetching.
- Preferred recognition language: `es-US`, falling back to another installed
  Spanish language tag when the service reports one.
- Partial results: requested and inspected when the service supplies them.

On-device recognition availability is device-dependent. The app reports an
unavailable state instead of falling back to a recognizer that might send audio to
a server. This preserves the local-first invariant.

Android's `SpeechRecognizer` remains an utterance-oriented bootstrap dependency,
so microphone sessions can visibly cycle during silence. Reliable continuous
operation requires the planned app-owned streaming wake-word detector.

## Model download flow

```text
ModelCatalog
  -> explicit user download action
  -> Android DownloadManager
  -> app-specific external models/<filename>.part
  -> exact size + SHA-256 verification
  -> rename to models/<filename>.gguf
  -> download-only ready state
```

Transfers continue under the system download manager when the model screen is not
visible. Kiko polls persisted download IDs while the screen is active. Canceling
removes the system transfer and partial file. Deleting removes the verified file.
Uninstalling Kiko removes the app-specific model directory.

Catalog URLs include immutable Hugging Face repository revisions instead of
`main`. The expected file sizes and SHA-256 hashes are recorded in code and
`docs/MODELS.md`. A download is never promoted from `.part` to `.gguf` unless both
checks pass.

Gemma is a special authenticated path: the user accepts its license externally,
then supplies a read token. The token is encrypted at rest with Android Keystore,
is passed only as an authorization header to Android's download manager, and is
never logged. Other catalog artifacts are public and require no credential.

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
- Catalog tests require four unique entries with immutable revisions, known sizes,
  `.gguf` filenames, and SHA-256 hashes.
- Android build and lint validate manifest/API integration when an SDK is present.
- A Redmi Note 10 Pro on Android 13 has produced `ki`, `kik`, and `Kiko` partial
  hypotheses plus `Kiko`/`Quico` final alternatives, confirming the current
  end-to-end wake-word path.
- A repeatable device test is still required before microphone behavior can be
  treated as regression-tested; USB integration remains untested.
- Download endpoint probes validate the GGUF magic bytes for all public catalog
  artifacts. Full transfer, cancellation, resumption, and checksum verification
  still require physical-device validation.
