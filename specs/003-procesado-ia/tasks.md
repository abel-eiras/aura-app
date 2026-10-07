# Tareas: Procesado con IA

- [x] T003-01 [P] Test: política de reintentos y reanudación (FR-003-01, 07, 08)
- [x] T003-02 [P] Test: normalización de hablantes y sin contenido (FR-003-03, 09)
- [x] T003-03 [P] Test: `NoteDrafter` (FR-003-05)
- [x] T003-04 `ProcessingPolicy`, `AiProvider`, `NoteDrafter`, `Speakers.normalize`, `Transcript`
- [~] T003-05 **Spike Gemini**: documentación leída (`docs/spikes/proveedores-ia.md`) y `scripts/probe-gemini.sh` listo; **falta ejecutarlo con una clave** (¿Opus-en-OGG?, gallego, modelo por defecto)
- [~] T003-06 **Spike OpenRouter**: el *callback* apunta a `localhost`; límites de audio sin comprobar
- [x] T003-07 Completar este plan con el diseño de los clientes (Gemini)
- [x] T003-08 `GeminiProvider` con `MockWebServer` (FR-003-02, 04, 14)
- [ ] T003-09 `OpenRouterProvider`
- [x] T003-10 Persistencia de trabajos, transcripciones y notas (ficheros atómicos en vez de Room; ver el plan) y `ProcessingPipeline` (FR-003-01)
- [x] T003-11 `ProcessingWorker`, espera de red, notificación y ajustes (FR-003-06, 10, 12)
- [x] T003-12 Aviso de privacidad previo al primer envío (FR-003-13): `canSendToProvider` + `ProviderSetup`
- [ ] T003-13 Regenerar redacción/procesado completo (FR-003-11)
- [ ] T003-14 Medir CE-003-1…4 con grabaciones reales
