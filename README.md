# Kiko

Kiko is an Android application for a local, embodied AI toy. Its final objective
is to run its intelligence entirely on the Android device, interact in Spanish,
use local camera/sensor/memory tools, and connect to a physical body over USB. It
is a personal, noncommercial project.

Kiko currently listens with Android's on-device speech recognizer and displays
**“escuchando!”** whenever it hears the wake word **“kiko”**. It also provides a
download-only library of local GGUF language models; inference is not implemented
yet.

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
- Offers pinned Gemma 3 1B, Bonsai 1.7B, Qwen3 0.6B, and LFM2.5 350M GGUF
  downloads with progress, cancellation, deletion, and SHA-256 verification.
- Stores model files in Kiko's app-specific external directory so uninstalling the
  app removes them without requesting broad storage access.

The current implementation uses the speech recognizer supplied by the Android
device. A future local-model milestone will replace this platform dependency with
an app-owned wake-word and inference pipeline.

## Run it

Requirements:

- Android Studio with Android SDK 37 installed.
- JDK 17 or newer.
- A physical Android 12+ device with an on-device recognition service.

Open the repository in Android Studio, let Gradle sync, and run the `app`
configuration. Grant microphone access when prompted, then say “kiko”.

If a Spanish model needs to be downloaded, leave the phone online until the system
speech service completes it, then close and reopen Kiko. Audio recognition remains
on-device.

Open **Modelos locales** from the wake-word screen to manage language-model
downloads. Kiko requests network access solely for user-initiated model-file
downloads; it does not send prompts, microphone audio, or inference data anywhere.
Gemma requires accepting Google's Gemma terms on Hugging Face and entering a
read-only Hugging Face token. The token is encrypted with Android Keystore.

The four catalog files require about 2.0 GiB in total. Downloads continue through
Android's system download manager if the model screen closes. Details and pinned
sources are in `docs/MODELS.md`.

From a configured command line:

```sh
./gradlew test
./gradlew assembleDebug
```

## Project map

- `app/src/main/java/com/kiko/app/MainActivity.java` owns the activity lifecycle,
  permission flow, recognizer lifecycle, and screen state.
- `app/src/main/java/com/kiko/app/WakeWordMatcher.java` contains the deterministic,
  independently tested wake-word matcher.
- `app/src/main/java/com/kiko/app/SpeechLanguageSelector.java` selects an installed
  Spanish recognition model without coupling that logic to Android UI state.
- `app/src/main/java/com/kiko/app/ModelLibraryActivity.java` renders the model
  catalog and download controls.
- `app/src/main/java/com/kiko/app/ModelDownloadStore.java` owns system downloads,
  private model storage, progress, cancellation, and integrity verification.
- `docs/MODELS.md` records exact artifacts, licenses, and authentication
  requirements.
- `docs/MODEL_RESEARCH.md` evaluates local tool-calling and vision models for
  sensors and the USB-connected body.
- `docs/COMMANDS.md` defines the Spanish command, memory, face-recognition, and
  toy-safety contract.
- `docs/PRODUCT.md` defines the product goal, current milestone, and roadmap.
- `docs/ARCHITECTURE.md` records the present design and intended architectural
  boundaries.
- `AGENTS.md` is the source of truth for AI maintainers.

## AI maintenance

This repository is maintained exclusively by AI agents. Read `AGENTS.md` before
making changes. Every behavior or architecture change must update the relevant
documentation in the same commit.
