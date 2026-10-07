# Plan de implementación: Notas, hablantes y exportación

**Spec:** `specs/004-notas-y-exportacion/spec.md` · **Fecha:** 2026-10-06 · **Estado del plan:** parcial (dominio + hito 1: compartir audio y exportar audio a carpeta; faltan nota, transcripción y vista de nota).

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

## Hito 1: compartir audio y exportar audio (diseño, implementado)

- **Compartir audio (FR-004-05, solo audio)**: `FileProvider` sobre `filesDir/recordings/` y `ACTION_SEND` con `audio/ogg` desde cada fila de la lista.
- **Carpeta de exportación (FR-004-07, solo audio)**: selector del sistema (`OpenDocumentTree`), permiso persistente (`takePersistableUriPermission`), se muestra el nombre de la carpeta. Si ya hay grabaciones, se pregunta si exportarlas también (escenario 5); la respuesta se guarda como `exportFromMillis`.
- **Estado de exportación derivado, sin migración de Room**: `ExportPolicy.status()` calcula NONE / PENDING / ERROR / EXPORTED a partir de un *ledger* de ficheros exportados (`StringSet` en preferencias), `exportFromMillis` y los fallos de la sesión. Motivo: un cambio de esquema en Room sin tests de migración podría romper el arranque de quien actualiza desde la 0.1.0, justo lo que se quiere probar con el actualizador. Cuando 003 necesite estado por transcripción y nota se migrará con tests (T004-06).
- **Escritura (FR-004-09)**: se crea `<nombre>.ogg.part` (mime genérico, para que el proveedor no le cambie el nombre), se copia, y se renombra con `DocumentsContract.renameDocument`. Si el proveedor no soporta renombrar, se escribe directamente con el nombre final (aura-transcribe ya espera a que el fichero deje de cambiar). Un `.part` huérfano de un cierre anterior se borra antes de reintentar.
- **Colisiones**: si ya hay un fichero con el mismo nombre y el mismo tamaño se da por exportado (idempotente); si el tamaño difiere se usa `ExportNaming.uniqueName` (`_1`, `_2`…), nunca se sobrescribe.
- **Cuándo se intenta**: al registrar una grabación nueva, al abrir la app y con "Reintentar". No hay vigilancia de la carpeta en segundo plano (el escenario 4 se cumple al reintentar). Si el proceso muere a mitad de una copia, queda PENDING y se rehace.
- **Borrar (FR-004-10)**: no toca lo exportado; el diálogo lo dice si hay carpeta configurada.
- **Permiso perdido**: si la carpeta ya no está en `persistedUriPermissions` (p. ej. tras restaurar en otro móvil) Ajustes lo explica y pide elegirla otra vez.

## Pendiente de diseñar

- Escritura atómica en SAF (fichero temporal + renombrado con `DocumentsContract`), reintentos y estado "Exportación pendiente" (FR-004-09).
- Mover la nota al cambiar de tipo (no dejar dos del mismo audio).

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-004-02, 03 | `Speakers` | `SpeakersTest`, `NoteRendererTest` |
| FR-004-08 | `NoteRenderer` | `NoteRendererTest` |
| FR-004-05 (audio) | `ShareRecording`, `FileProvider` | Manual |
| FR-004-07 (audio) | `ExportRepository`, `SettingsScreen` | `ExportTest` (estado); manual (SAF) |
| FR-004-09 | `ExportRepository`, `ExportNaming` | `ExportTest` (nombres); manual (SAF) |
| FR-004-10 | `RecordingsScreen` (diálogo) | Manual |
| FR-004-01, 04, 06 | `:app` | Hito 2 |
