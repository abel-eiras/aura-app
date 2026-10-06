# Plan de implementación: Procesado con IA

**Spec:** `specs/003-procesado-ia/spec.md` · **Fecha:** 2026-10-06 · **Estado del plan:** parcial (dominio); los clientes de proveedor y el worker se planifican tras los spikes.

## Resumen

Dos pasos persistidos por separado. El dominio ya define la interfaz
`AiProvider`, la política de reintentos, la normalización de hablantes y el
paso 2 completo (`NoteDrafter`). Falta: clientes HTTP de Gemini y OpenRouter,
Room, `ProcessingWorker`.

## Contexto técnico

- **Módulos:** `:domain` (`processing/`, `transcript/`, hecho), `:app` (`data/provider`, `work/`, `data/db`).
- **Dependencias nuevas (pendientes):** OkHttp; Room; WorkManager.
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
