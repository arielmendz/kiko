# Kiko

Kiko is an Android application for a local, embodied AI toy. Its final objective
is to run its intelligence entirely on the Android device, interact in Spanish,
use local camera/sensor/memory tools, and connect over Bluetooth Low Energy (BLE)
to a Raspberry Pi body controlling two servos. It is a personal, noncommercial
project.

Kiko currently listens with Android's on-device speech recognizer and displays
**“escuchando!”** whenever it hears the wake word **“kiko”**. For the first
perception use case, “Kiko, ¿qué ves?”, it captures one front-camera frame,
runs a downloaded YOLO26n object detector locally, and speaks a short Spanish
observation with a deliberately simple offline voice. It also provides a
download-only library of local GGUF language models; language-model inference is
not implemented yet.

## Current scope

- Native Android app written in Java.
- On-device speech recognition; microphone audio is never sent by Kiko.
- Checks for an installed Spanish on-device model and asks the system speech
  service to download one when necessary.
- Spanish wake-word matching ignores case, punctuation, and accents, and accepts
  common `Quico`/`Quiko` transcriptions.
- Clear UI states for permission, recognition availability, listening, and match.
- Displays the latest recognition hypothesis or actionable error on screen for
  physical-device diagnosis.
- Accepts “¿qué ves?” in the same utterance as “Kiko” or during a ten-second
  command window after the wake word.
- Requests camera permission only for that explicit command, captures one
  front-camera still in memory, and releases the camera immediately.
- Runs YOLO26n through ONNX Runtime on the device and reports up to three detected
  COCO object types in Spanish.
- Saves every completed “¿qué ves?” capture with Kiko's Spanish result in a
  private, on-device **Historial visual**, including the “nothing recognized” or
  analysis-error result. Captures can be deleted individually or all at once.
- Speaks the displayed result with an installed Spanish TTS voice only when that
  voice declares that it does not require a network connection.
- Offers pinned Gemma 3 1B, Bonsai 1.7B, Qwen3 0.6B, and LFM2.5 350M GGUF
  downloads plus the runnable YOLO26n vision artifact, with progress,
  cancellation, deletion, and SHA-256 verification.
- Stores model files in Kiko's app-specific external directory so uninstalling the
  app removes them without requesting broad storage access.
- Includes a transport-independent Raspberry Pi body safety core, strict BLE
  protocol parser, two-servo motion simulator, and deterministic tests.

The current implementation uses the speech recognizer and text-to-speech engine
supplied by Android. These are temporary platform dependencies: future
local-model milestones will replace them with app-owned wake-word and voice
components. Vision inference is app-owned and local, using the MIT-licensed ONNX
Runtime directly rather than a cloud or analytics SDK. YOLO26n is an AGPL-3.0
bounded COCO object detector, not a vision-language model or detailed scene
captioner. Kiko is consequently licensed under AGPL-3.0-only. The body scaffold
does not yet advertise through BlueZ, drive GPIO, or connect from Android, so the
Android app does not request Bluetooth permissions yet.

## Run it

Requirements:

- Android Studio with Android SDK 37 installed.
- JDK 17 or newer.
- A physical Android 12+ device with an on-device recognition service.

Open the repository in Android Studio, let Gradle sync, and run the `app`
configuration. Open **Modelos locales** and explicitly download
**YOLO26n** before trying perception. Grant microphone access, then say
“Kiko, ¿qué ves?” (or say “Kiko” and then “¿qué ves?” within ten seconds), grant
camera access, and face the phone's front camera toward the scene. Kiko displays
the response even if the device has no installed offline Spanish TTS voice. Open
**Historial visual** on the wake-word screen to inspect the exact saved image and
label, delete one capture, or erase the complete history.

If a Spanish model needs to be downloaded, leave the phone online until the system
speech service completes it, then close and reopen Kiko. Audio recognition remains
on-device.

Open **Modelos locales** from the wake-word screen to manage language and vision
model downloads. Kiko requests network access solely for user-initiated
model-file downloads; it does not send prompts, microphone audio, camera images,
labels, or inference data anywhere. Visual-history JPEGs and labels remain in
Kiko's private internal storage until the user deletes them or uninstalls the app.
Gemma requires accepting Google's Gemma terms on Hugging Face and entering a
read-only Hugging Face token. The token is encrypted with Android Keystore.

The five catalog files require 2,130,544,293 bytes in total. Downloads continue
through Android's system download manager if the model screen closes. Details and
pinned sources are in `docs/MODELS.md`.

From a configured command line:

```sh
./gradlew test
./gradlew assembleDebug
```

Run and test the Raspberry Pi body simulator independently:

```sh
PYTHONPATH=body/raspberry-pi/src \
  python3 -m unittest discover -s body/raspberry-pi/tests -v

PYTHONPATH=body/raspberry-pi/src \
  python3 -m unittest discover -s integration-tests -v

PYTHONPATH=body/raspberry-pi/src \
  python3 -m kiko_body
```

## Project map

- `app/src/main/java/com/kiko/app/MainActivity.java` owns the activity lifecycle,
  permission flow, recognizer lifecycle, and screen state.
- `app/src/main/java/com/kiko/app/WakeWordMatcher.java` contains the deterministic,
  independently tested wake-word matcher.
- `app/src/main/java/com/kiko/app/SpeechLanguageSelector.java` selects an installed
  Spanish recognition model without coupling that logic to Android UI state.
- `app/src/main/java/com/kiko/app/SpanishCommandMatcher.java` recognizes the
  bounded “¿qué ves?” command without a language model.
- `app/src/main/java/com/kiko/app/FrontCameraCapture.java` owns one-shot in-memory
  front-camera capture and camera release.
- `app/src/main/java/com/kiko/app/LocalVisionEngine.java` produces the current
  YOLO26n object detections through direct local ONNX Runtime inference and hands
  the oriented capture and result to the private visual-history store.
- `app/src/main/java/com/kiko/app/VisualHistoryStore.java` atomically stores,
  lists, and deletes private capture/label records.
- `app/src/main/java/com/kiko/app/VisualHistoryActivity.java` displays every saved
  capture and provides per-item and erase-all controls.
- `app/src/main/java/com/kiko/app/Yolo26DetectionParser.java` validates the pinned
  model's end-to-end rows, class indices, and confidence threshold outside Android
  UI state.
- `app/src/main/java/com/kiko/app/SpanishSceneDescription.java` composes the
  structured observation in Spanish.
- `app/src/main/java/com/kiko/app/OfflineSpanishSpeaker.java` selects only an
  installed non-network Spanish voice and applies the simple robotic profile.
- `app/src/main/java/com/kiko/app/ModelLibraryActivity.java` renders the model
  catalog and download controls.
- `app/src/main/java/com/kiko/app/ModelDownloadStore.java` owns system downloads,
  private model storage, progress, cancellation, and integrity verification.
- `docs/MODELS.md` records exact artifacts, licenses, and authentication
  requirements.
- `docs/MODEL_RESEARCH.md` evaluates local tool-calling and vision models for
  sensors and the BLE-connected body.
- `docs/COMMANDS.md` defines the Spanish command, memory, face-recognition, and
  toy-safety contract.
- `docs/FLOWS.md` contains Mermaid architecture sequences for each supported toy
  behavior and its failure paths.
- `body/raspberry-pi/` contains the independently deployable body safety core,
  simulated motion driver, GATT boundary, and tests.
- `protocol/` is the shared, versioned BLE command and event contract.
- `hardware/` records the two-servo body, parts, wiring, calibration, and power
  decisions before physical integration.
- `integration-tests/` defines cross-device and failure-case acceptance tests.
- `docs/PRODUCT.md` defines the product goal, current milestone, and roadmap.
- `docs/ARCHITECTURE.md` records the present design and intended architectural
  boundaries.
- `AGENTS.md` is the source of truth for AI maintainers.

## AI maintenance

This repository is maintained exclusively by AI agents. Read `AGENTS.md` before
making changes. Every behavior or architecture change must update the relevant
documentation in the same commit.

## License

Kiko is licensed under AGPL-3.0-only because its runnable YOLO26n weights are
AGPL-3.0. See `LICENSE` and `docs/MODELS.md`. Proprietary or commercial deployment
requires a new project-license review and an applicable Ultralytics Enterprise
license.
