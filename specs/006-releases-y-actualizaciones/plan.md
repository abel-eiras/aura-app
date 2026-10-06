# Plan de implementación: Releases, instalación y actualizaciones

**Spec:** `specs/006-releases-y-actualizaciones/spec.md` · **Fecha:** 2026-10-06

## Resumen

Dos mitades. (1) **Pipeline de release**: etiqueta → GitHub Actions compila,
firma, verifica y publica (hito 0, ya implementado). (2) **Actualizador en la
app**: la lógica de decisión y verificación vive en `:domain`
(`UpdateChecker`, `Version`); `:app` aporta red, descarga e instalación con
`PackageInstaller` (hito 1).

## Contexto técnico

- **Módulos afectados:** `:domain` (`update/`), `:app` (`data/update`, `ui/update`), `.github/workflows`, `docs/releases.md`.
- **Dependencias nuevas:** ninguna en hito 0. Hito 1: OkHttp para la API de GitHub y la descarga (ya prevista).
- **Almacenamiento:** APK descargado en `filesDir/updates/` (excluido de copias de seguridad); preferencias: última comprobación, "buscar automáticamente", "recibir pre-releases".
- **Tests:** `VersionsTest`, `UpdateCheckerTest` (JVM). Instalación y firma se prueban a mano en un dispositivo (no automatizable en CI).

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| I. Usable sin ayuda | Sí | Actualizar = aviso + 1 botón; el único paso técnico (permiso de instalar) se explica antes |
| II. Sin servidores propios | Sí | Solo GitHub Releases |
| III. Fuera de tiendas / sin GMS | Sí | `PackageInstaller`, sin GMS |
| IV. Privacidad | Sí | Única petición propia: API de GitHub, 1×/24 h, desactivable (FR-006-04); sin identificadores |
| VII. Simplicidad | Sí | Un APK universal, sin delta ni canales múltiples |
| X. Calidad | Sí | Lógica de decisión en dominio con tests; CI en cada PR |

## Diseño

- **Versión única** (`aura.versionName` en `gradle.properties`) → `versionName`;
  `versionCode = MAJOR*10000 + MINOR*100 + PATCH` calculado en Gradle con la
  misma fórmula que `Version.versionCode` (FR-006-03).
- **Release** (`release.yml`): comprueba etiqueta = versión (FR-006-02), tests
  + lint, `assembleRelease` firmado desde secretos, `apksigner verify`,
  `sha256sum`, publica `aura-X.apk` + `.sha256` + huella del certificado en las
  notas; `-` en la etiqueta → pre-release (FR-006-01/02/10).
- **Selección** (`UpdateChecker.findUpdate`): ignora borradores y
  pre-releases (salvo opt-in), nunca baja de versión, exige los dos assets
  con nombre `aura-<versión>.apk[.sha256]` (FR-006-05).
- **Verificación** en la app (hito 1): SHA-256 contra el `.sha256` publicado y
  comparación del certificado del APK con el de la app instalada
  (`PackageManager.getPackageArchiveInfo` con `GET_SIGNING_CERTIFICATES`) antes
  de entregar a `PackageInstaller` (FR-006-06).
- **Guardas**: no actualizar con grabación o procesado en curso (FR-006-08);
  sin descargas en segundo plano.

## Riesgos y mitigaciones

- Clave de firma perdida → copias y procedimiento en `docs/releases.md` (FR-006-11).
- Verificación de desarrolladores de Android → spike antes de la 1.0 (ver spec).
- Actions por etiqueta y no por SHA en un workflow con la clave → pendiente anotado en `docs/releases.md`.

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-006-01, 02, 10 | `release.yml` | Primera Release `v0.1.0` (manual) |
| FR-006-03 | `app/build.gradle.kts`, `Version.versionCode` | `VersionsTest` |
| FR-006-05 | `UpdateChecker.findUpdate` | `UpdateCheckerTest` |
| FR-006-06 (hash) | `UpdateChecker.sha256Hex/parseSha256File` | `UpdateCheckerTest` |
| FR-006-04, 06 (firma), 07, 08 | `:app` `data/update` | Hito 1 |
| FR-006-09 | README / GitHub Pages | Hito 3 |
| FR-006-11 | `docs/releases.md` | — |
