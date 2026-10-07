# Plan de implementación: Procesado con IA

**Spec:** `specs/003-procesado-ia/spec.md` · **Fecha:** 2026-10-06 · **Estado del plan:** Gemini completo (dominio + worker); OpenRouter pendiente de su spike.

## Resumen

Dos pasos persistidos por separado. El dominio ya define la interfaz
`AiProvider`, la política de reintentos, la normalización de hablantes y el
paso 2 completo (`NoteDrafter`). Hecho en el hito 2: `GeminiProvider`, `ProcessingPipeline` y el worker. Falta OpenRouter.

## Contexto técnico

- **Módulos:** `:domain` (`processing/`, `transcript/`, `ai/`), `:app` (`data/processing`, `ui/notes`).
- **Dependencias nuevas:** WorkManager 2.9.1 (sin GMS: usa `JobScheduler`).
- **Tests:** `ProcessingPolicyTest`, `NoteDrafterTest`, `SpeakersTest` (JVM). Clientes con `MockWebServer` y respuestas grabadas.

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| II. Sin servidores propios | Sí | Cuenta del usuario, directo al proveedor |
| IV. Privacidad | Sí | Solo audio (paso 1) y texto + prompt (paso 2); aviso aceptado antes (FR-003-13/14) |
| VI. Nunca perder una grabación | Sí | Errores nunca tocan el audio; cada paso persistido |
| VIII. Compatibilidad | Sí | Contrato `transcripcion.schema.json` |

## Diseño (hecho)

- `ProcessingPolicy`: `MAX_ATTEMPTS = 5`, backoff 15 s·2ⁿ con tope 15 min, `onFailure(error, attempts)` → `Retry`/`GiveUp`, `nextStep(...)` para reanudar, límite de 3 h (FR-003-01, 07, 08).
- `ProviderException(reason, transient)`: los proveedores clasifican; el worker decide con la política.
- `Speakers.normalize`: etiquetas `SPEAKER_NN` por primera aparición, descarta segmentos en blanco, recalcula `full_text` y nº de hablantes (FR-003-03, 09).
- `NoteDrafter`: clasifica (T=0.0, solo `criterio`) y redacta (T=0.3, `prompt_redaccion`, `modelo` del tipo o el general); borrador vacío → `INVALID_RESPONSE` transitorio (FR-003-05).

## Diseño del hito 2 (Gemini)

- **Persistencia en ficheros, no en Room** (`FileProcessingStore`): `<id>.transcript.json`, `<id>.note.json`, `<id>.job.json` en `filesDir/processing/` (`<id>` = nombre del audio sin extensión), con escritura atómica (`.part` + renombrado). Así una actualización desde 0.2.0 no necesita migración de la base de datos y un proceso muerto a medias nunca deja una transcripción a medio escribir. Cambia respecto a lo previsto (T003-10, Room); el estado de exportación de 004 (T004-06b) sigue el mismo criterio.
- **`ProcessingPipeline`** (dominio, probado con un almacén en memoria): una pasada ejecuta los pasos pendientes, persiste cada resultado antes del siguiente (FR-003-01), y ante un `ProviderException` decide con `ProcessingPolicy`: `RetryLater(espera)` o `Finished(ERROR, motivo)`. Una respuesta con formato inválido se reintenta una sola vez; un audio de más de 3 h se rechaza antes de subir nada; el silencio acaba en `NO_CONTENT` sin llamar al paso 2 (FR-003-09). La cancelación se propaga sin marcar error.
- **`GeminiProvider`**: subida reanudable a la Files API (cualquier tamaño), espera a `ACTIVE`, `generateContent` con `responseSchema` (idioma + segmentos `{speaker,start,end,text}`), borrado del fichero remoto en `finally`. La clave va en cabecera. Clasificación de errores (FR-003-07): 429 con "per day" → cuota agotada (permanente); 429 → límite (transitorio); 400/401/403 con "API key" → credencial; "location is not supported" → no disponible (permanente); 5xx y red → transitorios.
- **`ProcessingRepository` + `ProcessingWorker`** (`:app`): `enqueueUniqueWork("process-<id>", KEEP)` con restricción de red y *backoff* exponencial; un `Mutex` de proceso procesa una grabación cada vez (FR-003-06). El worker no envía nada si el modo, la credencial o el aviso de privacidad no lo permiten (`ProviderController.keyForSending()`, FR-003-13). Se procesa solo al guardar si "Procesar automáticamente" está activo (FR-003-12) o con el botón "Procesar".
- **Notificación** (FR-003-10): "Nota lista: <tipo> — <fecha>" solo si la app no está visible; abre la nota. Desactivable en Ajustes.
- **Plan B si Gemini rechaza Opus-en-OGG**: un fichero `FAILED` en la Files API produce un error permanente claro; la conversión previa (p. ej. a FLAC/WAV) se añade en `GeminiProvider.transcribe` solo si el spike con `scripts/probe-gemini.sh` lo confirma.

## Spikes pendientes (bloquean el plan de los clientes)

1. **Gemini**: subida con Files API + salida JSON estructurada con hablantes; calidad en es/gl; tamaño máximo; qué errores devuelve por cuota (para clasificar transitorio/permanente).
2. **OpenRouter**: modelos que aceptan audio, límite de tamaño; callback del OAuth PKCE (spec 002).

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-003-01 | `ProcessingPolicy.nextStep` | `ProcessingPolicyTest` |
| FR-003-02 | `Transcript` (+ JSON) | `SpeakersTest` (round-trip) |
| FR-003-03 | `Speakers.normalize` | `SpeakersTest` |
| FR-003-05 | `NoteDrafter`, `NoteClassifier` | `NoteDrafterTest`, `NoteClassifierTest` |
| FR-003-07, 08 | `ProcessingPolicy` | `ProcessingPolicyTest` |
| FR-003-09 | `Transcript.hasContent`, `NoteDrafter.run` | `SpeakersTest`, `NoteDrafterTest` |
| FR-003-04, 06, 10–14 | `:app` | Hito 2 |
