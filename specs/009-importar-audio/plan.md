# Plan de implementación: Importar audio

**Spec:** `specs/009-importar-audio/spec.md` · **Fecha:** 2026-10-12

## Resumen

Un importador copia el fichero elegido (selector del sistema o "Compartir") al directorio de grabaciones con el
nombre estándar, lo valida y lo registra; desde ahí es una grabación más. La lógica pura (formatos, tipos MIME,
nombres únicos) vive en `:domain` con tests; Android aporta el selector, la copia y la UI.

## Contexto técnico

- **Dominio** (`recording/AudioFormats`, `RecordingNaming`): extensiones admitidas, tipo MIME por extensión,
  extensión a partir de nombre o tipo MIME, nombre único si ya existe uno con esa fecha. `RecordingNaming` y la
  recuperación de huérfanos aceptan ahora todas las extensiones admitidas, no solo `.ogg`.
- **`:app`**: `AudioImporter` (copia a `<nombre>.part`, valida con `MediaMetadataRetriever`, renombra, registra,
  y llama a procesado y exportación); selector `OpenMultipleDocuments`; filtro de intents `SEND`/`SEND_MULTIPLE`
  de `audio/*` en `MainActivity`; resultado con un aviso (`Toast`).
- **Fecha de la grabación**: `COLUMN_LAST_MODIFIED` del documento si el proveedor la da; si no, la hora actual.
- **Sin migración**: la entidad de Room no cambia (la calidad se guarda como la de por defecto).
- **Tipo MIME al proveedor**: sale de la extensión (`GeminiProvider.mimeTypeOf` usa `AudioFormats`).

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| I. Usable sin ayuda | Sí | Un botón y el menú de compartir del sistema |
| II / III | Sí | Sin red nueva ni permisos nuevos (el selector no pide permisos de almacenamiento) |
| IV. Privacidad | Sí | Nada sale del móvil salvo lo que ya decide el procesado (aviso aceptado, FR-003-13) |
| VI. Nunca perder una grabación | Sí | Se copia, no se mueve; el original no se toca; la copia a medias no se lista |
| VII. Simplicidad | Sí | Reutiliza el flujo de una grabación nueva |
| X. Calidad | Parcial | Formatos y nombres con tests; el selector y la copia, solo CI y prueba manual |

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-009-01, 07, 08 | `RecordingsScreen`, `MainActivity`, manifiesto | Manual |
| FR-009-02 | `AudioFormats` | `AudioFormatsTest` |
| FR-009-03, 03b | `RecordingNaming`, `AudioImporter` | `RecordingTest`, `AudioFormatsTest` |
| FR-009-04, 05 | `AudioImporter` | Manual |
| FR-009-06 | `AppContainer` (procesado y exportación tras importar) | Manual |
