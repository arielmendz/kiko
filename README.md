# Kiko

Kiko is an Android application for a local, embodied AI. Its final objective is to
run an AI model entirely on the Android device and connect that intelligence to a
physical body over USB.

The first milestone is intentionally small: while the app is open, it listens with
Android's on-device speech recognizer and displays **“escuchando!”** whenever it
hears the wake word **“kiko”**.

## Current scope

- Native Android app written in Java.
- On-device speech recognition only; no network permission is requested.
- Spanish wake-word matching that ignores case, punctuation, and accents.
- Clear UI states for permission, recognition availability, listening, and match.

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
- `docs/PRODUCT.md` defines the product goal, current milestone, and roadmap.
- `docs/ARCHITECTURE.md` records the present design and intended architectural
  boundaries.
- `AGENTS.md` is the source of truth for AI maintainers.

## AI maintenance

This repository is maintained exclusively by AI agents. Read `AGENTS.md` before
making changes. Every behavior or architecture change must update the relevant
documentation in the same commit.
