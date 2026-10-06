# Plan de implementación: Grabación de audio

**Spec:** `specs/001-grabacion/spec.md` · **Fecha:** 2026-10-06

## Resumen

Portar de Aura el servicio de grabación, el mosaico, el widget y la pausa por
llamadas, desacoplados de Drive. La lógica pura (máquina de estados con pausas,
nombres de fichero, umbrales, calidades) vive en `:domain` con tests; Android
aporta `MediaRecorder`, el servicio en primer plano, Room y la UI.

## Contexto técnico

- **Módulos:** `:domain` (`recording/`), `:app` (`recording/`, `data/db`, `data/prefs`, `tile/`, `widget/`, `ui/`).
- **Dependencias nuevas:** Room (+ KSP, solo para Room), Navigation Compose, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `material-icons-extended`. Sin Hilt (ADR-0008).
- **Formato:** `MediaRecorder` con `OutputFormat.OGG` + `AudioEncoder.OPUS` (API 29, es el minSdk), mono, 48 kHz, 32/64 kbps (FR-001-04). Pausa/reanudación con `MediaRecorder.pause()/resume()`: la versión anterior de Aura (commit `3bafe2b`) ya lo hacía así en dispositivo real.
- **Almacenamiento:** `filesDir/recordings/aura_AAAAMMDD_HHMMSS.ogg` + fila en Room (`recordings`). Ajustes no sensibles en `SharedPreferences`.
- **Tests:** `RecordingTest` (JVM). La parte Android la valida el CI (lint + compilación) y la prueba manual en un móvil.

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| I. Usable sin ayuda | Sí | Permisos explicados antes de pedirlos; el de teléfono es opcional |
| II. Sin servidores propios | Sí | Nada sale del móvil en esta feature |
| III. Fuera de tiendas / sin GMS | Sí | Solo APIs de plataforma |
| IV. Privacidad | Sí | Sin red; `READ_PHONE_STATE` solo para el estado de llamada, opcional |
| V. Nada oculto al grabar | Sí | Notificación fija no descartable + mosaico + widget + pantalla |
| VI. Nunca perder una grabación | Sí | Fila en Room al parar, antes de nada más; recuperación de ficheros huérfanos al abrir; sin borrado automático |
| VII. Simplicidad | Sí | Sin animación del widget/mosaico; ajustes mínimos (calidad) |
| X. Calidad | Parcial | Lógica en dominio con tests; el servicio y la UI no se prueban automáticamente |

## Diseño

- **`RecordingStateHolder`** (singleton en `AppContainer`): `StateFlow<RecordingStatus>` + transiciones de `RecordingTransitions`. Lo observan servicio, mosaico, widget y UI.
- **`RecordingService`** (foreground, tipo micrófono): acciones START/PAUSE/RESUME/STOP. Al arrancar abre el fichero y notifica; vigila el espacio libre cada 10 s (FR-001-12); al parar: `MediaRecorder.stop()`, si `isTooShort` borra el fichero (FR-001-11), si no inserta la fila en Room y solo entonces cambia el estado a `Idle`.
- **Llamadas** (`CallStateMonitor`): `TelephonyCallback` (API 31+) o `PhoneStateListener`; sin el permiso no hace nada (FR-001-06).
- **Recuperación**: al abrir la app, los `.ogg` de `recordings/` sin fila en Room se insertan con la duración leída con `MediaMetadataRetriever` (0 si no se puede leer); nunca se borran (Constitución VI).
- **Mosaico y widget**: leen el estado del holder; clic = alternar. Sin permiso de micrófono, el mosaico avisa con un aviso (FR-001-10).
- **UI**: una `Activity`, `NavHost` con Principal / Grabaciones / Ajustes; permisos pedidos al primer uso con diálogo explicativo previo (FR-001-10); aviso de optimización de batería una vez.
- **Reproductor**: `MediaPlayer` con reproducir/pausar y barra de posición (HU-001-4); se pasará a Media3 en la spec 004.

## Riesgos y mitigaciones

- `MediaRecorder.pause()` con OGG/Opus en algunos fabricantes → capturado con `runCatching` (como en Aura); si falla, la grabación sigue (no se pausa) y se anota en el log.
- Fabricantes que matan servicios → aviso de batería + `START_NOT_STICKY` con fichero ya en disco, recuperable.
- Android 14+: servicio de micrófono desde mosaico/widget requiere interacción del usuario (lo es).

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-001-01, 02 | `RecordingService` | Manual |
| FR-001-03 | `AuraTileService`, `AuraWidgetProvider` | Manual |
| FR-001-04 | `AudioQuality`, `AudioRecorderEngine` | `RecordingTest` (calidades, CE-001-3) |
| FR-001-05 | `RecordingTransitions`, engine | `RecordingTest` (estado); manual (fichero) |
| FR-001-06 | `CallStateMonitor` | Manual |
| FR-001-07 | `RecordingService.stop` + Room | Manual |
| FR-001-08 | `RecordingNaming` | `RecordingTest` |
| FR-001-09 | `RecordingsScreen` | Manual |
| FR-001-10 | `ui/permissions` | Manual |
| FR-001-11 | `RecordingPolicy.isTooShort` | `RecordingTest` |
| FR-001-12 | `RecordingPolicy.isStorageLow`, servicio | `RecordingTest` (umbral) |
