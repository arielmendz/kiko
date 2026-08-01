# Embodied model research

Research date: 2026-07-30.

> This is a dated research snapshot, not a list of shipped models or commands.
> Kiko currently performs no language-model inference. See
> [Product](PRODUCT.md) for current behavior and [Local model catalog](MODELS.md)
> for the exact artifacts available in the app.

## Decision

Kiko should treat tool selection and physical execution as separate systems. The
recommended model for the eventual action-routing role is a **Kiko-specific,
fine-tuned FunctionGemma 270M**, executed locally with LiteRT-LM. It is small
enough for responsive CPU inference and was designed specifically to translate
natural-language requests into structured function calls.

The stock FunctionGemma checkpoint is not approved to control Kiko. Google
explicitly positions it as a base for task-specific fine-tuning, and the published
Mobile Actions fine-tune does not contain Kiko's body protocol, sensor tools,
Spanish command distribution, or safety refusals.

For an earlier single-model prototype that must both converse and call tools,
**LFM2.5 1.2B Instruct** is the strongest practical baseline found in this
research. It supports Spanish, function calling, GGUF and ONNX distributions, and
is designed for sub-1 GB edge inference. Its LFM license is compatible with the
currently declared noncommercial project, but must be reviewed again before any
commercial distribution. **Granite 4.0 H 1B** is the preferred Apache-2.0
alternative.

This is a benchmark hypothesis, not a permanent brand commitment. No model may
become Kiko's default until it passes the Kiko tool suite on supported phones.

## Why FunctionGemma leads

Google's FunctionGemma model and mobile-action recipe closely match Kiko's core
problem:

- the model has 270 million parameters and is trained specifically for function
  calling;
- the official recipe maps offline voice or text requests to Android actions;
- the fine-tuned Mobile Actions example improved from 58% to 85% on Google's task
  evaluation;
- Google's published dynamic-int8 build is 288 MB and used about 551 MB peak RSS,
  with a 0.3 second time to first token on a Samsung S25 Ultra CPU; and
- LiteRT-LM has a stable Android Kotlin API, tool-use support, and CPU/GPU/NPU
  backends.

Those performance measurements are not predictions for Kiko's Redmi Note 10 Pro.
That phone uses a Snapdragon 732G and 6 or 8 GB of RAM, so latency, sustained
temperature, and battery use must be measured on the actual device.

FunctionGemma is text-only and is not intended to be a general dialogue model.
Kiko can initially use it as a narrow action router, then either hand non-action
conversation to a replaceable local conversational model or use a single larger
model on devices that can afford it.

## Candidate comparison

| Candidate | Best role | Strengths | Blocking concern |
| --- | --- | --- | --- |
| FunctionGemma 270M, Kiko fine-tune | Default action router | Purpose-built function calling; official offline Android path; very small | Must be fine-tuned and evaluated for Kiko; Gemma terms; Spanish tool accuracy is not established by the stock checkpoint |
| LFM2.5 1.2B Instruct | Single-model prototype | Spanish; general dialogue plus tool calls; GGUF/ONNX; publisher reports under 1 GB memory | Larger and less deterministic than a specialized router; license must be re-reviewed if scope becomes commercial |
| LFM2 1.2B Tool | Tool-focused GGUF baseline | Purpose-built non-thinking tool model; Spanish; single- and multi-turn; llama.cpp distribution | Uses the older LFM2 backbone; public card does not expose enough reproducible benchmark numbers |
| Granite 4.0 H 1B | Permissive single-model alternative | Apache-2.0; Spanish; explicit tool schema; official GGUF | Q4_K_M is about 901 MB; lower published tool score than its larger competitors |
| Granite 4.0 H 350M | Permissive tiny baseline | Apache-2.0; Spanish; function calling; Q4_K_M about 223 MB | Published BFCL v3 score is materially below the 1B variant; likely needs Kiko fine-tuning |
| Qwen3 0.6B / 1.7B | General multilingual baseline | Apache-2.0; Hermes-style tool template; broad runtime support | Generalist rather than action-specialized; Qwen3 1.7B is weak on BFCL v4 agentic and multi-turn sections |
| Hammer 2.1 0.5B / 1.5B | Noncommercial research baseline | Tool-specialized; multi-step and multi-turn; integrated with Google AI Edge | Eligible for this noncommercial project, but requires attribution and would block a later commercial scope without relicensing |
| Current Gemma 3 1B, Bonsai 1.7B, LFM2.5 350M | Conversation/compatibility experiments | Already present in the download catalog | They were not selected or tuned as Kiko's physical-action router |

The benchmark versions and task distributions differ, so scores from separate
model cards must not be treated as a single ranking. Kiko's own Spanish,
hardware-specific evaluation is the deciding test.

## Camera and sensor strategy

Accelerometers, gyroscopes, GPS, battery state, and body telemetry are numeric or
structured signals. Native Android code should summarize them deterministically;
an LLM should not consume raw high-frequency streams.

The delivered bounded object-detection baseline is **YOLO26n**, using the
official FP32 ONNX artifact through ONNX Runtime Android. It stays entirely local,
recognizes 80 COCO classes, and does not perform free-form captioning, OCR,
activity understanding, or identity. Its AGPL-3.0 license makes Kiko
AGPL-3.0-only; proprietary or commercial scope requires a new review and an
applicable Ultralytics Enterprise license.

The camera is different because richer scene understanding can require a
vision-language model. The recommended low-resource richer-vision candidate is
**LFM2.5-VL-450M**: it supports Spanish vision prompts, object localization, a
roughly 229 MB Q4_K_M GGUF, and llama.cpp. Its function calling is documented for
text-only input, so Kiko should use it as a separate `VisionEngine` that returns a
compact observation to the action router. Gemma 3n E2B is a stronger multimodal
research candidate for newer phones, but its published 4-bit LiteRT-LM package is
roughly 3 GB and is too heavy to make the Redmi Note 10 Pro the baseline target.

Example flow:

```text
voice/text
  -> ActionRouter
  -> typed tool request
  -> ToolCallParser
  -> ActionPolicy
  -> ToolRegistry
       -> Android sensor adapters
       -> VisionEngine -> camera adapter
       -> BodyBleTransport
  -> structured tool result
  -> ActionRouter
```

The model never receives Android permissions and never writes directly to a BLE
characteristic or servo driver. Native adapters own permissions, sampling,
timeouts, and lifecycle.

## Physical-action safety contract

Tool output is untrusted input even when it came from a local model. Before an
action reaches the body:

1. parse it against a closed schema and reject unknown fields or tools;
2. distinguish read-only observations from state-changing actions;
3. clamp speed, force, angle, duration, and repetition to deterministic limits;
4. require an explicit confirmation for actions classified as hazardous;
5. attach command IDs, deadlines, and idempotency behavior;
6. provide a native emergency stop that bypasses the model;
7. stop motion on timeout, missed BLE heartbeat, disconnect, app lifecycle loss,
   or invalid
   telemetry; and
8. retain a local, privacy-preserving audit record suitable for debugging.

The model may propose a sequence, but `ActionPolicy` decides whether each step is
allowed. Sensor observations must include timestamp, units, accuracy, and
availability so stale or absent data is not mistaken for current truth.

## Kiko evaluation gate

Create a versioned, Spanish-first suite before fine-tuning. English commands are
outside the initial acceptance criteria. The suite should contain:

- direct, indirect, ambiguous, and incomplete commands;
- correct no-tool responses and irrelevant-tool traps;
- permission denied, sensor unavailable, stale GPS, camera failure, and BLE
  disconnect or missed-heartbeat cases;
- boundary values and malicious attempts to exceed motion limits;
- multi-turn clarification and tool-result handling;
- paraphrases from actual on-device speech recognition; and
- recorded expected tool name, exact typed arguments, confirmation requirement,
  and final response.

It must cover every command in `docs/COMMANDS.md`, including Spanish number words,
face unknown/enrollment/deletion, fact clarification and forgetting, offline
knowledge uncertainty, and deterministic emergency stop.

Measure schema validity, correct tool selection, exact arguments, unsafe proposal
rate, false tool activation, multi-turn completion, latency, peak RSS, battery
use, and thermal throttling. Run at temperature zero or greedy decoding for action
routing. Safety acceptance is measured after `ActionPolicy`; model accuracy alone
can never authorize physical motion.

The first runtime spike should compare:

1. stock and Kiko-tuned FunctionGemma 270M through LiteRT-LM;
2. LFM2.5 1.2B Instruct or LFM2 1.2B Tool through llama.cpp; and
3. Granite 4.0 H 350M and 1B through llama.cpp.

Do not add all candidates to the user-facing catalog. Add only artifacts whose
license, format, parser, runtime, and on-device results are known.

## Primary sources

- [FunctionGemma model card](https://ai.google.dev/gemma/docs/functiongemma/model_card)
- [FunctionGemma mobile-actions fine-tuning guide](https://ai.google.dev/gemma/docs/mobile-actions)
- [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)
- [LFM2 1.2B Tool model card](https://huggingface.co/LiquidAI/LFM2-1.2B-Tool)
- [LFM2.5 1.2B Instruct model card](https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct)
- [Granite 4.0 H 350M model card](https://huggingface.co/ibm-granite/granite-4.0-h-350m)
- [Granite 4.0 H 1B model card](https://huggingface.co/ibm-granite/granite-4.0-h-1b)
- [Qwen function-calling guide](https://qwen.readthedocs.io/en/stable/framework/function_call.html)
- [Hammer 2.1 0.5B model card](https://huggingface.co/MadeAgents/Hammer2.1-0.5b)
- [Berkeley Function Calling Leaderboard v4](https://gorilla.cs.berkeley.edu/leaderboard)
- [LFM2.5-VL-450M model card](https://huggingface.co/LiquidAI/LFM2.5-VL-450M)
- [Gemma 3n overview](https://ai.google.dev/gemma/docs/gemma-3n)
- [Ultralytics YOLO26 documentation](https://docs.ultralytics.com/models/yolo26/)
- [Ultralytics licensing](https://www.ultralytics.com/license)
- [Android sensor framework](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview)
- [Android Bluetooth Low Energy overview](https://developer.android.com/develop/connectivity/bluetooth/ble/ble-overview)
