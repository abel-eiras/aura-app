# Contrato: nota Markdown exportada

Mismo formato que generan las notas de aura-transcribe (`notas.py`), con dos
campos opcionales añadidos (`hablantes_nombres`, `generado_por`). Un lector
debe ignorar los campos que no conozca.

```markdown
---
tipo: reunion
fecha: 2026-10-06
audio_origen: "aura_20261006_101500.ogg"
json_origen: "aura_20261006_101500.json"
idioma: es
hablantes: 3
duracion_min: 42.5
tags: [aura, reunion]
hablantes_nombres: ["Ana", "Luis", "Hablante 3"]
generado_por: "aura-app 1.0.0 (gemini)"
---

# Reunión — 2026-10-06

## Asistentes
…
```

| Campo | Obligatorio | Origen |
|---|---|---|
| `tipo` | sí | `id` del tipo de nota (005) |
| `fecha` | sí | Fecha local de inicio de la grabación, `AAAA-MM-DD` |
| `audio_origen` | sí | `metadata.source_file` |
| `json_origen` | sí | Nombre del JSON exportado |
| `idioma` | sí | `metadata.language_used` |
| `hablantes` | sí | `metadata.num_speakers_detected` |
| `duracion_min` | sí | `metadata.duration_seconds / 60`, 1 decimal |
| `tags` | sí | `[aura, <tag del tipo>]` |
| `hablantes_nombres` | no | Nombres asignados, en orden de `SPEAKER_00…`; los no asignados como "Hablante N" |
| `generado_por` | no | Versión de la app y proveedor |

Título: `# <etiqueta del tipo> — <fecha>`, seguido del cuerpo devuelto por el
paso de redacción con las etiquetas de hablante ya sustituidas por sus
nombres.

Nombre del fichero: `<fecha>-<nombre del audio sin extensión>.md`, dentro de
`notas/<carpeta del tipo>/`.
