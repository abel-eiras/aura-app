# Tareas: Importar audio

**Spec:** `spec.md` · **Plan:** `plan.md`

## Fase 1 — Dominio

- [x] T009-01 [P] Tests: formatos admitidos, MIME, extensión por nombre o tipo, nombre único (FR-009-02, 03)
- [x] T009-02 `AudioFormats`, `RecordingNaming` con extensiones admitidas, `GeminiProvider.mimeTypeOf`

## Fase 2 — Android

- [x] T009-03 `AudioImporter` (copia atómica, validación, registro, procesado y exportación) (FR-009-03…06)
- [x] T009-04 Recuperación de huérfanos y "Compartir audio" con cualquier extensión admitida
- [x] T009-05 "Importar audio" en la lista de grabaciones y resultado en un aviso (FR-009-01, 08)
- [x] T009-06 Aura como destino de "Compartir" (FR-009-07)
- [x] T009-07 Cadenas es/gl/en

## Fase 3 — Verificación

- [x] T009-08 CI en verde
- [ ] T009-09 **Autor:** importar una nota de voz de WhatsApp (compartir y selector), un `m4a` y un `mp3`; comprobar fecha, nota y exportación
