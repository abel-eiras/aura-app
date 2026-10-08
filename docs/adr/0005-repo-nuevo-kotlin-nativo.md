# ADR-0005: Repositorio nuevo, Kotlin nativo reutilizando Aura

**Estado:** Aceptada · **Fecha:** 2026-10-06

## Contexto

Aura (proyecto anterior, repositorio privado) es una app Android de ~3.100 líneas en
Kotlin/Compose con buena base de grabación (servicio en primer plano,
mosaico, widget, pausa en llamadas). Su capa de datos está construida
alrededor de Drive, que desaparece (ADR-0004).

## Decisión

- **Repositorio nuevo `abel-eiras/aura-app`**, desarrollado con SDD desde
  el principio. Se **porta** de Aura el código de grabación, tema, widget,
  mosaico y localización, adaptado a las specs; no se hace fork del historial.
- **applicationId nuevo: `io.github.abeleiras.aura`**. Un dominio que el
  autor controla (GitHub Pages) y distinto de `com.aura.app`, que además de
  ser genérico está firmado con claves locales: la app nueva convive con la
  antigua y su primera versión no choca con instalaciones previas.
- **minSdk 29 (Android 10)**: garantiza Opus/OGG nativo en `MediaRecorder`
  y elimina la rama AAC/M4A de Aura. Android 8-9 es hoy residual.
- Stack: Kotlin, Jetpack Compose (Material 3), Hilt, Room, WorkManager,
  OkHttp + kotlinx.serialization. Sin Retrofit si OkHttp basta (Constitución VII).
- `androidx.security:security-crypto` está deprecada: las credenciales se
  cifran con una clave AES-GCM del Android Keystore (detalle en el plan de 002).

## Consecuencias

- El repositorio Aura original queda como está (archivado o con un aviso
  apuntando a aura-app cuando la v1 esté lista).
- aura-transcribe sigue siendo un proyecto independiente, compatible por
  contratos (Constitución VIII).
