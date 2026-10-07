# Tareas: Grabación de audio

**Spec:** `spec.md` · **Plan:** `plan.md`

## Fase 1 — Dominio

- [x] T001-01 [P] Test: estado con pausas, nombres, calidades, umbrales (FR-001-04, 05, 08, 11, 12)
- [x] T001-02 `AudioQuality`, `RecordingStatus`/`RecordingTransitions`, `RecordingNaming`, `RecordingPolicy`

## Fase 2 — Datos y motor

- [x] T001-03 Room: `RecordingEntity` + DAO (FR-001-07, 09)
- [x] T001-04 `AppContainer`, preferencias (calidad, aviso de batería visto)
- [x] T001-05 `AudioRecorderEngine` con `MediaRecorder` OGG/Opus (FR-001-04, 05)
- [x] T001-06 `RecordingStateHolder`
- [x] T001-07 `RecordingRepository`: registrar, listar, borrar, recuperar huérfanos (FR-001-07, 09)

## Fase 3 — Servicio, mosaico y widget

- [x] T001-08 `RecordingService`: notificación fija, acciones, espacio libre, descarte < 1 s (FR-001-01, 02, 11, 12)
- [x] T001-09 `CallStateMonitor` (FR-001-06)
- [x] T001-10 `AuraTileService` (FR-001-03)
- [x] T001-11 `AuraWidgetProvider` (FR-001-03)

## Fase 4 — UI

- [x] T001-12 Permisos con explicación previa y aviso de batería (FR-001-10)
- [x] T001-13 Pantalla principal: orbe, contador, pausar (HU-001-1, 3)
- [x] T001-14 Lista de grabaciones: reproducir/pausar/posición, borrar con confirmación, total y aviso 2 GB (FR-001-09, HU-001-4)
- [x] T001-15 Ajustes: calidad y versión (FR-001-04)
- [x] T001-16 Cadenas es/gl/en

## Fase 4b — Hallazgos de la prueba en un Pixel 10 Pro (0.2.0)

Grabar y pausar funcionaban; **parar desde la app no**, aunque sí desde la notificación.

- [x] T001-20 Botón "Parar y guardar" explícito en la pantalla principal junto a Pausar (HU-001-1, escenario 3). Antes solo el círculo alternaba y no había indicación de que parase
- [x] T001-21 Trazas (`Log.i`) en todo el camino de parada: toque en el círculo → `MainViewModel` → `RecordingService` → `MediaRecorder.stop()` (con su duración) → registro en Room → `finish`
- [ ] T001-22 **Autor:** con la 0.2.1, parar con el botón nuevo y con el círculo; si el círculo sigue sin responder, pasar `adb logcat -d | grep -E "AuraApp|MainViewModel|RecordingService|AudioRecorderEngine"` para localizar dónde se pierde

## Fase 5 — Verificación

- [x] T001-17 CI en verde (lint + compilación + tests del dominio)
- [ ] T001-18 **Autor:** prueba manual con APK: 60 min con pantalla apagada (CE-001-1), mosaico < 1 s (CE-001-2), pausa por llamada, permiso denegado
- [ ] T001-19 Marcar la spec como "Implementada"
