# Runtime flows

These Mermaid diagrams specify how the planned Spanish toy behaviors cross the
architecture boundaries defined in `docs/ARCHITECTURE.md`. The scene flow and the
hardware-free step/dance/stop branch now describe delivered implementations;
physical BLE motion remains a target design.

Each section is marked **Delivered**, **Mixed**, or **Planned**. “Mixed” diagrams
show a shipped branch and its future replacement together. For a nontechnical
walkthrough, start with the [owner guide](USER_GUIDE.md).

## Command lifecycle

**Status: planned shared lifecycle.** Current flows implement their relevant
subsets, but there is no general `SpanishCommandSession` or language-model router.

```mermaid
stateDiagram-v2
    [*] --> Idle
    state "LISTO" as Idle
    state "ESCUCHANDO" as Listening
    state "PENSANDO" as Thinking
    state "CONFIRMANDO" as Confirming
    state "ACTUANDO" as Acting
    state "RESPONDIENDO" as Responding
    state "ERROR RECUPERABLE" as RecoverableError

    Idle --> Listening: oye “kiko”
    Listening --> Thinking: recibe petición en español
    Listening --> Idle: silencio o cancelación
    Thinking --> Confirming: memoria o acción sensible
    Thinking --> Acting: herramienta validada
    Thinking --> Responding: conocimiento o aclaración
    Confirming --> Acting: confirma acción
    Confirming --> Responding: confirma memoria
    Confirming --> Idle: rechaza
    Acting --> Responding: acuse nativo
    Acting --> RecoverableError: fallo, desconexión o plazo
    RecoverableError --> Responding: explica el problema
    Responding --> Idle: termina la respuesta

    Listening --> Idle: “para”
    Thinking --> Idle: “para”
    Confirming --> Idle: “para”
    Acting --> Idle: STOP nativo
    Responding --> Idle: “para”
```

In the current simulated-body flow, “para,” “detente,” and the stop button issue
`STOP` without model inference. A future shared `EmergencyStopController` owns
this rule across every tool and cancels pending model, camera, and memory work.

## Routing a Spanish command

**Status: planned.** The deterministic parsers exist; `SpanishCommandSession`,
`ActionRouter`, the general `ActionPolicy`, and language-model routing do not.

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Audio as Audio local
    participant Sesion as SpanishCommandSession
    participant Parser as DeterministicCommandParser
    participant Router as ActionRouter local
    participant Policy as ActionPolicy
    participant Tools as ToolRegistry
    participant Reply as Respuesta española

    Persona->>Audio: “Kiko”
    Audio->>Sesion: wake word
    Sesion-->>Persona: “escuchando!”
    Persona->>Sesion: petición en español
    Sesion->>Parser: texto reconocido

    alt Orden determinista
        Parser-->>Sesion: llamada tipada
    else Paráfrasis o petición semántica
        Parser-->>Sesion: sin coincidencia
        Sesion->>Router: texto + herramientas disponibles
        Router-->>Sesion: propuesta de herramienta
    end

    Sesion->>Policy: llamada tipada no confiable
    alt Válida y permitida
        Policy->>Tools: ejecutar
        Tools-->>Reply: resultado estructurado
    else Ambigua, inválida o no disponible
        Policy-->>Reply: aclaración o rechazo
    end
    Reply-->>Persona: pantalla + voz local opcional
```

## “Da tres pasos” and “baila”

**Status: mixed.** The protocol loopback branch is delivered. The grey BLE and
Raspberry Pi branch is planned and cannot move hardware today.

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Parser as Parser español
    participant Policy as ActionPolicy
    participant Loop as LoopbackBodyTransport
    participant Codec as BodyProtocolCodec
    participant Peer as LoopbackBodyPeer
    participant BLE as BodyBleTransport
    participant Body as Raspberry Pi BodyController
    participant Stop as EmergencyStopController

    Loop->>Codec: GET_CAPABILITIES
    Codec->>Peer: JSON v1 (bytes)
    Peer-->>Codec: CAPABILITIES (bytes)
    Codec-->>Loop: maxSteps=6, seal_wiggle, watchdog=750 ms

    Persona->>Parser: “Da tres pasos”
    Parser->>Policy: move_steps(count=3)
    Policy->>Loop: BodyCapabilities negociadas

    alt Cantidad permitida
        Policy->>Loop: MOVE_STEPS(3, commandId, deadline)
        Loop->>Codec: codificar comando tipado
        Codec->>Peer: JSON v1 (bytes)
        Peer-->>Codec: ACCEPTED + duración (bytes)
        Codec-->>Loop: evento validado
        Loop-->>Persona: “Simulación: dando 3 pasos…”
        loop Antes de 750 ms mientras esté activo
            Loop->>Codec: HEARTBEAT con ID único
            Codec->>Peer: JSON v1 (bytes)
            Peer-->>Codec: ALIVE(moving=true)
        end
        Peer-->>Codec: COMPLETED (bytes)
        Codec-->>Loop: evento validado
        Loop-->>Persona: “Simulación completada: di 3 pasos”
    else Fuera de rango
        Policy-->>Persona: aclaración o rechazo en español
    end

    opt La persona dice “para” durante el movimiento
        Persona->>Stop: “para”
        Stop->>Loop: STOP(stopCommandId)
        Loop->>Codec: STOP JSON v1
        Codec->>Peer: bytes
        Peer-->>Loop: STOPPED solicitado
        Loop-->>Persona: “Simulación detenida”
    end

    Persona->>Parser: “Baila”
    Parser->>Policy: dance(routineId="seal_wiggle")
    Policy->>Loop: DANCE(seal_wiggle, commandId, deadline)
    Loop->>Codec: comando/eventos JSON v1
    Codec->>Peer: bytes
    Peer-->>Loop: COMPLETED o STOPPED validado

    rect rgb(245, 245, 245)
        Note over Policy,Body: Futuro transporte físico, no ejecutado hoy
        Policy->>BLE: GET_CAPABILITIES / comando validado
        BLE->>Body: GATT con heartbeat
        Body-->>BLE: ACCEPTED / COMPLETED / STOPPED
    end
```

The shipped branch crosses the strict production v1 codec but is explicitly
simulated and never opens Bluetooth. Malformed or unexpected peer telemetry
disconnects the loop and stops its active command. The app never sends free-form
joint or motor values. In the future physical branch, the Raspberry Pi executes a
bounded two-servo routine and independently stops when its BLE watchdog or
deadline expires.

## “¿Qué sabes de X?”

**Status: planned.** This means open-ended knowledge such as dinosaurs, not the
delivered structured query “¿qué sabes de Pedro?”. No conversational model runs.

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Router as ActionRouter
    participant Memory as MemoryStore
    participant Model as ConversationalModel local
    participant Reply as ResponseComposer

    Persona->>Router: “¿Qué sabes de los dinosaurios?”
    Router->>Memory: buscar recuerdos confirmados sobre “dinosaurios”
    Memory-->>Router: hechos/observaciones relevantes o vacío
    Router->>Model: tema + recuerdos etiquetados + instrucción en español
    Model-->>Reply: respuesta local
    Reply-->>Persona: respuesta en español

    note over Router,Model: No hay búsqueda web
    note over Memory,Reply: “Recuerdo que…” distingue memoria de conocimiento general
```

The response admits uncertainty and does not present model training knowledge as
current information.

## “¿Qué ves?”

**Status: delivered.** Free-form scene captioning is not; the current result is a
bounded YOLO object report plus the guarded SFace branch described below.

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Session as MainActivity
    participant Eyes as KikoEyesView
    participant Camera as SceneCameraCapture
    participant Preview as PreviewView
    participant Vision as LocalVisionEngine
    participant Model as YOLO26n ONNX verificado
    participant Face as FaceDetector + SFace verificado
    participant Registry as FaceIdentityStore cifrado
    participant Subjects as VisualSubjectStore cifrado
    participant Reply as SpanishSceneDescription
    participant History as Historial visual privado
    participant Voice as TTS español sin red

    Persona->>Session: “Kiko, ¿qué ves?” o ventana de 10 s
    Session->>Session: comprobar modelo local verificado

    alt Modelo ausente o no verificado
        Session-->>Reply: pedir descarga desde Modelos locales
        Reply-->>Persona: explicación en español sin abrir la cámara
    else Permiso, cámara y modelo verificado disponibles
        Session->>Eyes: modo SQUINTING
        Session->>Camera: describe_scene() determinista
        Camera->>Camera: abrir cámara trasera
        Camera->>Preview: mostrar feed en vivo
        Preview-->>Persona: viewport durante encuadre
        Camera->>Camera: capturar un frame
        Camera->>Preview: ocultar y liberar
        Camera-->>Session: bitmap
        Camera->>Camera: cerrar
        Session->>Vision: bitmap + instante de captura
        Vision->>Model: inferencia ONNX Runtime local
        Model-->>Vision: clases COCO + confianza
        alt YOLO incluye person
            Vision->>Face: preparar una cara + embedding
            Face->>Registry: comparar localmente
            alt Coincidencia clara
                Registry-->>Reply: “Veo a <nombre>”
                Vision->>Subjects: asociar foto → persona reconocida
            else Cara desconocida usable
                Registry-->>Reply: “Veo una persona, no la conozco, ¿quién es?”
            else SFace ausente, cara no usable o error
                Face-->>Reply: explicación sin adivinar
            end
        else Sin person
            Vision-->>Reply: hasta 3 tipos de objeto
        end
        Vision->>History: guardar JPEG orientado + respuesta exacta
        Reply-->>Persona: descripción visible en español
        opt Hay voz española instalada sin red
            Reply->>Voice: frase española
            Voice-->>Persona: voz robótica simple
        end
        opt Cara desconocida usable y foto guardada
            Session->>Eyes: modo LISTENING
            Session-->>Persona: escuchar nombre local (máx. 2 intentos / 12 s)
            Persona->>Session: nombre o “cancelar”
            Session-->>Persona: confirmar Guardar/Cancelar en pantalla desbloqueada
            alt Guardar confirmado
                Session->>Registry: cifrar nombre + embedding + vínculo
                Session->>Subjects: cifrar asociación foto → persona
            else Cancelar, timeout o nombre inválido
                Session->>History: dejar foto sin nombre
            end
        end
        Vision->>Vision: reciclar bitmaps de trabajo
        Session->>Eyes: volver a escucha tras respuesta
    else Sin permiso o captura fallida
        Session-->>Reply: error recuperable
        Reply-->>Persona: explicación en español
    end
```

A scene without YOLO's `person` class does not run identity recognition. The
unlocked user may explicitly choose a previously stored cat/dog for a photo;
object detection never guesses an individual pet. Named photos are grouped via a
separate encrypted subject registry. Every capture initially enters app-private
troubleshooting history, where the user can remove a tag, forget one identity,
delete one record and its linked identity, or erase everything. An optional sleep
policy may later delete captures with a conclusive no-object/error outcome that
remain unnamed. Recognized but unnamed objects remain. A clear face match is only
a toy label and never authentication.

## “¿A quién ves?” and face enrollment

**Status: planned as standalone spoken commands.** The delivered “¿qué ves?” flow
can already match a clear enrolled face and can enroll an unknown usable face
after an unlocked on-screen confirmation.

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Owner as Control de propietario
    participant Perception as PerceptionCoordinator
    participant Camera as Cámara Android
    participant Face as Detector + embeddings
    participant Registry as FaceRegistry cifrado
    participant Reply as ResponseComposer

    Persona->>Perception: “¿A quién ves?”
    Perception->>Camera: capturar un frame
    Camera-->>Perception: frame
    Perception->>Face: detectar y obtener embeddings
    Face->>Registry: comparar con identidades locales
    Registry-->>Face: nombres sobre umbral + unknown
    Face-->>Reply: coincidencias locales
    alt Hay coincidencia suficiente
        Reply-->>Persona: “Veo a Ana”
    else Rostro desconocido o ambiguo
        Reply-->>Persona: “No sé quién es”
    end
    Perception->>Perception: descartar frame y embeddings temporales

    Persona->>Perception: “Recuerda esta cara como Ana”
    Perception->>Camera: capturar un frame de inscripción
    Camera-->>Face: frame
    Face-->>Owner: nombre + calidad + una cara detectada
    Owner-->>Persona: confirmación en pantalla desbloqueada
    alt Propietario confirma
        Owner->>Registry: guardar nombre + embedding cifrado
        Registry-->>Reply: inscripción completada
        Reply-->>Persona: “Recordaré a Ana”
    else Rechazo, varias caras o baja calidad
        Owner-->>Reply: no guardar
        Reply-->>Persona: explicación en español
    end
```

Face matches are friendly labels, never authentication. They cannot authorize
motion, reveal private memories, or enter owner settings.

## Memoria estructurada sobre personas

**Status: delivered.**

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Session as MainActivity
    participant Parser as SpanishPersonMemoryParser
    participant Memory as PersonMemoryStore cifrado
    participant Reply as SpanishPersonMemoryResponses
    participant Owner as Memorias

    Persona->>Session: “Kiko”
    Persona->>Session: “La comida favorita de Pedro es la pasta”
    Session->>Parser: hipótesis final
    Parser-->>Session: SET_FAVORITE_FOOD(Pedro, la pasta)
    Session->>Memory: commit AES-GCM
    Memory-->>Reply: registro actualizado
    Reply-->>Persona: “¡A mí también me gusta la pasta!”
    Reply-->>Persona: pantalla “memoria actualizada”

    Persona->>Session: “Kiko”
    Persona->>Session: “¿Qué sabes de Pedro?”
    Session->>Parser: hipótesis final
    Parser-->>Session: QUERY_SUMMARY(Pedro)
    Session->>Memory: buscar nombre canónico
    Memory-->>Reply: comida + gustos + edad guardados
    Reply-->>Persona: respuesta solo con esos datos

    Owner->>Memory: ver / borrar Pedro / borrar todo
    Memory-->>Owner: estado local actualizado
```

The complete bounded declaration is explicit authorization to store that one
fact. Partial hypotheses, unsupported predicates, malformed names, out-of-range
ages, and overlong values never mutate memory. Person memory and face identity
remain separate encrypted registries.

## Memoria estructurada sobre gatos y perros

**Status: delivered.**

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Session as MainActivity
    participant Parser as SpanishPetMemoryParser
    participant Memory as PetMemoryStore cifrado
    participant Reply as SpanishPetMemoryResponses
    participant Owner as Memorias

    Persona->>Session: “Kiko”
    Persona->>Session: “Luna es la gata de Pedro”
    Session->>Parser: hipótesis final
    Parser-->>Session: REGISTER(Luna, GATA, Pedro)
    Session->>Memory: commit AES-GCM con clave separada
    Memory-->>Reply: registro actualizado
    Reply-->>Persona: “Recordaré que Luna es la gata de Pedro”
    Reply-->>Persona: pantalla “memoria actualizada”

    Persona->>Session: “Kiko”
    Persona->>Session: “¿Qué sabes de la gata Luna?”
    Session->>Parser: QUERY_SUMMARY(Luna, GATA)
    Session->>Memory: buscar nombre + especie
    Memory-->>Reply: dueño + comida + gustos + edad
    Reply-->>Persona: respuesta solo con esos datos

    Persona->>Session: “¿Qué mascotas tiene Pedro?”
    Session->>Memory: buscar dueño canónico
    Memory-->>Reply: gatos/perros vinculados explícitamente

    Owner->>Memory: ver / borrar Luna / borrar todo
    Memory-->>Owner: estado local actualizado
```

Only `gato`, `gata`, `perro`, and `perra` are accepted. Individual pet facts and
queries require the species marker so an unqualified name cannot collide with a
person. The unlocked **Memorias** screen combines both record types but person,
pet, and face data retain separate keys and registries.

## Sueño local y consolidación segura

**Status: delivered.**

```mermaid
sequenceDiagram
    autonumber
    actor Owner as Dueño desbloqueado
    participant UI as Sueño
    participant Scheduler as WorkManager
    participant Worker as SleepMaintenanceWorker
    participant People as PersonMemoryStore
    participant Pets as PetMemoryStore
    participant Faces as FaceIdentityStore
    participant Subjects as VisualSubjectStore
    participant History as VisualHistoryStore
    participant Report as SleepMaintenanceReportStore

    Owner->>UI: activar diario o programar una ejecución
    opt consentimiento destructivo separado
        Owner->>UI: activar “borrar fotos no reconocidas”
    end
    UI->>Scheduler: trabajo único + restricciones
    Note over Scheduler: cargando + inactivo + batería/storage<br/>NetworkType.NOT_REQUIRED

    alt temperatura severa
        Scheduler->>Worker: iniciar
        Worker->>Report: mantener estado en espera
        Worker-->>Scheduler: retry con backoff
    else condiciones seguras
        Scheduler->>Worker: iniciar
        Worker->>Report: RUNNING
        Worker->>People: descifrar + validar + consolidar duplicados
        Worker->>Pets: descifrar + validar + consolidar duplicados
        Worker->>Faces: descifrar + validar solamente
        Worker->>Subjects: descifrar + validar asociaciones
        alt todos válidos
            Worker->>History: agrupar fotos por persona/mascota
            opt limpieza de fotos activada
                Worker->>History: borrar solo no-object/error sin nombre
            end
            Worker->>Report: SUCCESS + timestamps + conteos
            Report-->>UI: informe sin nombres ni hechos
        else un registro no es legible
            Worker->>Report: FAILED
            Note over Worker: el registro inválido no se reemplaza
        end
    end
```

The worker never opens microphone/camera, initializes inference, accesses the
network, generates memories, trains weights, deletes distinct facts, or controls
the body. Unnamed-photo deletion is initially disabled and independent of the
automatic schedule; unreadable face/subject registries stop cleanup rather than
classifying every photo as unknown. A one-time request can be cancelled;
disabling automatic sleep cancels only the periodic request.

## “Recuerda esto”

**Status: planned.** Only the bounded person and pet declarations above are
durable spoken-memory commands today.

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Session as SpanishCommandSession
    participant Resolver as MemoryCandidateResolver
    participant Memory as MemoryStore cifrado
    participant Reply as ResponseComposer

    Persona->>Session: “Recuerda esto: mi color favorito es verde”
    Session->>Resolver: texto + contexto de la sesión actual
    Resolver-->>Session: candidato único
    Session-->>Persona: “¿Recuerdo que tu color favorito es verde?”

    alt Confirma
        Persona->>Session: “Sí”
        Session->>Memory: guardar MemoryItem confirmado
        Memory-->>Reply: id + timestamp
        Reply-->>Persona: “Lo recordaré”
    else Rechaza
        Persona->>Session: “No”
        Session-->>Persona: “No lo guardaré”
    else No existe un candidato único
        Resolver-->>Reply: ambiguous
        Reply-->>Persona: “¿Qué quieres que recuerde?”
    end

    Persona->>Session: “¿Qué recuerdas de mí?”
    Session->>Memory: consultar recuerdos confirmados
    Memory-->>Reply: hechos y observaciones locales
    Reply-->>Persona: “Recuerdo que…”

    Persona->>Session: “Olvida que mi color favorito es verde”
    Session-->>Persona: confirmación
    Persona->>Session: “Sí”
    Session->>Memory: eliminar coincidencia
    Memory-->>Persona: “Lo olvidé”
```

Ordinary conversation is ephemeral. Only explicit bounded `PersonMemoryRecord`
or `PetMemoryRecord` updates, or separately confirmed future `MemoryItem` records,
enter durable memory, and a remembered scene stores text rather than another copy
of its camera frame.
The separate visual troubleshooting history initially retains the original
`describe_scene` capture until manual deletion or opted-in unrecognized-photo
cleanup; recognized captures remain even without a person/pet name.

## Failure containment

**Status: mixed safety contract.** The loopback implements protocol deadlines,
heartbeat, invalid-telemetry, lifecycle, and stop containment. The same contract
must govern the future BLE transport and physical body.

```mermaid
flowchart TD
    Request["Petición española"] --> Available{"¿Componente disponible?"}
    Available -- "No" --> Explain["Explicar el problema<br/>sin fingir éxito"]
    Available -- "Sí" --> Parse{"¿Esquema válido?"}
    Parse -- "No" --> Clarify["Pedir aclaración"]
    Parse -- "Sí" --> Allowed{"¿ActionPolicy permite?"}
    Allowed -- "No" --> Refuse["Rechazar en español"]
    Allowed -- "Sí" --> Execute["Ejecutar herramienta nativa"]
    Execute --> Ack{"¿Acuse antes del plazo?"}
    Ack -- "Sí" --> Report["Reportar resultado real"]
    Ack -- "No" --> Stop["STOP + cancelar + cerrar recursos"]
    Stop --> Explain

    Emergency["“para” o botón STOP"] --> Stop
    Disconnect["BLE desconectado o sin heartbeat"] --> Stop
    Lifecycle["App detenida"] --> Stop
    InvalidTelemetry["Telemetría inválida"] --> Stop
```
