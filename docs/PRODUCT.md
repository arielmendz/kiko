# Product

## Vision

Kiko is the mind of a physical companion toy: an Android application that runs
local AI models, observes phone sensors, remembers user-provided facts and
enrolled faces—including structured facts about people, cats, and dogs—and
connects over Bluetooth Low Energy (BLE) to a Raspberry Pi body controlling two
servos. The servos act as crude, non-articulated legs that
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
5. it opens, blinks, and moves a pair of native googly eyes side to side while
   actively listening; and
6. it displays “escuchando!” when a recognition hypothesis contains that word.

Matching is case-insensitive, accent-insensitive, and token-based, so punctuation
around the word is accepted while substrings such as “kikongo” are rejected.
`Quico` and `Quiko` are accepted because they are common speech-to-text spellings
of the spoken name. The screen shows the latest hypothesis or a meaningful error
to make device-specific recognition behavior observable.

## Delivered milestone: downloadable local models

From **Modelos locales**, the user can:

1. inspect four language artifacts plus the YOLO26n object and SFace embedding
   artifacts;
2. see each artifact's model details, quantization, size, and license;
3. start a model download and leave the screen while Android continues it;
4. see progress, cancel an active download, retry a failure, or delete a model;
5. use the system browser to inspect the upstream source and license; and
6. receive a usable model file only after exact byte-size and SHA-256 verification.

Gemma is gated by Google on Hugging Face. The user must accept the Gemma license
externally and provide a Hugging Face read token. Kiko encrypts that token with
Android Keystore and never logs it.

The language artifacts remain download-only. YOLO26n and SFace are loaded only
for an explicit “¿qué ves?” request.

## Current milestone: first local “¿qué ves?” loop

After hearing “Kiko”, the visible app opens a ten-second command window. If the
user says “¿qué ves?” in that window, or says “Kiko, ¿qué ves?” in one utterance,
Kiko:

1. requests camera permission only when needed;
2. squints its animated eyes and shows a live rear-camera viewport;
3. keeps the live viewport visible for a short framing interval, captures one
   still into memory, then closes the camera and hides the viewport;
4. executes the verified YOLO26n ONNX artifact locally through ONNX Runtime and
   selects up to three detected COCO object types;
5. when YOLO reports a person, uses local face geometry and the verified SFace
   ONNX artifact to compare one usable face with explicitly enrolled identities;
6. displays a short Spanish observation and speaks it through an installed
   non-network Spanish TTS voice; and
7. saves the oriented image and the exact Spanish result in private on-device
   visual history, including an empty-detection or analysis-error result.

If one face clearly matches an enrolled identity, Kiko responds “Veo a
<nombre>.” If no identity clears the `0.50` cosine threshold and `0.08`
best-versus-runner-up margin, the result is “Veo una persona, no la conozco,
¿quién es?”. Kiko then listens locally for at most two short name attempts within
twelve seconds. Tapping **Guardar** in the unlocked on-screen confirmation stores
the name, source-photo link, and normalized 128-value embedding in an AES-GCM
registry protected by Android Keystore. Saying “cancelar”, timing out, rejecting
the dialog, or failing validation leaves the photo unnamed and stores no identity.

The **Historial visual** screen shows every saved capture newest first. The user
can inspect enrolled names, forget one identity while keeping its photo, delete
one capture and its linked identity, or erase all captures and identities. These
owner operations require the phone to be unlocked. Records remain until deletion
or app uninstall; there is intentionally no automatic retention cap in this
troubleshooting milestone. Names created by an older non-biometric build remain
visible as legacy labels and are not silently enrolled.

If the vision artifact is absent or unverified, Kiko does not open the camera and
asks the user in Spanish to download it from **Modelos locales**. This is bounded
object detection, not a free-form scene caption. If YOLO sees a person but SFace
is absent or unverified, the capture is retained and Kiko asks the user to
download SFace instead of guessing or opening enrollment. No image, embedding,
name, or match leaves the device, and no vision-language model runs.

## Delivered milestone: structured person and pet memory

After “kiko”, Kiko recognizes three complete, explicit fact forms without a
language model:

1. “la comida favorita de <nombre> es <comida>” replaces that person's favorite
   food;
2. “a <nombre> le gusta <gusto>” adds one deduplicated like; and
3. “<nombre> tiene <1–130> años” replaces the stored age.

The complete declaration is the explicit storage instruction. A successful
update produces a short Spanish response and shows **memoria actualizada**. Names
are limited to three words, fact values to five words, each person to twenty
likes, and the registry to one hundred people. Unsupported or malformed claims
are not stored.

After a later wake word, “¿qué le gusta a <nombre>?”, “¿qué sabes de
<nombre>?”, and “¿cuál es la comida favorita de <nombre>?” read only the matching
structured record. Kiko combines known values, deduplicates a favorite food from
general likes, and says that it does not know when the person or requested field
is absent. It does not fill gaps with model knowledge.

`PersonMemoryStore` encrypts names and facts as one AES-GCM registry under a
separate Android Keystore key. The unlocked **Memorias** screen lists each
person's facts and update time, deletes one complete person record, or erases the
whole registry. Uninstall also removes it. Person facts and facial identities are
separate stores: a face match cannot retrieve or disclose these memories.

Pet memory is a second closed grammar and encrypted registry. Kiko accepts only
cats and dogs, expressed as `gato`, `gata`, `perro`, or `perra`. “Luna es la gata
de Pedro”, “Pedro tiene un perro que se llama Toby”, and “mi perra se llama Nala”
register a named pet. Species-qualified declarations set favorite food, add a
like, or replace an age from 1 to 40, for example “la gata Luna tiene 3 años”;
such a declaration can also create an ownerless pet record directly.
The species marker is required so a pet is never selected merely because its name
matches a person.

“¿Qué sabes de la gata Luna?”, “¿qué le gusta a la gata Luna?”, “¿cuál es la
comida favorita de la gata Luna?” and “¿qué mascotas tiene Pedro?” retrieve only
matching pet records. Pet names and owners are limited to three words, fact values
to five words, each pet to twenty likes, and the registry to one hundred pets.
`PetMemoryStore` uses a separate Android Keystore key from person and face data.
The unlocked **Memorias** screen combines person and pet records for inspection,
targeted deletion, and erase-all while the encrypted registries remain separate.

## Current limitations

- Recognition is active only while the activity is in the foreground.
- Recognition quality and availability depend on the device's installed on-device
  recognition service.
- Android's utterance-oriented recognizer still times out and restarts during
  silence; it is not a production-quality continuous wake-word detector.
- The current wake-word mechanism is a bootstrap dependency on an Android platform
  service, not the final app-owned local model.
- No language-model or vision-language-model inference is implemented, and there
  is no Android-to-body BLE communication yet. Object detection and face
  embedding are the only app-owned model executions.
- Person-memory language is intentionally bounded to favorite food, likes, age,
  and the three documented query forms. Paraphrases, relationships, birthdays,
  addresses, arbitrary biographies, and fact-level editing are not yet parsed.
- Pet-memory language is limited to named cats and dogs, explicit owners,
  favorite food, likes, ages 1–40, and the documented queries. Other species,
  breeds, medical details, and unqualified names are never resolved as pet
  records.
- Structured person-memory speech routing, encrypted persistence, the Memorias
  screen, pet-memory routing, and offline spoken answers still require
  physical-device validation.
- “¿Qué ves?” is limited to the 80 COCO classes known by YOLO26n. It cannot
  reliably describe activities, relationships, text, or unfamiliar objects.
- The `person` class can be wrong. The alpha face preparer uses Android's
  eye-midpoint face geometry rather than SFace's preferred five-landmark
  alignment, so pose, distance, lighting, occlusion, multiple faces, and similar
  appearances can cause unknown or incorrect results. Conservative thresholds
  reduce but cannot eliminate false matches.
- Face recognition is a toy presentation response, never authentication. It
  cannot authorize body actions, owner controls, or private-memory disclosure.
- YOLO26n must be explicitly downloaded and verified before the command can run;
  SFace must also be downloaded and verified for recognition and enrollment.
- The YOLO26n weights are AGPL-3.0; Kiko is therefore AGPL-3.0-only. A future
  proprietary or commercial deployment requires an applicable Ultralytics
  Enterprise license and a new project-license review.
- The universal debug APK carries ONNX Runtime native libraries for every bundled
  ABI and is materially larger than the former LiteRT build. A future
  distribution build should split APKs or app bundles by ABI.
- Spoken output requires an installed Spanish voice that Android marks as not
  requiring a network connection; otherwise the complete answer remains visible.
- Rear-camera live preview/capture, eye animation timing, and Spanish TTS still
  require physical-device validation.
- Face crop quality, SFace latency/matching, encrypted enrollment, the
  post-detection name flow, and identity deletion still require physical-device
  validation.
- Visual-history rendering, persistence across process restarts, deletion, and
  storage behavior still require physical-device validation. Because every
  successful capture is retained until explicit deletion, storage usage can grow
  without bound.
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
   squint the on-screen eyes, preview and capture one rear-camera frame, run
   YOLO26n on-device, report bounded object detections with an offline Spanish
   voice, retain an inspectable troubleshooting record, and recognize/enroll one
   explicitly confirmed face locally with SFace.
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
9. **Local memory (face, person, and cat/dog portions delivered):** extend the
   shipped encrypted face registry and structured person/pet registries with
   general confirmed facts and observation memories.
10. **Local vision expansion:** benchmark richer app-owned scene and face
    alignment models without sending images or SDK telemetry to a cloud service.
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
- Language-model, vision-language-model, or activity-captioning inference. The
  system speech service may download its own Spanish recognition pack, and the
  model library downloads only artifacts explicitly selected by the user.
