# ADR-0007: Módulo `:domain` en Kotlin puro, separado de la app Android

**Estado:** Aceptada · **Fecha:** 2026-10-06 · **Modifica:** `docs/arquitectura.md` ("un único módulo")

## Contexto

La arquitectura inicial proponía un único módulo `app`. Pero buena parte de la
lógica que más importa es independiente de Android: clasificación y redacción
de notas, hablantes, formato de exportación, política de reintentos, selección
de actualizaciones. Mezclada con el módulo Android, solo se puede probar con
el SDK completo y el plugin de Android.

## Decisión

- `:domain` es un módulo **Kotlin/JVM puro** (kotlinx.serialization y
  coroutines; nada de `android.*`). Contiene modelos, reglas y los contratos de
  proveedor (`AiProvider`).
- `:app` depende de `:domain` y aporta lo que sí es Android: servicio de
  grabación, Room, WorkManager, UI Compose, instalador de APK.
- Los contratos de `specs/*/contracts` se copian como recursos de `:domain`
  (tarea `syncContracts`), de modo que código, tests y specs leen el mismo
  fichero.
- `./gradlew -Paura.domainOnly=true :domain:test` compila y prueba el dominio
  sin descargar el SDK ni el plugin de Android.

## Consecuencias

- Tests del dominio en segundos, en cualquier máquina y en entornos de agente
  sin acceso a `dl.google.com`.
- Un módulo más que mantener; se acepta porque el límite entre "reglas" y
  "plataforma" ya existía en la capa `domain/` de la arquitectura.
- **Decisión (2026-10-06):** los clientes HTTP (releases de GitHub, y más
  adelante Gemini y OpenRouter) viven también en `:domain`, con OkHttp (que
  funciona igual en JVM y en Android) y se prueban con `MockWebServer`. `:domain`
  sigue sin depender de `android.*`.
