# Plan de implementación: Notas, hablantes y exportación

**Spec:** `specs/004-notas-y-exportacion/spec.md` · **Fecha:** 2026-10-06 · **Estado del plan:** parcial (dominio).

## Resumen

La parte pura (renombrar/unir hablantes, aplicar nombres, renderizar la nota
Markdown y calcular rutas de exportación) está en `:domain`. La UI, el
reproductor por segmentos y la escritura en la carpeta SAF son del hito 1–2.

## Contexto técnico

- **Módulos:** `:domain` (`notes/NoteRenderer`, `transcript/Speakers`, hecho), `:app` (`ui/note`, `data/export`).
- **Dependencias nuevas (pendientes):** Media3 ExoPlayer; librería de render Markdown para Compose (a elegir, con licencia compatible y sin GMS).
- **Tests:** `NoteRendererTest`, `SpeakersTest` (JVM).

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| III. Sin GMS | Sí | SAF y menú de compartir del sistema |
| VIII. Compatibilidad | Sí | Frontmatter y layout idénticos a aura-transcribe |
| VI. Nunca perder una grabación | Sí | Borrar en la app no borra lo exportado (FR-004-10) |

## Diseño (hecho)

- `Speakers.rename/merge/applyNames/displayNames` (FR-004-02/03): los nombres viven en `metadata.speaker_names`; unir reasigna segmentos y conserva el nombre del destino.
- `NoteRenderer.render`: frontmatter del contrato (`tipo, fecha, audio_origen, json_origen, idioma, hablantes, duracion_min, tags` + `hablantes_nombres`, `generado_por` opcionales), título `# <etiqueta> — <fecha>`, cuerpo con etiquetas sustituidas; strings entrecomillados y escapados.
- `NoteRenderer.exportPaths`: `audio`, `transcription/<stem>.json`, `notas/<carpeta>/<fecha>-<stem>.md` (FR-004-08).

## Pendiente de diseñar

- Escritura atómica en SAF (fichero temporal + renombrado con `DocumentsContract`), reintentos y estado "Exportación pendiente" (FR-004-09).
- Mover la nota al cambiar de tipo (no dejar dos del mismo audio).

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-004-02, 03 | `Speakers` | `SpeakersTest`, `NoteRendererTest` |
| FR-004-08 | `NoteRenderer` | `NoteRendererTest` |
| FR-004-01, 04–07, 09, 10 | `:app` | Hito 1–2 |
