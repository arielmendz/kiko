# AI maintainer instructions

This repository is maintained exclusively by AI agents. Treat its documentation
as part of the product, not as optional commentary.

## Product invariant

Kiko's final objective is an Android application that:

1. runs its AI model locally on the Android device;
2. continues to function without a cloud inference dependency;
3. behaves as a personal, noncommercial physical toy that interacts in Spanish;
4. remembers explicitly supplied facts and explicitly enrolled faces locally;
5. observes device sensors through typed tools; and
6. connects over Bluetooth Low Energy to a Raspberry Pi body that controls two
   servos through a native safety boundary.

The current milestones are:

1. while the foreground wake-word screen hears “kiko”, it shows “escuchando!”;
2. users can download, verify, inspect, cancel, and delete the documented local
   GGUF model artifacts; and
3. the repository contains a transport-independent Raspberry Pi body safety core
   and simulator, but no Android BLE, BlueZ, GPIO, or physical-servo integration;
   and
4. the wake-word screen shows native googly eyes that blink and look side to side
   while listening; after “kiko”, “¿qué ves?” squints those eyes, shows a live
   rear-camera viewport, captures one still, and
   runs a downloaded, SHA-verified YOLO26n ONNX artifact locally through ONNX
   Runtime, then reports bounded COCO object detections through Spanish screen
   text and an offline-only voice, and saves the image plus result in an
   inspectable, locally erasable troubleshooting history; after a `person`
   detection it runs a downloaded, SHA-verified SFace model locally, names only a
   clear match to an explicitly enrolled encrypted identity, or asks an unknown
   person for a name and enrolls only after unlocked on-screen confirmation; face
   output is never authentication; and
5. after “kiko”, bounded explicit person facts for favorite food, likes, and age
   are stored in an encrypted, inspectable, locally erasable registry; bounded
   person-memory questions return only those stored facts; and
6. no language-model or vision-language-model inference runs yet; local object
   detection and face-embedding inference do.

Do not silently weaken the local-first or BLE-body goals. Temporary platform
dependencies must be identified as such in `README.md`, `docs/PRODUCT.md`, and
`docs/ARCHITECTURE.md`.

## Required workflow for every change

1. Read `README.md`, `docs/PRODUCT.md`, and `docs/ARCHITECTURE.md` before editing.
2. Inspect the existing implementation and tests; do not infer behavior from file
   names alone.
3. Make the smallest coherent change that advances the documented objective.
4. Add or update tests for observable logic whenever practical.
5. Run the most relevant available verification. Never claim a check passed if it
   was skipped or the environment lacked an Android SDK/device.
6. Self-document in the same commit:
   - update `README.md` when setup, usage, capability, or project layout changes;
   - update `docs/PRODUCT.md` when user behavior, scope, constraints, or roadmap
     status changes;
   - update `docs/ARCHITECTURE.md` when components, dependencies, data flow,
     permissions, hardware integration, or technical tradeoffs change;
   - add a short entry to `docs/CHANGELOG.md` under `Unreleased` for every
     user-visible or architectural change.
7. Re-read the diff for contradictions between code, tests, and documentation.

Documentation-only changes should update only the documents they make inaccurate;
they do not need a circular changelog entry unless they change product behavior or
architecture.

## Engineering constraints

- Local-first is a hard requirement. `INTERNET` is authorized only for explicit
  user-initiated model artifact downloads. Do not use it for analytics, cloud
  inference, prompt transport, remote speech, or silent background fetching
  without new explicit human approval and prominent documentation.
- Keep catalog artifacts pinned to immutable upstream revisions and expected
  SHA-256 hashes. Review source, license, filename, size, and runtime compatibility
  before changing any entry.
- Keep wake-word detection, inference, BLE transport, and UI state as separate
  boundaries so each can be replaced and tested independently.
- Treat model output as untrusted. Models may propose typed tool calls, but native
  code must own Android permissions, schema validation, physical limits,
  confirmations, deadlines, and emergency-stop behavior. Never connect free-form
  model output directly to a sensor, camera, location, BLE API, or servo driver.
- The Raspberry Pi owns servo calibration, bounded native trajectories, the
  motion watchdog, connection-loss handling, and the final physical stop. Android
  sends semantic commands only; it never sends model-generated angles or PWM.
- Request only permissions needed by implemented behavior.
- Never commit secrets, signing keys, generated build output, local SDK paths, or
  model binaries without an explicit artifact strategy.
- Prefer deterministic unit tests for parsing, matching, protocols, and state
  transitions. Hardware behavior requires documented manual verification until a
  repeatable device test exists.
- Preserve Spanish user-facing copy unless a product change explicitly introduces
  localization.
- Durable fact or face memory must be explicit, inspectable, locally erasable, and
  encrypted at rest. The explicit “¿qué ves?” flow retains every completed frame
  in private, inspectable, locally erasable troubleshooting history; other camera
  flows must not retain frames by default. Never use a language model to guess
  identity.
- A complete bounded declaration such as “la comida favorita de Pedro es la
  pasta”, “a Pedro le gusta el fútbol”, or “Pedro tiene 10 años” is itself an
  explicit fact-memory instruction and may be stored immediately with a visible
  “memoria actualizada” state. Unsupported or ambiguous statements must not be
  persisted. Retrieval must return only stored structured facts and admit when a
  requested fact is unknown.
- A confirmed name on an “¿qué ves?” photo is explicit face enrollment. It
  requires an unlocked on-screen confirmation, stores only a bounded encrypted
  embedding/name/source link in the face registry, and may be reused only for
  local toy identification. Legacy name-only photo tags must not be silently
  converted into biometric enrollment.
- Face matches are toy responses, never authentication. Enrollment, identity
  deletion, and erase-all require an unlocked on-screen owner control; face output
  must not authorize actions or disclose private memory.
- Treat the declared noncommercial scope as a license constraint. Any commercial
  scope change requires a new review of models, datasets, dependencies, privacy,
  and physical-safety assumptions.
- Keep `CLAUDE.md` as a pointer to this file so Claude-based maintainers follow the
  same source of truth.

## Definition of done

A change is done only when implementation, tests, and relevant documentation agree;
the performed checks and any unverified device behavior are reported honestly.
