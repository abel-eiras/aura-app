# Plan de implementación: Informe de errores local

**Spec:** `specs/007-informe-de-errores/spec.md` · **Fecha:** 2026-10-07

## Resumen

Un `Thread.UncaughtExceptionHandler` que arranca una `Activity` en el proceso
`:crash` con la traza como extra, y un `CoroutineExceptionHandler` en el ámbito de
la aplicación. La pantalla usa `View`s clásicas (no Compose): es lo menos que puede
fallar cuando algo ya ha ido mal.

## Contexto técnico

- **Módulos:** `:app` (`crash/`), `AuraApplication`, `AppContainer`, `UpdateController`.
- **Dependencias nuevas:** ninguna.
- **Tests:** la formación del informe (recorte, campos) es lógica pura en `:domain` (`CrashReportFormat`). El handler y la pantalla se prueban a mano.

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| I. Usable sin ayuda | Sí | Mensaje en lenguaje llano, un toque para copiar o compartir |
| II. Sin servidores propios | Sí | Nada sale del móvil salvo que la persona lo comparta |
| IV. Privacidad | Sí | Sin envío automático; el informe no contiene datos personales (FR-007-03) |
| VII. Simplicidad | Sí | Sin librería de terceros, sin historial |

## Diseño

- `CrashReport.install(app)` sólo en el proceso principal (`Application.getProcessName() == packageName`): así el proceso `:crash` no instala el handler y no puede entrar en bucle (FR-007-02).
- El handler construye el informe (`CrashReportFormat.build`), inicia `CrashActivity` (`FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK`) y mata el proceso: se evita el diálogo "La app se ha detenido" duplicado.
- `AuraApplication.onCreate` no arranca trabajo de fondo en el proceso `:crash`.
- `AppContainer.appScope` lleva un `CoroutineExceptionHandler` que registra con `Log.e` (FR-007-05); `UpdateController.check` captura `Exception` (menos cancelación) (FR-007-06).

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-007-01, 02 | `CrashReport`, `CrashActivity`, manifiesto | Manual |
| FR-007-03, 07 | `CrashReportFormat` | `CrashReportFormatTest` |
| FR-007-04 | `CrashActivity` | Manual |
| FR-007-05 | `AppContainer.appScope` | Manual |
| FR-007-06 | `UpdateController.check` | Manual |
