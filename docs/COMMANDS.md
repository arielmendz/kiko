# Spanish command contract

Kiko is a noncommercial physical toy whose spoken interaction is in Spanish. The
initial product does not expose a general automation platform. It recognizes a
small, versioned set of toy behaviors and answers in Spanish.

The complete Mermaid sequence diagrams for these behaviors are in
`docs/FLOWS.md`.

## Interaction contract

A normal interaction begins with the wake word and one Spanish request:

```text
“Kiko” -> “escuchando!” -> petición -> respuesta o acción
```

The app may accept “Kiko, da tres pasos” as one utterance once the app-owned audio
pipeline exists. Commands in other languages are outside the initial acceptance
criteria. Internal tool identifiers may remain stable English identifiers, but
descriptions, training examples, clarifications, confirmations, errors, and
spoken/displayed answers are Spanish.

The required command families are:

| Spanish examples | Internal intent | Result |
| --- | --- | --- |
| “Da tres pasos”, “camina cinco pasos” | `move_steps` | Move exactly the validated number of steps, then stop |
| “Baila”, “haz un baile” | `dance` | Run a bounded native dance routine |
| “¿Qué sabes de los dinosaurios?” | `answer_about` | Answer in Spanish from the selected local model and relevant local memories |
| “¿Qué ves?” | `describe_scene` | Capture one current frame and describe it in Spanish |
| “¿A quién ves?” | `recognize_faces` | Capture one current frame and name only locally enrolled faces |
| “Recuerda esto: mi color favorito es verde” | `remember_fact` | Confirm and store the supplied fact locally |
| “Recuerda lo que ves” | `remember_observation` | Confirm and store the latest textual scene observation, not the image |
| “Recuerda esta cara como Ana” | `enroll_face` | Confirm and locally enroll a face embedding under the supplied name |
| “La comida favorita de Pedro es la pasta” | `set_person_favorite_food` | Immediately store this complete explicit structured fact |
| “A Pedro le gusta el fútbol”, “Pedro tiene 10 años” | `update_person_memory` | Add or replace the bounded person field and show “memoria actualizada” |
| “¿Qué le gusta a Pedro?”, “¿qué sabes de Pedro?”, “¿cuál es la comida favorita de Pedro?” | `query_person_memory` | Answer only from Pedro's encrypted structured record |
| “Luna es la gata de Pedro”, “Pedro tiene un perro que se llama Toby” | `register_pet` | Store a named cat or dog and optional owner in a separate encrypted registry |
| “La gata Luna tiene 3 años”, “a la gata Luna le gusta dormir” | `update_pet_memory` | Add or replace one species-qualified pet field and show “memoria actualizada” |
| “¿Qué sabes de la gata Luna?”, “¿qué mascotas tiene Pedro?” | `query_pet_memory` | Answer only from matching encrypted cat/dog records |

Supporting safety and memory commands are also required:

- “Para”, “detente” and an on-screen stop control invoke a native emergency stop
  without waiting for a language model.
- “Olvida que…”, “olvida a Ana” and “borra todos tus recuerdos” delete the
  corresponding local memory after confirmation.
- “¿Qué recuerdas de mí?” retrieves only memories stored by Kiko and labels them
  as memories rather than general model knowledge.

## Routing rules

Commands use two lanes:

1. A deterministic Spanish grammar handles emergency stop, clearly formed step
   counts, dance, the shipped bounded person/pet-memory updates/queries, and
   explicit memory deletion. A `SpanishNumberParser` converts
   forms such as `3`, `tres`, and common speech-recognition variants into an
   integer.
2. The local `ActionRouter` handles paraphrases, clarification, knowledge,
   vision, face-memory, and fact-memory requests. Its output is still parsed and
   validated as an untrusted proposal.

The deterministic lane wins when both lanes could match. The model must never
reinterpret an emergency stop or increase a requested movement.

## Command-specific rules

### Steps

`move_steps` contains an integer `count`, a command ID, and a deadline. Valid
counts are `1..maxStepsPerCommand`, where the maximum comes from the connected
body's versioned capability profile. Until that profile is known, movement
remains disabled rather than assuming a safe maximum.

Zero, negative, fractional, missing, or out-of-range counts produce a Spanish
clarification or refusal. The body acknowledges each accepted command and reports
completion or failure. A timeout, disconnect, stop request, or invalid telemetry
halts motion.

### Dance

`dance` selects an allowlisted native `routineId`. A dance is a pre-tested body
macro with a fixed maximum duration; the model does not generate joint or motor
sequences. Stop interrupts it immediately.

### Local knowledge

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

`remember_fact` stores content explicitly provided by the user. A
`MemoryCandidateResolver` may also propose the immediately preceding user
statement or tool result from the current command session. Kiko reads back the
exact proposed memory in Spanish and stores it only after confirmation. If the
user says only “recuerda esto” and the current session does not contain one
unambiguous candidate, Kiko asks “¿Qué quieres que recuerde?”.

`remember_observation` may store the latest textual result from `VisionEngine`
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

Every command produces one visible state and, when an offline Spanish voice is
available, the same response through local speech synthesis:

```text
ESCUCHANDO -> PENSANDO -> CONFIRMANDO? -> ACTUANDO? -> RESPONDIENDO -> LISTO
```

Failures use Spanish and identify the recoverable condition: no body, no camera
permission, no face enrolled, unclear count, model unavailable, memory unavailable,
or action stopped. Kiko must not pretend an action completed without a matching
native acknowledgement.
