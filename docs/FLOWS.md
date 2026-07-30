# Runtime flows

These Mermaid diagrams specify how the planned Spanish toy behaviors cross the
architecture boundaries defined in `docs/ARCHITECTURE.md`. They describe the
target design, not behavior implemented by the current download-only milestone.

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
    participant USB as UsbBodyTransport
    participant Body as Firmware del cuerpo
    participant Stop as EmergencyStopController

    Persona->>Parser: “Da tres pasos”
    Parser->>Policy: move_steps(count=3)
    Policy->>USB: GET_CAPABILITIES
    USB->>Body: GET_CAPABILITIES
    Body-->>USB: límites + versión + rutinas
    USB-->>Policy: BodyCapabilities

    alt Cantidad permitida
        Policy->>USB: MOVE_STEPS(3, commandId, deadline)
        USB->>Body: comando enmarcado
        Body-->>USB: ACCEPTED(commandId)
        Body-->>USB: COMPLETED(commandId, telemetry)
        USB-->>Persona: “Di tres pasos”
    else Fuera de rango o cuerpo ausente
        Policy-->>Persona: aclaración o rechazo en español
    end

    opt La persona dice “para” durante el movimiento
        Persona->>Stop: “para”
        Stop->>USB: STOP(stopCommandId)
        USB->>Body: STOP inmediato
        Body-->>USB: STOPPED
        USB-->>Persona: “Me detuve”
    end

    Persona->>Parser: “Baila”
    Parser->>Policy: dance(routineId="default")
    Policy->>USB: DANCE(routineId, commandId, deadline)
    USB->>Body: rutina nativa allowlisted
    Body-->>USB: COMPLETED o STOPPED
```

The app never sends free-form joint or motor values. The firmware executes a
bounded routine and independently stops when its watchdog or deadline expires.

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
    participant Router as ActionRouter
    participant Perception as PerceptionCoordinator
    participant Camera as Cámara Android
    participant Vision as VisionEngine local
    participant Reply as ResponseComposer

    Persona->>Router: “¿Qué ves?”
    Router->>Perception: describe_scene()

    alt Permiso y cámara disponibles
        Perception->>Camera: abrir y capturar un frame
        Camera-->>Perception: frame + timestamp
        Perception->>Vision: describir frame en español
        Vision-->>Perception: observación textual
        Perception->>Camera: cerrar
        Perception-->>Reply: observación estructurada
        Reply-->>Persona: descripción en español
        Perception->>Perception: descartar frame
    else Sin permiso o captura fallida
        Perception-->>Reply: error recuperable
        Reply-->>Persona: explicación en español
    end
```

A normal scene description does not run identity recognition and does not retain
the frame.

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
    Disconnect["USB desconectado"] --> Stop
    Lifecycle["App detenida"] --> Stop
    InvalidTelemetry["Telemetría inválida"] --> Stop
```
