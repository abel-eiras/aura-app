# Hoja de ruta

Cada hito es usable por sí mismo. Una feature entra en un hito cuando su
`spec.md` está aprobada; antes de codificarla se escriben su `plan.md` y su
`tasks.md` (ver [AGENTS.md](AGENTS.md)).

## Hito 0 — Esqueleto (v0.1.0)

Objetivo: el proceso de release funciona de punta a punta antes de que haya
funciones.

- [x] Proyecto Android (`io.github.abeleiras.aura`, minSdk 29) con Compose,
      cadenas es/gl/en y `gradle-wrapper.jar` versionado. (Hilt y Room entran con
      la primera feature que los necesite.)
- [x] Módulo `:domain` en Kotlin puro con la lógica de 003/004/005/006 y sus
      tests (ADR-0007).
- [x] CI en PRs.
- [x] Clave de firma de release generada y custodiada; `docs/releases.md` escrito. **`v0.1.0` publicada.**
- [x] Workflow de release con etiqueta (FR-006-01…03).
- [ ] Spike: estado de la verificación de desarrolladores de Android → ADR.

## Hito 1 — Grabadora (v0.2.0)

- [x] **001 Grabación** completa (portada de Aura, sin Drive). *Código listo y CI en verde; falta la prueba manual en móvil (T001-18).*
- [x] **006** Comprobación de versión + actualización con un toque
      (FR-006-04…08). *Código listo y CI en verde; falta probar 0.1.0 → 0.2.0 en un móvil (T006-16).* A partir de aquí, los amigos-testers se actualizan solos.
- [x] **004** HU-004-3 (compartir audio) y HU-004-5 (carpeta de exportación,
      solo audio) → el autor ya puede sustituir Aura + Drive por
      aura-app + Syncthing + aura-transcribe. *Código listo y CI en verde; falta la prueba manual con Syncthing (T004-13/14).*

- [x] **Prueba de humo en CI con emulador** (T006-18): la 0.1.0 se publicó sin que nadie
      la hubiera ejecutado y se cerraba al abrir (faltaba `INTERNET`). Ahora cada commit abre
      el APK release en un emulador.

## Hito 2 — Notas con IA (v0.5.0)

- [ ] Spikes: callback PKCE de OpenRouter; límites de audio en OpenRouter;
      calidad de diarización de Gemini en es y gl.
- [ ] **002** Configuración inicial con Gemini y "Solo grabar". *Asistente, clave cifrada, comprobación, aviso y modelos avanzados listos; falta la prueba con una clave real (T002-13) y el idioma de la interfaz.*
- [ ] **005** Tipos predefinidos (HU-005-1).
- [ ] **003** Procesado completo con Gemini.
- [ ] **004** Vista de nota, renombrar/unir hablantes, compartir, búsqueda,
      exportación de transcripción y nota.

## Hito 3 — Para amigos (v1.0.0)

- [ ] **002** OpenRouter (HU-002-3).
- [ ] **004** "Abrir en…" (HU-004-4).
- [ ] **006** Página de instalación con QR y guía con capturas (FR-006-09).
- [ ] Prueba con 5 personas no técnicas contra CE-002-1 y CE-006-1…2.
- [ ] Repositorio Aura original: aviso apuntando a aura-app.

## Después de la 1.0 (sin spec todavía)

- **005** Editor de tipos de nota e importación/exportación (HU-005-2/3).
- Procesado en el dispositivo (sin conexión ni cuenta), como proveedor nuevo.
- Otros proveedores (AssemblyAI, Deepgram, Ollama en la red local).
- Builds reproducibles y F-Droid.
