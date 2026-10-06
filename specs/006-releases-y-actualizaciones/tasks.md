# Tareas: Releases, instalación y actualizaciones

**Spec:** `spec.md` · **Plan:** `plan.md`

## Fase 1 — Hito 0 (pipeline)

- [x] T006-01 Versión única + `versionCode` derivado (FR-006-03)
- [x] T006-02 [P] Test: orden semver, pre-releases (`VersionsTest`)
- [x] T006-03 [P] Test: selección de actualización (`UpdateCheckerTest`) (FR-006-05)
- [x] T006-04 `Version`, `UpdateChecker`, helpers SHA-256 en `:domain`
- [x] T006-05 `release.yml` (FR-006-01, 02, 10)
- [x] T006-06 `docs/releases.md` (FR-006-11)
- [ ] T006-07 **Autor:** generar clave de firma, copias de seguridad, secretos del repo
- [ ] T006-08 Publicar `v0.1.0` y comprobar que la Release sale correcta (CE-006-1)
- [ ] T006-09 Spike verificación de desarrolladores de Android → ADR

## Fase 2 — Hito 1 (actualizador en la app)

- [ ] T006-10 Test: cliente de la API de Releases con `MockWebServer` (FR-006-04)
- [ ] T006-11 Comprobación diaria + ajustes "buscar automáticamente" / "pre-releases"
- [ ] T006-12 Descarga con progreso y verificación SHA-256 + certificado (FR-006-06)
- [ ] T006-13 Instalación con `PackageInstaller`, permiso explicado antes (FR-006-07)
- [ ] T006-14 Guarda de grabación/procesado en curso (FR-006-08)
- [ ] T006-15 Pantalla Ajustes → Acerca de y aviso discreto (HU-006-2, 3)
- [ ] T006-16 Prueba manual de actualización 0.1.0 → 0.2.0 conservando datos

## Fase 3 — Hito 3

- [ ] T006-17 Página de instalación con QR y capturas es/gl/en (FR-006-09)
