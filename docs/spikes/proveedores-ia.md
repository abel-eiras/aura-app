# Spikes de proveedores de IA (specs 002 y 003)

**Fecha:** 2026-10-07 · **Estado:** hallazgos por documentación **sin verificar** contra la fuente
primaria (los dominios `ai.google.dev` y `openrouter.ai` estaban bloqueados en la sesión de desarrollo;
solo se pudo leer resúmenes de búsqueda). Lo dudoso se comprueba con `scripts/probe-gemini.sh`.

## Gemini

| Pregunta | Hallazgo | Confianza |
|---|---|---|
| Formatos de audio | WAV, MP3, AIFF, AAC, **OGG (`audio/ogg`)**, FLAC | Media |
| ¿Opus dentro de OGG? | La lista dice "OGG **Vorbis**". Aura graba **Opus** en OGG. Puede aceptarse o no | **Baja: probar** |
| Duración máxima por petición | 9,5 horas | Media |
| Files API frente a inline | Files API cuando el total de la petición supera 20 MB | Media |
| Diarización y marcas de tiempo | Hay un modelo de transcripción dedicado ("Gemini 3.5 Transcribe") con identificación de idioma, hablantes (hasta 8; con 3 o más es experimental) y marcas de tiempo por palabra | Media: el nombre exacto del modelo y si lo ve una clave gratuita |
| Calidad en gallego | Desconocida | **Probar** |

### Consecuencias para el diseño (003)

- **Subir con Files API siempre**: 1 h de audio a 32 kbps son ~14 MB, pero 2 h ya pasan los 20 MB.
- **Plan B si Gemini no acepta Opus-en-OGG**: convertir en el móvil a un formato admitido antes de subir
  (decodificar con `MediaCodec` y empaquetar WAV o FLAC). Pesa más (WAV 16 kHz mono ≈ 115 MB/h), pero la
  Files API lo admite. Decidir **solo** si la prueba falla.
- **Modelo configurable** (spec 002, FR-002-07): los identificadores de modelo cambian con frecuencia.
  Nada de modelos fijos en el código salvo una constante por defecto y la lista que devuelve la API.
- Esquema JSON de salida con `speaker`, `start`, `end`, `text` y normalización posterior (`Speakers.normalize`),
  porque el modelo puede etiquetar `spk_1` o `SPEAKER_1` en vez de `SPEAKER_00`.

### Cómo verificarlo (5 minutos, con tu clave)

```bash
export GEMINI_API_KEY=...                       # https://aistudio.google.com/apikey
scripts/probe-gemini.sh list-models             # qué modelos ve tu clave
scripts/probe-gemini.sh transcribe grabacion.ogg gemini-2.5-flash   # una grabación de Aura, exportada o compartida
```
Mira: si responde `HTTP 200` con segmentos (formato aceptado), cuántos hablantes distingue, si los tiempos
cubren la grabación, y el tiempo que tarda. Con una grabación en gallego, si el texto es correcto.

## OpenRouter (login OAuth PKCE)

| Pregunta | Hallazgo | Confianza |
|---|---|---|
| Flujo | `https://openrouter.ai/auth?callback_url=…&code_challenge=…&code_challenge_method=S256`; vuelve con `?code=`; se canjea con `POST https://openrouter.ai/api/v1/auth/keys` (`code`, `code_verifier`, `code_challenge_method`) y devuelve una clave `sk-or-…` | Media |
| ¿Esquema propio (`aura://`) como `callback_url`? | **No consta.** La documentación habla de sitios web y de `http://localhost:<puerto>/callback` (cualquier puerto) para apps nativas | Media |

### Consecuencias para el diseño (002, HU-002-3)

- Probable solución para Android: un servidor local mínimo que escuche en `127.0.0.1:<puerto libre>`
  mientras el navegador autoriza, y recoja el `code` del redirect. Sin hosting propio y sin App Links.
- Alternativa: una página puente en GitHub Pages con enlace de aplicación verificado. Más piezas; solo
  si lo anterior no funcionara.
- OpenRouter queda **detrás de Gemini** en prioridad (la spec ya lo marca como P2).

## Pendiente de comprobar (necesita una persona con clave)

- [ ] ¿Acepta Gemini Opus-en-OGG? → decide si hace falta el Plan B de conversión
- [ ] Calidad de la diarización y del gallego con grabaciones reales (CE-003-2)
- [ ] ¿Qué modelo usar por defecto? ¿Lo ve una clave gratuita?
- [ ] OpenRouter: ¿admite `http://localhost:<puerto>/callback`? ¿Y `aura://`?
