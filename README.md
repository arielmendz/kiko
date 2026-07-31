# Kiko

Kiko is an Android application for a local, embodied AI toy. Its final objective
is to run its intelligence entirely on the Android device, interact in Spanish,
use local camera/sensor/memory tools, and connect over Bluetooth Low Energy (BLE)
to a Raspberry Pi body controlling two servos. It is a personal, noncommercial
project.

Kiko currently listens with Android's on-device speech recognizer and displays
**“escuchando!”** whenever it hears the wake word **“kiko”**. Native googly eyes
open, blink, and look side to side while the recognizer listens. For the first
perception use case, “Kiko, ¿qué ves?”, the eyes squint, a live rear-camera
viewport appears, Kiko captures one frame, runs a downloaded YOLO26n object
detector locally, optionally compares one visible face with explicitly enrolled
SFace embeddings, and speaks a short Spanish observation with a deliberately
simple offline voice. Kiko also remembers explicitly stated favorite foods,
likes, and ages for named people, plus equivalent facts, species, and owners for
named cats and dogs, in encrypted local registries and answers bounded questions
about them. It provides a download-only library of local GGUF
language models; language-model inference is not implemented yet.

## Current scope

- Native Android app written in Java.
- On-device speech recognition; microphone audio is never sent by Kiko.
- Checks for an installed Spanish on-device model and asks the system speech
  service to download one when necessary.
- Spanish wake-word matching ignores case, punctuation, and accents, and accepts
  common `Quico`/`Quiko` transcriptions.
- Clear UI states for permission, recognition availability, listening, and match.
- Shows lightweight native googly eyes that animate only while listening and
  squint for the complete “¿qué ves?” request.
- Displays the latest recognition hypothesis or actionable error on screen for
  physical-device diagnosis.
- Accepts “¿qué ves?” in the same utterance as “Kiko” or during a ten-second
  command window after the wake word.
- Requests camera permission only for that explicit command, shows the rear-camera
  feed live for framing, captures one still in memory, and releases the camera
  immediately after capture.
- Runs YOLO26n through ONNX Runtime on the device and reports up to three detected
  COCO object types in Spanish.
- Saves every completed “¿qué ves?” capture with Kiko's Spanish result in a
  private, on-device **Historial visual**, including the “nothing recognized” or
  analysis-error result. Captures can be deleted individually or all at once.
- When YOLO emits the `person` class, prepares one face locally and compares its
  SFace embedding with encrypted, explicitly confirmed identities. A clear match
  answers “Veo a <nombre>”; an unknown face asks “Veo una persona, no la conozco,
  ¿quién es?” and listens locally for a short name. Saving requires an unlocked
  on-screen confirmation.
- Stores confirmed face embeddings with AES-GCM under an Android Keystore key.
  Historial visual lets the unlocked user forget one identity while retaining its
  photo and removes linked identities when a photo or the complete history is
  deleted. Face matches are toy labels, never authentication.
- After “kiko”, accepts explicit structured facts such as “la comida favorita de
  Pedro es la pasta”, “a Pedro le gusta el fútbol”, and “Pedro tiene 10 años”. It
  displays **memoria actualizada**, encrypts the record locally, and never infers
  unstated facts.
- Answers “¿qué le gusta a Pedro?”, “¿qué sabes de Pedro?”, and “¿cuál es la
  comida favorita de Pedro?” from that encrypted registry. The unlocked
  **Memorias** screen lists each person and pet and supports delete-one and
  erase-all.
- Registers cats and dogs with explicit phrases such as “Luna es la gata de
  Pedro”, then accepts species-qualified favorite-food, like, and age facts. It
  answers questions about that pet and “¿qué mascotas tiene Pedro?” without
  treating unsupported animals or an unqualified name as a pet.
- Speaks the displayed result with an installed Spanish TTS voice only when that
  voice declares that it does not require a network connection.
- Offers pinned Gemma 3 1B, Bonsai 1.7B, Qwen3 0.6B, and LFM2.5 350M GGUF
  downloads plus the runnable YOLO26n and SFace vision artifacts, with progress,
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
bounded COCO object detector; SFace is an Apache-2.0 face-embedding model. Neither
is a vision-language model or detailed scene captioner. Kiko is consequently
licensed under AGPL-3.0-only. The body scaffold
does not yet advertise through BlueZ, drive GPIO, or connect from Android, so the
Android app does not request Bluetooth permissions yet.

## Run it

Requirements:

- Android Studio with Android SDK 37 installed.
- JDK 17 or newer.
- A physical Android 12+ device with an on-device recognition service.

Open the repository in Android Studio, let Gradle sync, and run the `app`
configuration. Open **Modelos locales** and explicitly download **YOLO26n** and
**SFace** before trying person recognition. Grant microphone access, then say
“Kiko, ¿qué ves?” (or say “Kiko” and then “¿qué ves?” within ten seconds), grant
camera access, and point the phone's rear camera toward the scene. Kiko shows the
live viewport before taking the still and displays the response even if the
device has no installed offline Spanish TTS voice. Open **Historial visual** on
the wake-word screen to inspect the exact saved image and label, forget one
identity, delete one capture, or erase the complete history. If Kiko finds an
unknown usable face, answer with a name (or say “cancelar”) and confirm
**Guardar** on screen to enroll it. On a later clear match, Kiko answers with that
name. Photo labels created by older builds remain visible as legacy labels but
are not silently converted into biometric enrollments.

For person or pet memory, say “Kiko”, then a supported explicit fact. Kiko speaks a
short reaction and shows **memoria actualizada**. On a later day, say “Kiko” and
ask “¿qué le gusta a Pedro?”, “¿qué sabes de Pedro?”, or “¿cuál es la comida
favorita de Pedro?”. For a pet, first say “Luna es la gata de Pedro”; then use
phrases such as “la gata Luna tiene 3 años”, “¿qué sabes de la gata Luna?” or
“¿qué mascotas tiene Pedro?”. Open **Memorias** to inspect or erase the
structured person and pet facts.

If a Spanish model needs to be downloaded, leave the phone online until the system
speech service completes it, then close and reopen Kiko. Audio recognition remains
on-device.

Open **Modelos locales** from the wake-word screen to manage language and vision
model downloads. Kiko requests network access solely for user-initiated
model-file downloads; it does not send prompts, microphone audio, camera images,
labels, or inference data anywhere. Visual-history JPEGs and labels remain in
Kiko's private internal storage until the user deletes them or uninstalls the app.
Face names and 128-value embeddings are additionally AES-GCM encrypted with a key
held by Android Keystore.
Person-memory names, favorite foods, likes, and ages use an AES-GCM registry
protected by Android Keystore. Pet names, cat/dog kind, owners, favorite foods,
likes, and ages use a second key and encrypted registry.
Gemma requires accepting Google's Gemma terms on Hugging Face and entering a
read-only Hugging Face token. The token is encrypted with Android Keystore.

The six catalog files require 2,169,240,646 bytes in total. Downloads continue
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
- `app/src/main/java/com/kiko/app/KikoEyesView.java` draws the native googly eyes;
  `KikoEyeMotion.java` provides deterministic blink, gaze, and squint samples.
- `app/src/main/java/com/kiko/app/SceneCameraCapture.java` owns the rear-camera
  live preview, delayed one-shot capture, and camera release.
- `app/src/main/java/com/kiko/app/LocalVisionEngine.java` produces the current
  YOLO26n object detections through direct local ONNX Runtime inference, delegates
  person faces to `LocalFaceRecognizer`, and hands the oriented capture and result
  to the private visual-history store.
- `app/src/main/java/com/kiko/app/LocalFaceRecognizer.java` uses Android's local
  face geometry to prepare one crop, runs the verified SFace ONNX embedding model,
  and accepts only thresholded, unambiguous matches.
- `app/src/main/java/com/kiko/app/FaceIdentityStore.java` encrypts explicitly
  confirmed names and embeddings with Android Keystore and supports targeted or
  complete deletion.
- `app/src/main/java/com/kiko/app/SpanishPersonMemoryParser.java` recognizes the
  bounded person-fact declarations and three query families without a language
  model.
- `app/src/main/java/com/kiko/app/PersonMemoryStore.java` encrypts structured
  person facts with Android Keystore; `PersonMemoryActivity.java` provides the
  shared person/pet inspect, delete-one, and erase-all owner controls.
- `app/src/main/java/com/kiko/app/SpanishPetMemoryParser.java` recognizes the
  bounded cat/dog declarations and queries; `PetMemoryStore.java` encrypts those
  structured records under a separate Android Keystore key.
- `app/src/main/java/com/kiko/app/VisualHistoryStore.java` atomically stores,
  lists, and deletes private capture/label records.
- `app/src/main/java/com/kiko/app/VisualHistoryActivity.java` displays every saved
  capture and provides per-item and erase-all controls.
- `app/src/main/java/com/kiko/app/Yolo26DetectionParser.java` validates the pinned
  model's end-to-end rows, class indices, and confidence threshold outside Android
  UI state.
- `app/src/main/java/com/kiko/app/SpanishSceneDescription.java` composes the
  structured observation in Spanish.
- `app/src/main/java/com/kiko/app/SpanishPersonNameExtractor.java` bounds and
  normalizes local speech hypotheses for explicit face enrollment.
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
