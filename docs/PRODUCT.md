# Product

## Vision

Kiko is the mind of a physical companion toy: an Android application that runs
local AI models, observes phone sensors, remembers user-provided facts and
enrolled faces, and connects over Bluetooth Low Energy (BLE) to a Raspberry Pi
body controlling two servos. The servos act as crude, non-articulated legs that
move the toy with a seal-like motion. The completed product interacts in Spanish
and must not depend on cloud inference for its core intelligence.

Kiko is a personal, noncommercial project. A future decision to distribute it
commercially requires a new review of every model, dataset, dependency, privacy
assumption, and physical-safety requirement.

## Target toy experience

After hearing “kiko”, the completed toy supports these Spanish behaviors:

1. move an exact, safe number of steps;
2. perform a pre-tested dance;
3. answer “¿qué sabes de X?” from a local model and relevant local memories;
4. describe a current camera frame for “¿qué ves?”;
5. identify explicitly enrolled people for “¿a quién ves?”;
6. remember an explicit fact or confirmed textual scene observation for later; and
7. enroll, retrieve, and forget local face, fact, and observation memories.

The detailed language, clarification, retention, and action contract is in
`docs/COMMANDS.md`. English commands and open-ended third-party automation are
outside the initial product scope.

## Delivered milestone: wake-word screen

While the app is visible:

1. it requests microphone permission;
2. it starts Android's on-device Spanish speech recognizer;
3. it verifies that an on-device Spanish model is installed, requesting a system
   model download when one is available but missing;
4. it listens for “kiko”; and
5. it displays “escuchando!” when a recognition hypothesis contains that word.

Matching is case-insensitive, accent-insensitive, and token-based, so punctuation
around the word is accepted while substrings such as “kikongo” are rejected.
`Quico` and `Quiko` are accepted because they are common speech-to-text spellings
of the spoken name. The screen shows the latest hypothesis or a meaningful error
to make device-specific recognition behavior observable.

## Delivered milestone: downloadable local models

From **Modelos locales**, the user can:

1. inspect four language artifacts and the YOLO26n vision artifact;
2. see each artifact's model details, quantization, size, and license;
3. start a model download and leave the screen while Android continues it;
4. see progress, cancel an active download, retry a failure, or delete a model;
5. use the system browser to inspect the upstream source and license; and
6. receive a usable model file only after exact byte-size and SHA-256 verification.

Gemma is gated by Google on Hugging Face. The user must accept the Gemma license
externally and provide a Hugging Face read token. Kiko encrypts that token with
Android Keystore and never logs it.

The language artifacts remain download-only. YOLO26n is loaded only for an
explicit “¿qué ves?” request.

## Current milestone: first local “¿qué ves?” loop

After hearing “Kiko”, the visible app opens a ten-second command window. If the
user says “¿qué ves?” in that window, or says “Kiko, ¿qué ves?” in one utterance,
Kiko:

1. requests camera permission only when needed;
2. captures one still from the front camera into memory;
3. closes the camera immediately;
4. executes the verified YOLO26n ONNX artifact locally through ONNX Runtime and
   selects up to three detected COCO object types;
5. displays a short Spanish observation and speaks it through an installed
   non-network Spanish TTS voice; and
6. discards the frame without writing it to storage or creating face memory.

If the vision artifact is absent or unverified, Kiko does not open the camera and
asks the user in Spanish to download it from **Modelos locales**. This is bounded
object detection, not a free-form scene caption. It does not name people, infer
identity, retain face data, or execute a vision-language model.

## Current limitations

- Recognition is active only while the activity is in the foreground.
- Recognition quality and availability depend on the device's installed on-device
  recognition service.
- Android's utterance-oriented recognizer still times out and restarts during
  silence; it is not a production-quality continuous wake-word detector.
- The current wake-word mechanism is a bootstrap dependency on an Android platform
  service, not the final app-owned local model.
- No language-model or vision-language-model inference is implemented, and there
  is no Android-to-body BLE communication yet. Object-detection inference is the
  only app-owned model execution.
- “¿Qué ves?” is limited to the 80 COCO classes known by YOLO26n. It
  cannot reliably describe activities, relationships, text, unfamiliar objects,
  or identity.
- YOLO26n must be explicitly downloaded and verified before the command can run.
- The YOLO26n weights are AGPL-3.0; Kiko is therefore AGPL-3.0-only. A future
  proprietary or commercial deployment requires an applicable Ultralytics
  Enterprise license and a new project-license review.
- The universal debug APK carries ONNX Runtime native libraries for every bundled
  ABI and is materially larger than the former LiteRT build. A future
  distribution build should split APKs or app bundles by ABI.
- Spoken output requires an installed Spanish voice that Android marks as not
  requiring a network connection; otherwise the complete answer remains visible.
- Front-camera capture and Spanish TTS still require physical-device validation.
- The Raspberry Pi safety core and two-servo simulator exist, but BlueZ
  advertising, GPIO output, hardware calibration, and physical-servo validation
  are not implemented.
- Downloaded models are removed when Kiko is uninstalled because they live in the
  app-specific external files directory.
- Bonsai's Q1 format requires Prism ML's specialized llama.cpp kernels; selecting
  an inference runtime remains future work.
- Physical-device validation on a Redmi Note 10 Pro running Android 13 confirmed
  partial and final “Kiko” detection after the diagnostic fix. Other recognizers
  and device models remain unverified.

## Roadmap

1. **Wake-word screen (delivered):** recognize “kiko” locally and show
   “escuchando!”.
2. **Model library (delivered):** securely download and verify pinned local model
   artifacts without executing them.
3. **Local scene baseline (delivered):** route “¿qué ves?” deterministically,
   capture one ephemeral front-camera frame, run YOLO26n on-device, and
   report bounded object detections with an offline Spanish voice.
4. **Embodied tool contract:** define typed read-only sensor tools and
   state-changing body tools, plus native validation, confirmation, deadlines,
   and emergency-stop behavior.
5. **Spanish command core:** implement deterministic stop, step-count, dance, and
   clarification parsing plus local Spanish responses.
6. **App-owned audio pipeline:** replace the platform recognizer with deterministic
   streaming audio capture and a bundled wake-word model.
7. **Local action router:** benchmark and run a Kiko-specific FunctionGemma 270M
   fine-tune and larger tool-capable baselines on supported Android hardware.
8. **BLE body link (scaffolded):** finish the Android BLE central and Raspberry Pi
   BlueZ peripheral around the versioned GATT command/event protocol, bonding,
   capability negotiation, heartbeats, reconnects, and emergency stop.
9. **Local memory:** store confirmed facts and explicitly enrolled face embeddings
   with inspect, forget, and erase-all controls.
10. **Local vision expansion:** benchmark richer app-owned scene models and
    recognize only locally enrolled faces without sending images or SDK telemetry
    to a cloud service.
11. **Embodied loop:** connect wake word, local inference, safety policy, physical
   actions, telemetry, and recovery into an offline-first experience.

The current model recommendation and alternatives are recorded in
`docs/MODEL_RESEARCH.md`. It is a benchmark hypothesis rather than a shipped
default.

## Non-goals for the current milestone

- Background or always-on listening.
- Cloud speech recognition or cloud AI.
- Android BLE discovery, bonding, or body control.
- Raspberry Pi BlueZ advertising, GPIO servo output, or physical movement.
- Language-model, vision-language-model, face-identity, or activity-captioning
  inference. The system speech service may download its own Spanish recognition
  pack, and the model library downloads only artifacts explicitly selected by the
  user.
