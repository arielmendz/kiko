# Spanish command contract

Kiko is a noncommercial physical toy whose spoken interaction is in Spanish. The
initial product does not expose a general automation platform. It recognizes a
small, versioned set of toy behaviors and answers in Spanish.

This is a precise product contract, not the easiest first introduction. New
owners should start with the [plain-language owner guide](USER_GUIDE.md).

Status labels in this document mean:

- **Delivered** — available in the current Android app;
- **Partial** — available only through the specific flow described; and
- **Planned** — a target contract, not an available command.

The complete Mermaid sequence diagrams for these behaviors are in
`docs/FLOWS.md`.

## Interaction contract

A normal interaction begins with the wake word and one Spanish request:

```text
“Kiko” -> “escuchando!” -> petición -> respuesta o acción
```

The current foreground recognizer accepts “Kiko, da tres pasos” in one utterance
or “Kiko” followed by the command within ten seconds. Commands in other languages
are outside the initial acceptance criteria. Internal tool identifiers may remain stable English identifiers, but
descriptions, training examples, clarifications, confirmations, errors, and
spoken/displayed answers are Spanish.

The command families are:

| Status | Spanish examples | Internal intent | Current or target result |
| --- | --- | --- | --- |
| **Delivered as simulation** | “Da tres pasos”, “camina cinco pasos” | `move_steps` | Complete the validated protocol loop; no physical movement |
| **Delivered as simulation** | “Baila”, “haz un baile” | `dance` | Complete the allowlisted `seal_wiggle` protocol loop; no physical movement |
| **Delivered** | “¿Qué ves?” | `describe_scene` | Capture one frame and report bounded local object/face results in Spanish |
| **Delivered** | “La comida favorita de Pedro es la pasta” | `set_person_favorite_food` | Store this complete explicit structured fact |
| **Delivered** | “A Pedro le gusta el fútbol”, “Pedro tiene 10 años” | `update_person_memory` | Add or replace the bounded person field and show “memoria actualizada” |
| **Delivered** | “¿Qué le gusta a Pedro?”, “¿qué sabes de Pedro?”, “¿cuál es la comida favorita de Pedro?” | `query_person_memory` | Answer only from Pedro's encrypted structured record |
| **Delivered** | “Luna es la gata de Pedro”, “Pedro tiene un perro que se llama Toby” | `register_pet` | Store a named cat or dog and optional owner in a separate encrypted registry |
| **Delivered** | “La gata Luna tiene 3 años”, “a la gata Luna le gusta dormir” | `update_pet_memory` | Add or replace one species-qualified pet field and show “memoria actualizada” |
| **Delivered** | “¿Qué sabes de la gata Luna?”, “¿qué mascotas tiene Pedro?” | `query_pet_memory` | Answer only from matching encrypted cat/dog records |
| **Partial** | Name prompt after an unknown face in “¿qué ves?” | face enrollment | Save only after an unlocked on-screen confirmation |
| **Planned** | “¿Qué sabes de los dinosaurios?” | `answer_about` | Answer in Spanish from a future selected local language model |
| **Planned** | “¿A quién ves?” | `recognize_faces` | Run a standalone enrolled-face flow |
| **Planned** | “Recuerda esto: mi color favorito es verde” | `remember_fact` | Confirm and store a future general fact record |
| **Planned** | “Recuerda lo que ves” | `remember_observation` | Confirm and store the latest textual scene observation |
| **Planned** | “Recuerda esta cara como Ana” | `enroll_face` | Run a standalone explicit face-enrollment flow |

Supporting commands have different delivery status:

- **Delivered for the simulated body:** “Para”, “detente” and the on-screen stop
  control invoke a native stop without waiting for a language model.
- **Delivered on screen:** unlocked owner controls delete individual or complete
  person, pet, face, and visual-history records within their documented scope.
- **Planned spoken commands:** “Olvida que…”, “olvida a Ana”, “borra todos tus
  recuerdos”, and the general “¿Qué recuerdas de mí?” flow.

## Routing rules

The target architecture uses two lanes. Only the deterministic lane is delivered:

1. A deterministic Spanish grammar handles emergency stop, clearly formed step
   counts, dance, and the shipped bounded person/pet-memory updates and queries.
   A `SpanishNumberParser` converts forms such as `3` and `tres` into an integer.
2. **Planned:** a local `ActionRouter` handles paraphrases, clarification,
   knowledge, vision, face-memory, and general fact-memory requests. It does not
   exist in the current app. Its future output must still be parsed and validated
   as an untrusted proposal.

The deterministic lane wins when both lanes could match. The model must never
reinterpret an emergency stop or increase a requested movement.

## Command-specific rules

### Steps

`move_steps` contains an integer `count`, a command ID, and a deadline. Valid
counts are `1..maxStepsPerCommand`, where the maximum comes from the connected
body's versioned capability profile. The shipped Android loopback negotiates the
same six-step maximum from its protocol-level peer; it produces no movement. A
future physical transport remains disabled until it receives a real versioned
profile.

Zero, negative, fractional, missing, or out-of-range counts produce a Spanish
clarification or refusal. The body acknowledges each accepted command and reports
completion or failure. A timeout, disconnect, stop request, or invalid telemetry
halts motion.

The delivered loopback recognizes exact “da/dame/camina/avanza N paso(s)” forms,
assigns a native command ID and ten-second deadline, serializes `MOVE_STEPS`,
sends protocol heartbeats, and reports “Simulación completada” only after a valid
`COMPLETED` event. Missing, fractional, or out-of-range counts never dispatch.

### Dance

`dance` selects an allowlisted native `routineId`. A dance is a pre-tested body
macro with a fixed maximum duration; the model does not generate joint or motor
sequences. Stop interrupts it immediately.

The delivered Android loopback maps “baila”, “haz un baile”, and “haz el baile”
only to `seal_wiggle`; the encoded command completes after the peer returns the
matching 2.4-second simulated plan's `COMPLETED` event. It does not contain or
generate servo angles.

### Local knowledge

**Status: planned.** No language-model inference or `ActionRouter` runs today.

`answer_about` receives a topic and retrieves relevant local memories before
asking the conversational model. It does not search the internet. The response
distinguishes saved memory (“Recuerdo que…”) from general model knowledge and
admits uncertainty instead of inventing current facts.

### Scene description

`describe_scene` activates the camera only after the explicit command, captures a
bounded rear-camera still after showing a short live preview, passes it to the
current `LocalVisionEngine`, and releases the camera. Kiko's eyes squint from
command acceptance through completion. Every completed capture and its exact
Spanish response are initially retained in Kiko's private visual troubleshooting
history until the user deletes that record, erases all history, enables the
separate sleep policy that deletes conclusively unrecognized and still-unnamed
photos, or uninstalls the app. Named
photos are grouped through an encrypted person/pet association. Because YOLO
cannot identify individual pets, a pet association requires an unlocked explicit
choice from stored cat/dog memory.
The response describes only the supported visible properties in Spanish. Person
identity comes only from the separate local enrolled-face matcher.

The delivered first iteration recognizes “¿qué ves?” deterministically in the
same utterance as “Kiko” or during a ten-second post-wake window. It uses the rear
camera and the pinned YOLO26n model to report up to three COCO object types. If
any accepted detection has the `person` class, Kiko runs the pinned SFace
embedding model on one locally prepared face. A clear enrolled match says “Veo a
<nombre>.” An unknown usable face says “Veo una persona, no la conozco, ¿quién
es?” and opens a bounded local name-listening window. An unlocked on-screen
**Guardar** confirmation encrypts the supplied name and embedding and links the
enrollment to that history photo. Saying “cancelar” or declining leaves the photo
unnamed and stores no identity.

The `person` class is only the gate for the dedicated face path and is not itself
an identity result. Missing SFace, no usable face, multiple faces, a score below
`0.50`, or less than `0.08` separation from a differently named runner-up never
returns the nearest name. Free-form scene and activity captions remain future
work.

The answer is always displayed. It is spoken only through an installed Spanish
TTS voice that Android marks as not requiring a network connection. The current
low-pitch, reduced-rate profile is intentionally simple and robotic.

### Face identity

The delivered “¿qué ves?” person branch uses a local face preparer and SFace
embedding matcher. The separate `recognize_faces` spoken intent remains future
work. Neither path asks a language or vision-language model to guess identity or
uses an external face-search service.

Enrollment is explicit: the user supplies a name, Kiko confirms it, checks that
one usable face is present, and requires an on-screen owner confirmation while
the phone is unlocked before storing an encrypted embedding. The “¿qué ves?”
history photo is the inspectable source for this first enrollment path and is
protected from unrecognized-photo cleanup by its encrypted identity link; other future
face flows discard source photos by default. Recognition below
the configured threshold or ambiguity margin returns unknown rather than the
nearest name. Names and embeddings can be inspected through their source records
and deleted locally; targeted deletion and erase-all use the same unlocked owner
control.

Face matching is a friendly toy memory, not biometric authentication or proof of
identity. Its result never authorizes movement, reveals private memory, or unlocks
an owner-only operation, and the UI must make clear that photos or lookalikes can
produce mistakes.

### Durable memory

The delivered person-memory subset treats these complete declarations as the
explicit storage command: favorite food, one general like, or numeric age. It
does not require a second spoken confirmation because the bounded subject,
predicate, and value are all present in the final hypothesis. Kiko stores only
after the encrypted commit succeeds and then shows **memoria actualizada**. A
partial speech hypothesis never mutates memory.

The shipped queries retrieve only the requested canonical-name record. “¿Qué le
gusta a Pedro?” combines favorite food and deduplicated likes; “¿qué sabes de
Pedro?” combines every populated field; “¿cuál es la comida favorita de Pedro?”
returns only that field. Missing people or fields produce an explicit unknown
answer. Face recognition cannot trigger these queries or disclose the record.

The delivered pet-memory subset accepts only `gato`, `gata`, `perro`, and
`perra`. Registration supports “Luna es la gata de Pedro”, “Pedro tiene un perro
que se llama Toby”, “mi perra se llama Nala”, and the equivalent ownerless
“Luna es una gata”. Favorite-food, like, age, and individual-pet queries must
include a matching species-qualified subject such as “la gata Luna”; this avoids
resolving a pet solely from a name that may also belong to a person. A structured
pet fact can create an ownerless record without a separate registration. Ages are
limited to 1–40. “¿Qué mascotas tiene Pedro?” retrieves all pets explicitly
linked to that owner. Unsupported species and unqualified pet facts are not
stored as pet records.

The following general-memory design is **planned, not shipped**. `remember_fact`
would store content explicitly provided by the user. A
`MemoryCandidateResolver` could also propose the immediately preceding user
statement or tool result from the current command session. Kiko would read back
the exact proposed memory in Spanish and store it only after confirmation. If the
user said only “recuerda esto” and the current session did not contain one
unambiguous candidate, Kiko would ask “¿Qué quieres que recuerde?”.

`remember_observation` could store the latest textual result from `VisionEngine`
after confirmation. It does not create a camera-frame memory record; the
underlying `describe_scene` capture may already exist separately in visual
troubleshooting history. Memory resolution never searches arbitrary past
conversation and never silently chooses between multiple candidates.

The first shipped memory records are deliberately structured:

```text
PersonMemoryRecord {
  canonicalName,
  displayName,
  favoriteFood?,
  likes[0..20],
  age?,
  updatedAt
}

PetMemoryRecord {
  canonicalName,
  displayName,
  kind: GATO | GATA | PERRO | PERRA,
  canonicalOwnerName?,
  displayOwnerName?,
  favoriteFood?,
  likes[0..20],
  age?,
  updatedAt
}
```

The person registry is limited to 100 people and the pet registry to 100 pets;
each is versioned and AES-GCM encrypted under its own Android Keystore key.
General future `MemoryItem` storage should use structured local full-text search
before adding another embedding model.

Conversation history is short-lived session state and is not automatically
promoted to durable memory. Facts, textual observations, names, face embeddings,
and any later semantic indexes remain on the device, are encrypted using keys
protected by Android Keystore, and are removed on explicit deletion or app
uninstall.

## Response states

This is the target shared lifecycle. The current app implements the relevant
visible subset for wake-word, scene, structured-memory, and simulated-body flows.
When an offline Spanish voice is available, current results are also spoken:

```text
ESCUCHANDO -> PENSANDO -> CONFIRMANDO? -> ACTUANDO? -> RESPONDIENDO -> LISTO
```

Failures use Spanish and identify the recoverable condition: no body, no camera
permission, no face enrolled, unclear count, model unavailable, memory unavailable,
or action stopped. Kiko must not pretend an action completed without a matching
native acknowledgement.
