# ADR-0004: Sin Google Play Services ni subida a Google Drive

**Estado:** Aceptada · **Fecha:** 2026-10-06

## Contexto

Aura sube cada grabación a Google Drive con Google Sign-In
(`play-services-auth`). Para compartirla, cada usuario tenía que crear su
propio proyecto de Google Cloud y cliente OAuth; centralizarlo exigiría al
autor mantener un proyecto de Google Cloud, una pantalla de consentimiento
en producción y una política de privacidad, y ata la app a Google Play
Services (no funciona en Android sin GMS ni es apta para F-Droid).

## Decisión

- Las grabaciones y notas viven en el almacenamiento interno de la app.
- Se sacan con el menú de compartir del sistema y, opcionalmente, con una
  **carpeta de exportación automática** elegida con el selector de carpetas
  del sistema (Storage Access Framework). Esa carpeta puede estar
  sincronizada por Syncthing, Nextcloud, etc., y es donde aura-transcribe
  puede recoger los audios.
- Ninguna dependencia de Google Play Services ni de SDKs propietarios.

## Consecuencias

- Desaparece el paso más difícil de la configuración actual.
- Quien quiera Drive puede usar una carpeta sincronizada por otra app, pero
  la subida directa a Drive deja de ser una función de la app.
- El flujo personal del autor (Drive → rclone → aura-transcribe) pasa a ser
  carpeta de exportación → Syncthing → aura-transcribe.
