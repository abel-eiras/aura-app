# ADR-0003: Distribución por GitHub Releases con actualizador integrado

**Estado:** Aceptada · **Fecha:** 2026-10-06

## Contexto

El autor no quiere pagar ni depender de ninguna tienda. Aura ya publica en
GitHub Releases y tiene un comprobador de versiones que solo abre la página
de la Release.

## Decisión

- APK universal firmado, publicado automáticamente por GitHub Actions al
  subir una etiqueta `v<semver>`.
- Actualizador dentro de la app: comprueba la API de Releases, descarga,
  verifica SHA-256 y certificado de firma, e instala con `PackageInstaller`.
- Compatibilidad con Obtainium sin configuración.
- Página de instalación (README + GitHub Pages) con guía paso a paso.

## Alternativas descartadas

- Google Play (25 $ y requisitos de testers/verificación), otras tiendas
  comerciales: contra la Constitución II/III.
- F-Droid: compatible en principio, pero exige builds reproducibles y
  tiempos de publicación ajenos; queda como opción futura.

## Consecuencias

- La primera instalación requiere permitir "orígenes desconocidos": la guía
  debe ser muy clara (spec 006).
- La clave de firma es crítica; su custodia se documenta.
- Riesgo a vigilar: verificación obligatoria de desarrolladores de Android
  (ver spec 006, Riesgos).
