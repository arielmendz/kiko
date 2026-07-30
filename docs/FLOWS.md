# Runtime flows

These Mermaid diagrams specify how the planned Spanish toy behaviors cross the
architecture boundaries defined in `docs/ARCHITECTURE.md`. Most describe the
target design. The “¿Qué ves?” flow now includes the delivered local
object-detection implementation.

## Command lifecycle

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

`EmergencyStopController` issues `STOP` on the acting transition without waiting
for model inference. Any transition to `Idle` caused by “para” also cancels
pending model, camera, and memory work.

## Routing a Spanish command

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

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Parser as Parser español
    participant Policy as ActionPolicy
    participant BLE as BodyBleTransport
    participant Body as Raspberry Pi BodyController
    participant Stop as EmergencyStopController

    Persona->>Parser: “Da tres pasos”
    Parser->>Policy: move_steps(count=3)
    Policy->>BLE: GET_CAPABILITIES
    BLE->>Body: escritura GATT
    Body-->>BLE: límites + versión + rutinas
    BLE-->>Policy: BodyCapabilities

    alt Cantidad permitida
        Policy->>BLE: MOVE_STEPS(3, commandId, timeout)
        BLE->>Body: escritura GATT
        Body-->>BLE: ACCEPTED(commandId)
        loop Mientras haya movimiento
            BLE->>Body: HEARTBEAT
            Body-->>BLE: ALIVE
        end
        Body-->>BLE: COMPLETED(commandId)
        BLE-->>Persona: “Di tres pasos”
    else Fuera de rango o cuerpo ausente
        Policy-->>Persona: aclaración o rechazo en español
    end

    opt La persona dice “para” durante el movimiento
        Persona->>Stop: “para”
        Stop->>BLE: STOP(stopCommandId)
        BLE->>Body: escritura GATT inmediata
        Body-->>BLE: STOPPED
        BLE-->>Persona: “Me detuve”
    end

    Persona->>Parser: “Baila”
    Parser->>Policy: dance(routineId="default")
    Policy->>BLE: DANCE(routineId, commandId, timeout)
    BLE->>Body: rutina nativa allowlisted
    Body-->>BLE: COMPLETED o STOPPED
```

The app never sends free-form joint or motor values. The Raspberry Pi executes a
bounded two-servo routine and independently stops when its BLE watchdog or
deadline expires.

## “¿Qué sabes de X?”

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

```mermaid
sequenceDiagram
    autonumber
    actor Persona
    participant Session as MainActivity
    participant Camera as FrontCameraCapture
    participant Vision as LocalVisionEngine
    participant Model as EfficientDet-Lite0 verificado
    participant Reply as SpanishSceneDescription
    participant Voice as TTS español sin red

    Persona->>Session: “Kiko, ¿qué ves?” o ventana de 10 s
    Session->>Session: comprobar modelo local verificado

    alt Modelo ausente o no verificado
        Session-->>Reply: pedir descarga desde Modelos locales
        Reply-->>Persona: explicación en español sin abrir la cámara
    else Permiso, cámara y modelo verificado disponibles
        Session->>Camera: describe_scene() determinista
        Camera->>Camera: abrir frontal y capturar un frame
        Camera-->>Session: bitmap solo en memoria
        Camera->>Camera: cerrar
        Session->>Vision: bitmap efímero
        Vision->>Model: inferencia LiteRT local
        Model-->>Vision: clases COCO + confianza
        Vision-->>Reply: hasta 3 tipos de objeto
        Reply-->>Persona: descripción visible en español
        opt Hay voz española instalada sin red
            Reply->>Voice: frase española
            Voice-->>Persona: voz robótica simple
        end
        Vision->>Vision: descartar bitmap
    else Sin permiso o captura fallida
        Session-->>Reply: error recuperable
        Reply-->>Persona: explicación en español
    end
```

A normal scene description does not run identity recognition and does not retain
the frame. EfficientDet-Lite0 is a bounded object detector rather than a scene
captioner; a future richer local model may replace it without changing the
explicit permission, ephemeral capture, or no-network boundaries.

## “¿A quién ves?” and face enrollment

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

## “Recuerda esto”

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

Ordinary conversation is ephemeral. Only confirmed `MemoryItem` records enter
durable storage, and a remembered scene stores text rather than its camera frame.

## Failure containment

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
