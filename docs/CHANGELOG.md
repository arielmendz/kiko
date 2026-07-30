# Changelog

## Unreleased

- Bootstrap the Android application.
- Add local on-device wake-word recognition for “kiko”.
- Show “escuchando!” when the wake word is detected.
- Document the local AI and USB-connected body product objective.
- Establish mandatory self-documentation rules for AI maintainers.
- Diagnose recognition on screen by showing hypotheses and actionable error states.
- Prefer `es-US`, verify/download on-device Spanish support, and stop retrying
  non-recoverable recognition failures.
- Accept `Quico` and `Quiko` as common transcriptions of the spoken “kiko” command.
- Add a download-only local model library for Gemma 3 1B, Bonsai 1.7B, Qwen3
  0.6B, and LFM2.5 350M.
- Add persistent system-managed downloads with progress, cancellation, retry,
  deletion, private app storage, and SHA-256 verification.
- Add encrypted Hugging Face token storage for license-gated Gemma downloads.
- Permit network access only for explicit model artifact downloads; inference
  remains unimplemented.
- Document the embodied tool architecture, physical-action safety boundary, model
  comparison, and recommendation to benchmark a Kiko-specific FunctionGemma 270M
  action router.
- Define Kiko as a Spanish-speaking noncommercial toy with bounded steps and dance
  commands, local knowledge and scene answers, explicit fact memory, and
  explicitly enrolled local face recognition.
- Add Mermaid component, lifecycle, command, memory, perception, body-control, and
  failure-containment diagrams for the intended architecture.
- Select BLE instead of USB for the body link and bootstrap monorepo boundaries
  for the shared GATT protocol, Raspberry Pi safety service, two-servo simulator,
  hardware documentation, and integration tests.
- Add the first local “¿qué ves?” loop: a bounded post-wake command, explicit
  one-shot front-camera permission/capture, downloaded EfficientDet-Lite0
  inference through LiteRT, Spanish object descriptions, and offline-only robotic
  TTS.
- Pin and verify the versioned EfficientDet-Lite0 artifact in the model library;
  discard every scene frame after inference and add no face enrollment, identity,
  camera persistence, cloud vision, heuristic fallback, or SDK analytics.
- Replace EfficientDet-Lite0/LiteRT with the official pinned YOLO26n ONNX model
  and ONNX Runtime Android, including aspect-preserving letterboxing, validated
  end-to-end output parsing, immutable release-asset download headers, and
  deterministic parser tests.
- License Kiko under AGPL-3.0-only to comply with the YOLO26n model license;
  proprietary or commercial use requires a separate Ultralytics license review.
- Add a private visual troubleshooting history that saves every completed
  “¿qué ves?” image with Kiko's exact result, displays records newest first, and
  supports individual deletion and erase-all without adding network or storage
  permissions.
- Add native googly eyes that blink and look side to side while listening, squint
  during “¿qué ves?”, and pair that scene request with a short live rear-camera
  viewport before the saved one-shot capture.
- When YOLO detects `person`, ask who it is, listen locally for a bounded name,
  require unlocked on-screen confirmation, and attach the confirmed name only to
  that erasable visual-history photo without enabling face recognition.
