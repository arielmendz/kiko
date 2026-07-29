# Product

## Vision

Kiko is the mind of a physical companion: an Android application that runs a local
AI model and connects to a body through USB. The completed product must not depend
on cloud inference for its core intelligence.

## Current milestone: wake-word screen

While the app is visible:

1. it requests microphone permission;
2. it starts Android's on-device Spanish speech recognizer;
3. it listens for “kiko”; and
4. it displays “escuchando!” when a recognition hypothesis contains that word.

Matching is case-insensitive, accent-insensitive, and token-based, so punctuation
around the word is accepted while substrings such as “kikongo” are rejected.

## Current limitations

- Recognition is active only while the activity is in the foreground.
- Recognition quality and availability depend on the device's installed on-device
  recognition service.
- The current wake-word mechanism is a bootstrap dependency on an Android platform
  service, not the final app-owned local model.
- No AI model inference or USB communication is implemented yet.
- Microphone behavior still needs validation on a physical device.

## Roadmap

1. **Wake-word screen (current):** recognize “kiko” locally and show
   “escuchando!”.
2. **App-owned audio pipeline:** replace the platform recognizer with deterministic
   streaming audio capture and a bundled wake-word model.
3. **Local conversational model:** package and run a quantized model on supported
   Android hardware, with explicit memory and thermal budgets.
4. **USB body link:** define a versioned command/telemetry protocol and communicate
   through Android USB host APIs.
5. **Embodied loop:** connect wake word, local inference, safety policy, physical
   actions, telemetry, and recovery into an offline-first experience.

## Non-goals for the current milestone

- Background or always-on listening.
- Cloud speech recognition or cloud AI.
- USB device discovery or control.
- Model download, inference, or conversation.
