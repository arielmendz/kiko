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
