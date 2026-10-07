# Plan de implementación: Configuración inicial y proveedores de IA

**Spec:** `specs/002-configuracion-inicial/spec.md` · **Fecha:** 2026-10-07

## Resumen

Primer tramo (hito 2): asistente de primer arranque, modo "Solo grabar" y Gemini con clave
(HU-002-1, 002-2, 002-4). OpenRouter (HU-002-3) queda para el hito 3, tras el spike del *callback*
PKCE. La lógica pura (detección de clave, clasificación de la comprobación, cifrado, política de
envío) vive en `:domain` con tests; Android aporta Keystore, preferencias y la UI.

## Contexto técnico

- **Módulos:** `:domain` (`ai/`, `secrets/`, `net/`), `:app` (`data/ai`, `ui/provider`, `ui/onboarding`).
- **Dependencias nuevas:** ninguna.
- **Credenciales (FR-002-05):** clave AES-256-GCM no exportable en Android Keystore (`aura_credentials_v1`);
  `SecretBox` (dominio, probado con claves software) sella/abre; el blob va en Base64 en el fichero de
  preferencias `secrets`, excluido de las copias de seguridad y de la transferencia entre móviles
  (`BackupRulesTest`). Si el blob no se puede abrir (otro móvil, clave perdida), se borra y se pide de nuevo.
- **Comprobación (FR-002-03):** `GET /v1beta/models` con la clave en la cabecera `x-goog-api-key` (nunca en la
  URL). 200 → válida (y lista de modelos con `generateContent`); 400/401/403 → inválida; "location is not
  supported" → región; 429 → cuota; sin respuesta → red. Sin conexión la clave se guarda "sin comprobar".
- **Modelos por defecto (FR-002-07):** `ProviderDefaults` (un solo sitio). **Sin verificar contra la API real**:
  `scripts/probe-gemini.sh list-models` lo confirma con la clave del autor.
- **Privacidad (FR-002-08, FR-003-13):** `canSendToProvider(modo, credencial, aviso aceptado)`; el aviso de
  Gemini se muestra al validar la clave y se acepta con casilla explícita. `ProviderController.keyForSending()`
  devuelve `null` si no se cumple, y es lo único que usará el procesado (003).
- **Idiomas (FR-002-06):** principal (preseleccionado el del sistema si está en la lista) y secundario opcional,
  en preferencias. La **interfaz** (FR-002-10) sigue el idioma del sistema; elegir idioma de la interfaz en
  Ajustes queda pendiente (requiere `LocaleManager`/`AppCompat`; se decide aparte).

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| I. Usable sin ayuda | Sí | Tres pasos numerados, botón que abre AI Studio, clave pegada con un toque desde el portapapeles |
| II. Sin servidores propios | Sí | La clave es del usuario y habla directamente con Google |
| III. Fuera de tiendas / sin GMS | Sí | Solo `AndroidKeyStore` y `OkHttp` |
| IV. Privacidad | Sí | Cifrado con Keystore, excluida de copias, nunca en logs ni en la URL; aviso explícito antes de enviar nada |
| V. Nada oculto | Sí | No hay envíos en esta feature salvo la comprobación pedida por el usuario |
| VI. Nunca perder una grabación | Sí | No toca audio |
| VII. Simplicidad | Sí | Un solo componente de configuración reutilizado en el asistente y en Ajustes |
| X. Calidad | Parcial | Dominio con tests; Keystore y UI solo los valida el CI y la prueba manual |

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-002-01 | `OnboardingScreen` | Manual |
| FR-002-02 | `ProcessingMode` (OpenRouter deshabilitado hasta el hito 3) | `ProcessingModeTest` |
| FR-002-03 | `ApiKeys`, `GeminiClient`, `ProviderSetup` | `ApiKeysTest`, `GeminiClientTest` |
| FR-002-05 | `SecretBox`, `CredentialStore`, reglas de copia | `SecretBoxTest`, `BackupRulesTest` |
| FR-002-06 | `OnboardingScreen` (idiomas) | Manual |
| FR-002-07 | `ProviderDefaults` | — |
| FR-002-08 | `ProviderSetup.PrivacyNotice` | Manual; `canSendToProvider` en `ProcessingModeTest` |
| FR-002-09 | `ProviderScreen` | Manual |
| FR-002-10 | recursos es/gl/en | Script de cadenas |
