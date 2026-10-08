# Tareas: Procesado con IA

- [x] T003-01 [P] Test: política de reintentos y reanudación (FR-003-01, 07, 08)
- [x] T003-02 [P] Test: normalización de hablantes y sin contenido (FR-003-03, 09)
- [x] T003-03 [P] Test: `NoteDrafter` (FR-003-05)
- [x] T003-04 `ProcessingPolicy`, `AiProvider`, `NoteDrafter`, `Speakers.normalize`, `Transcript`
- [~] T003-05 **Spike Gemini**: documentación leída (`docs/spikes/proveedores-ia.md`) y `scripts/probe-gemini.sh` listo; **falta ejecutarlo con una clave** (¿Opus-en-OGG?, gallego, modelo por defecto)
- [-] T003-06 Spike OpenRouter — descartado con el proveedor
- [x] T003-07 Completar este plan con el diseño de los clientes (Gemini)
- [x] T003-08 `GeminiProvider` con `MockWebServer` (FR-003-02, 04, 14)
- [-] T003-09 `OpenRouterProvider` — descartado
- [x] T003-10 Persistencia de trabajos, transcripciones y notas (ficheros atómicos en vez de Room; ver el plan) y `ProcessingPipeline` (FR-003-01)
- [x] T003-11 `ProcessingWorker`, espera de red, notificación y ajustes (FR-003-06, 10, 12)
- [x] T003-12 Aviso de privacidad previo al primer envío (FR-003-13): `canSendToProvider` + `ProviderSetup`
- [x] T003-13 Regenerar redacción / cambiar tipo / transcribir de nuevo con aviso de coste (FR-003-11): `ProcessingPipeline.run(forcedType)`, `ProcessingRepository.regenerate`
- [ ] T003-14 Medir CE-003-1…4 con grabaciones reales

- [x] T003-15 Detalle técnico del error visible y copiable en la lista de grabaciones (hallazgo de la beta.1/2: el motivo no se podía leer)
- [x] T003-16 Tiempo de espera de 10 min para las llamadas al proveedor (a 60 s la transcripción expiraba y se reintentaba sin fin) y motivo visible durante los reintentos (hallazgo de la beta.3)
- [x] T003-17 Cuota agotada (429 con espera de horas): no se reintenta cada pocos minutos; se programa un reintento automático para cuando se renueve y se muestra el estado (hallazgo de la beta.5: 20 peticiones/día en la capa gratuita)
- [x] T003-18 Clasificar y redactar en una sola petición (2 por grabación en vez de 3) y espaciar las llamadas 13 s por el límite de 5/min de la capa gratuita (hallazgo de la beta.6: 20 peticiones/día por modelo)
- [x] T003-19 Modelo por defecto Flash-Lite (500 peticiones/día en la capa gratuita frente a 20 del Flash más nuevo), según los límites reales de una clave (hallazgo de la beta.7). Pendiente: medir calidad de transcripción/hablantes/gallego con Lite (T003-14)
