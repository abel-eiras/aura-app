# ADR-0002: Procesado con IA usando la cuenta del usuario, sin backend

**Estado:** Aceptada · **Fecha:** 2026-10-06

## Contexto

aura-transcribe transcribe y separa hablantes en local con WhisperX +
pyannote, lo que exige un PC con GPU NVIDIA, Linux y un token de Hugging
Face, y redacta las notas vía OpenRouter con una clave en un fichero.
Nada de eso es viable para un usuario medio.

Opciones consideradas:

1. **Backend propio** (GPU serverless) — rechazada: cuesta dinero al autor,
   hay que mantenerlo y convierte al autor en encargado del tratamiento de
   audios de terceros (RGPD). Choca con la Constitución II.
2. **Procesado en el dispositivo** (Whisper/Parakeet + diarización con
   sherpa-onnx) — viable técnicamente, pero lento y costoso en batería en
   gamas medias, añade cientos de MB de modelos y mucha complejidad. Se deja
   como mejora futura.
3. **Usar la suscripción de consumo** (Claude Pro, ChatGPT Plus, Gemini
   Advanced) mediante sus logins — rechazada: esos OAuth son exclusivos de
   las apps de cada empresa; reutilizarlos en apps de terceros viola sus
   condiciones y se bloquea activamente.
4. **API del proveedor con la cuenta del usuario** — elegida.

## Decisión

- **Proveedor recomendado: Google Gemini** con clave de Google AI Studio.
  Un modelo multimodal recibe el audio y devuelve la transcripción con
  hablantes en una sola petición (sin WhisperX ni pyannote), y tiene capa
  gratuita sin tarjeta. Se avisa explícitamente de que en la capa gratuita
  Google puede usar los datos.
- **Proveedor alternativo: OpenRouter** con su flujo OAuth PKCE, que entrega
  a la app una clave a nombre del usuario sin que tenga que copiarla. Da
  acceso a muchos modelos y es el mismo servicio que ya usa aura-transcribe.
- **Modo "Solo grabar"** para quien procese en su PC o no quiera IA.
- Para quien quiera usar su suscripción, botón "Abrir en…" que comparte la
  transcripción + instrucciones a la app oficial (legítimo, sin integración).

## Consecuencias

- La calidad de la diarización depende del proveedor; es suficiente para
  2-5 hablantes y se compensa con renombrar y unir hablantes (spec 004).
- Hay que gestionar errores y cuotas de dos APIs externas.
- La arquitectura define una interfaz de proveedor para poder añadir
  procesado en dispositivo u otros proveedores sin tocar el resto.

## Actualización 2026-10-08

Se descarta OpenRouter como proveedor alternativo: es de pago por uso y el proyecto busca una vía gratuita para el usuario. El único proveedor es Gemini con la clave gratuita del usuario (AI Studio). La exportación a una carpeta de Google Drive, elegida con el selector del sistema, cubre la sincronización con gestores de notas y chatbots con conector de Drive.
