# AI maintainer instructions

This repository is maintained exclusively by AI agents. Treat its documentation
as part of the product, not as optional commentary.

## Product invariant

Kiko's final objective is an Android application that:

1. runs its AI model locally on the Android device;
2. continues to function without a cloud inference dependency; and
3. connects to and controls a physical body over USB.

The current milestone is narrower: while the foreground app hears the wake word
“kiko”, the screen must show “escuchando!”.

Do not silently weaken the local-first or USB-body goals. Temporary platform
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

- Local-first is a hard requirement. Do not add network permissions, analytics,
  cloud inference, or remote speech/model services without explicit human
  approval and prominent documentation.
- Keep wake-word detection, inference, USB transport, and UI state as separate
  boundaries so each can be replaced and tested independently.
- Request only permissions needed by implemented behavior.
- Never commit secrets, signing keys, generated build output, local SDK paths, or
  model binaries without an explicit artifact strategy.
- Prefer deterministic unit tests for parsing, matching, protocols, and state
  transitions. Hardware behavior requires documented manual verification until a
  repeatable device test exists.
- Preserve Spanish user-facing copy unless a product change explicitly introduces
  localization.
- Keep `CLAUDE.md` as a pointer to this file so Claude-based maintainers follow the
  same source of truth.

## Definition of done

A change is done only when implementation, tests, and relevant documentation agree;
the performed checks and any unverified device behavior are reported honestly.
