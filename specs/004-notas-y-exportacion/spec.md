# Especificación: Notas, hablantes y exportación

**ID:** 004 · **Estado:** Aprobada · **Creada:** 2026-10-06
**Depende de:** 001, 003

## Contexto

Una vez procesada, la grabación se convierte en una **nota**: un resumen
redactado según su tipo y la transcripción con hablantes. Aura ya no sube
nada a Google Drive (ver ADR-0004): las notas viven en el móvil, y el
usuario las saca de ahí cuando quiere, con el menú de compartir de Android o
con una carpeta de exportación automática. Esa carpeta, sincronizada con
Syncthing o similar, también sirve para que aura-transcribe procese los
audios en un PC.

## Historias de usuario

### HU-004-1 — Leer una nota (P1)

Como usuario, quiero abrir una grabación procesada y ver primero el resumen y
después la transcripción, para ir directo a lo importante.

**Prueba independiente:** abrir una nota procesada y navegar del resumen a la
transcripción y al audio.

**Escenarios de aceptación:**

1. **Dado** una grabación con estado "Lista", **cuando** la abro,
   **entonces** veo el título (tipo + fecha), la duración, los hablantes y la
   nota en Markdown renderizado, con una pestaña "Transcripción".
2. **Dado** que estoy en "Transcripción", **cuando** toco un segmento,
   **entonces** el audio se reproduce desde ese instante.
3. **Dado** una nota, **cuando** busco texto en la lista de grabaciones,
   **entonces** encuentro las notas cuyo resumen o transcripción lo contienen.

### HU-004-2 — Poner nombre a los hablantes (P1)

Como usuario, quiero cambiar "Hablante 1" por "Ana", para que la nota y la
transcripción sean legibles y útiles.

**Escenarios de aceptación:**

1. **Dado** una nota con hablantes, **cuando** toco un hablante y escribo un
   nombre, **entonces** el nombre se muestra en toda la transcripción.
2. **Dado** que he renombrado hablantes, **cuando** vuelvo al resumen,
   **entonces** las etiquetas de hablante que aparezcan en el resumen se
   sustituyen por los nombres, y se ofrece "Volver a redactar con los
   nombres" (FR-003-11) por si el resumen mejora.
3. **Dado** que dos etiquetas son en realidad la misma persona, **cuando**
   elijo "Unir con…" y selecciono la otra, **entonces** ambas pasan a ser el
   mismo hablante en la transcripción.

### HU-004-3 — Compartir una nota (P1)

Como usuario, quiero mandar la nota por WhatsApp, correo o a Obsidian/Notas,
para usarla donde ya trabajo.

**Escenarios de aceptación:**

1. **Dado** una nota, **cuando** pulso "Compartir", **entonces** puedo elegir
   qué compartir: "Resumen" (por defecto), "Resumen + transcripción" o
   "Audio", y se abre el menú de compartir de Android.
2. **Dado** que comparto como texto, **cuando** elijo una app, **entonces**
   recibe el Markdown como texto plano legible.
3. **Dado** una nota, **cuando** pulso "Copiar", **entonces** el resumen se
   copia al portapapeles.

### HU-004-4 — Resumir con mi suscripción de Claude o ChatGPT (P2)

Como usuario con suscripción a un asistente de IA, quiero mandarle la
transcripción con las instrucciones del tipo de nota, para usar mi
suscripción en vez de una clave.

**Escenarios de aceptación:**

1. **Dado** una grabación transcrita, **cuando** pulso "Abrir en…",
   **entonces** se copian al portapapeles las instrucciones del tipo de nota
   seguidas de la transcripción, se muestra "Copiado: pégalo en el chat" y se
   abre el menú de compartir con el mismo texto.
2. **Dado** que la transcripción es demasiado larga para compartirla como
   texto, **cuando** pulso "Abrir en…", **entonces** se comparte como fichero
   `.md`.

### HU-004-5 — Carpeta de exportación automática (P2)

Como usuario que usa Obsidian, Syncthing o aura-transcribe, quiero que cada
grabación y su nota se copien solas a una carpeta que elija, para tenerlas
fuera de la app sin hacer nada.

**Escenarios de aceptación:**

1. **Dado** que en Ajustes → Exportación elijo una carpeta del teléfono,
   **cuando** la confirmo, **entonces** la app obtiene permiso persistente
   sobre ella y muestra la ruta.
2. **Dado** una carpeta configurada y qué exportar (audio, transcripción,
   nota; por defecto las tres), **cuando** termina una grabación o un
   procesado, **entonces** los ficheros correspondientes se escriben en la
   estructura de FR-004-08.
3. **Dado** modo "Solo grabar" y exportar solo audio, **cuando** paro una
   grabación, **entonces** el audio aparece en la raíz de la carpeta, listo
   para que aura-transcribe lo recoja.
4. **Dado** que la carpeta deja de estar accesible, **cuando** falla una
   exportación, **entonces** se marca "Exportación pendiente", se avisa una
   vez, y se reintenta al volver a estar disponible o al pulsar "Reintentar".
5. **Dado** que activo la carpeta con grabaciones ya existentes,
   **cuando** confirmo, **entonces** se me pregunta si exportar también las
   anteriores.

## Casos límite

- Renombrar hablantes después de exportar: se vuelven a escribir la
  transcripción y la nota exportadas (misma ruta, sobrescritura atómica).
- Cambio de tipo de nota tras exportar: la nota se mueve a la carpeta del
  nuevo tipo (no quedan dos notas del mismo audio, como en aura-transcribe).
- Borrar una grabación en la app NO borra lo exportado (lo exportado es del
  usuario); se dice en el diálogo de confirmación.
- Nombre de fichero ya existente en la carpeta de destino que no es de Aura:
  no se sobrescribe; se añade sufijo.

## Requisitos funcionales

- **FR-004-01**: La vista de nota DEBE mostrar resumen (Markdown
  renderizado), transcripción por hablante con tiempos, y reproductor; tocar
  un segmento DEBE reproducir desde su inicio.
- **FR-004-02**: El usuario DEBE poder asignar un nombre a cada hablante y
  unir dos hablantes; los nombres se guardan con la nota (campo
  `speaker_names` del contrato de transcripción) y no alteran los segmentos
  originales salvo al unir.
- **FR-004-03**: Los nombres DEBEN aplicarse en pantalla y en todo lo que se
  comparte o exporta, sustituyendo las etiquetas `SPEAKER_NN`.
- **FR-004-04**: La lista de grabaciones DEBE permitir buscar por texto en
  resumen y transcripción, y filtrar por tipo de nota y estado.
- **FR-004-05**: "Compartir" DEBE ofrecer resumen, resumen + transcripción o
  audio mediante el menú de compartir del sistema; "Copiar" DEBE copiar el
  resumen.
- **FR-004-06**: "Abrir en…" DEBE componer instrucciones del tipo de nota +
  transcripción, copiarlo al portapapeles y compartirlo como texto o, si
  supera un tamaño umbral, como fichero `.md`.
- **FR-004-07**: La app DEBE permitir elegir una carpeta de exportación con
  el selector de carpetas del sistema, conservar el permiso entre reinicios,
  y elegir qué se exporta (audio, transcripción, nota).
- **FR-004-08**: La estructura de exportación DEBE ser compatible con
  aura-transcribe (Constitución VIII):
  - Audio: `<carpeta>/<nombre>.ogg`
  - Transcripción: `<carpeta>/transcription/<nombre>.json` (contrato 003)
  - Nota: `<carpeta>/notas/<carpeta del tipo>/<AAAA-MM-DD>-<nombre>.md`
    con el frontmatter de `contracts/nota-frontmatter.md`.
- **FR-004-09**: Las exportaciones DEBEN escribirse de forma atómica
  (fichero temporal + renombrado) y reintentarse si la carpeta no está
  disponible, sin bloquear el resto de la app.
- **FR-004-10**: Borrar una grabación en la app NO DEBE borrar ficheros
  exportados, y el diálogo de borrado DEBE decirlo.

## Entidades clave

- **Nota**: ver 003, más nombres de hablante.
- **Destino de exportación**: carpeta (URI con permiso persistente), qué se
  exporta, estado por grabación (no aplica / pendiente / exportada / error).

## Criterios de éxito

- **CE-004-1**: Renombrar 3 hablantes y compartir el resumen por WhatsApp
  lleva menos de 30 segundos.
- **CE-004-2**: Con una carpeta de exportación sincronizada por Syncthing
  con un PC, aura-transcribe procesa sin cambios los audios exportados en
  modo "Solo grabar", y Obsidian abre las notas exportadas con su
  frontmatter.

## Fuera de alcance

- Subida directa a Google Drive, Dropbox u otros servicios con cuenta: exige
  OAuth con proyectos de terceros o servicios propietarios (ADR-0004). Quien
  lo quiera puede elegir como carpeta de exportación una carpeta que ya
  sincronice otra app.
- Edición del texto de la nota dentro de la app (v1 solo lectura; se edita
  en la app de destino).
- Exportar a PDF o DOCX.
