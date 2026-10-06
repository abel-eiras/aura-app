# Tareas: Grabación de audio

**Spec:** `spec.md` · **Plan:** `plan.md`

## Fase 1 — Dominio

- [x] T001-01 [P] Test: estado con pausas, nombres, calidades, umbrales (FR-001-04, 05, 08, 11, 12)
- [x] T001-02 `AudioQuality`, `RecordingStatus`/`RecordingTransitions`, `RecordingNaming`, `RecordingPolicy`

## Fase 2 — Datos y motor

- [ ] T001-03 Room: `RecordingEntity` + DAO (FR-001-07, 09)
- [ ] T001-04 `AppContainer`, preferencias (calidad, aviso de batería visto)
- [ ] T001-05 `AudioRecorderEngine` con `MediaRecorder` OGG/Opus (FR-001-04, 05)
- [ ] T001-06 `RecordingStateHolder`
- [ ] T001-07 `RecordingRepository`: registrar, listar, borrar, recuperar huérfanos (FR-001-07, 09)

## Fase 3 — Servicio, mosaico y widget

- [ ] T001-08 `RecordingService`: notificación fija, acciones, espacio libre, descarte < 1 s (FR-001-01, 02, 11, 12)
- [ ] T001-09 `CallStateMonitor` (FR-001-06)
- [ ] T001-10 `AuraTileService` (FR-001-03)
- [ ] T001-11 `AuraWidgetProvider` (FR-001-03)

## Fase 4 — UI

- [ ] T001-12 Permisos con explicación previa y aviso de batería (FR-001-10)
- [ ] T001-13 Pantalla principal: orbe, contador, pausar (HU-001-1, 3)
- [ ] T001-14 Lista de grabaciones: reproducir/pausar/posición, borrar con confirmación, total y aviso 2 GB (FR-001-09, HU-001-4)
- [ ] T001-15 Ajustes: calidad y versión (FR-001-04)
- [ ] T001-16 Cadenas es/gl/en

## Fase 5 — Verificación

- [ ] T001-17 CI en verde (lint + compilación)
- [ ] T001-18 **Autor:** prueba manual con APK: 60 min con pantalla apagada (CE-001-1), mosaico < 1 s (CE-001-2), pausa por llamada, permiso denegado
- [ ] T001-19 Marcar la spec como "Implementada"
