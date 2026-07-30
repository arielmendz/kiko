# Product

## Vision

Kiko is the mind of a physical companion: an Android application that runs a local
AI model and connects to a body through USB. The completed product must not depend
on cloud inference for its core intelligence.

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

## Current milestone: downloadable local models

From **Modelos locales**, the user can:

1. inspect Gemma 3 1B, Bonsai 1.7B, Qwen3 0.6B, and LFM2.5 350M;
2. see each artifact's parameter count, quantization, size, and license;
3. start a model download and leave the screen while Android continues it;
4. see progress, cancel an active download, retry a failure, or delete a model;
5. use the system browser to inspect the upstream source and license; and
6. receive a usable `.gguf` only after exact byte-size and SHA-256 verification.

Gemma is gated by Google on Hugging Face. The user must accept the Gemma license
externally and provide a Hugging Face read token. Kiko encrypts that token with
Android Keystore and never logs it.

This phase downloads models but deliberately does not load or execute them.

## Current limitations

- Recognition is active only while the activity is in the foreground.
- Recognition quality and availability depend on the device's installed on-device
  recognition service.
- Android's utterance-oriented recognizer still times out and restarts during
  silence; it is not a production-quality continuous wake-word detector.
- The current wake-word mechanism is a bootstrap dependency on an Android platform
  service, not the final app-owned local model.
- No language-model inference or USB communication is implemented yet.
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
2. **Model library (current):** securely download and verify pinned local model
   artifacts without executing them.
3. **Embodied tool contract:** define typed read-only sensor tools and
   state-changing body tools, plus native validation, confirmation, deadlines,
   and emergency-stop behavior.
4. **App-owned audio pipeline:** replace the platform recognizer with deterministic
   streaming audio capture and a bundled wake-word model.
5. **Local action router:** benchmark and run a Kiko-specific FunctionGemma 270M
   fine-tune and larger tool-capable baselines on supported Android hardware.
6. **USB body link:** define a versioned command/telemetry protocol and communicate
   through Android USB host APIs.
7. **Optional local vision:** expose camera observations through a replaceable
   vision engine without sending images to a cloud service.
8. **Embodied loop:** connect wake word, local inference, safety policy, physical
   actions, telemetry, and recovery into an offline-first experience.

The current model recommendation and alternatives are recorded in
`docs/MODEL_RESEARCH.md`. It is a benchmark hypothesis rather than a shipped
default.

## Non-goals for the current milestone

- Background or always-on listening.
- Cloud speech recognition or cloud AI.
- USB device discovery or control.
- Language-model inference or conversation. The system speech service may download
  its own Spanish recognition pack, and the model library downloads only artifacts
  explicitly selected by the user.
