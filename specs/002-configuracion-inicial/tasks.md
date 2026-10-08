# Tareas: Configuración inicial y proveedores de IA

**Spec:** `spec.md` · **Plan:** `plan.md`

## Fase 1 — Dominio (tramo Gemini)

- [x] T002-01 [P] Tests: `ApiKeys`, `GeminiClient.checkCredential`, `SecretBox`, política de envío (FR-002-03, 05; FR-003-13)
- [x] T002-02 `ApiKeys`, `GeminiClient.checkCredential`, `SecretBox`, `ProcessingMode`, `ProviderDefaults`

## Fase 2 — Android

- [x] T002-03 `CredentialStore` (Keystore + `SecretBox`) y test de reglas de copia (FR-002-05)
- [x] T002-04 `ProviderController` y ajustes (modo, estado de la credencial, aviso aceptado, idiomas)
- [x] T002-05 `ProviderSetup`: modo, pasos de Gemini, portapapeles, comprobar, aviso de privacidad, desconectar (FR-002-03, 08, 09; HU-002-4)
- [x] T002-06 Asistente de primer arranque con "Saltar" y selección de idiomas (FR-002-01, 06)
- [x] T002-07 Ajustes → Procesado (FR-002-09)
- [x] T002-08 Cadenas es/gl/en (FR-002-10)

## Fase 3 — Pendiente

- [x] T002-09 Aviso discreto en la pantalla principal si se saltó la configuración (HU-002-1, escenario 4)
- [x] T002-10 Modelos de transcripción y redacción editables en Ajustes → Procesado → Avanzado (FR-002-07)
- [ ] T002-11 Idioma de la interfaz elegible en Ajustes (FR-002-10)
- [-] T002-12 OpenRouter con OAuth PKCE — **descartado** (2026-10-08): es de pago por uso
- [ ] T002-13 **Autor:** prueba manual con una clave real: `scripts/probe-gemini.sh list-models`, y el asistente en el móvil
- [ ] T002-14 Marcar la spec como "Implementada" (cuando estén T002-09…13)
- [x] T002-15 Modelo elegido de lo que la clave realmente tiene (alias `gemini-flash-latest` o el flash estable más nuevo) y cambio automático si el modelo se retira (hallazgo de la beta.4: `gemini-2.5-flash` ya no estaba disponible)
