# Especificación: Procesado con IA (transcripción con hablantes y nota)

**ID:** 003 · **Estado:** Aprobada · **Creada:** 2026-10-06
**Depende de:** 001, 002, 005

## Contexto

Es el núcleo de lo que venden los Plaud: convertir una grabación en una
transcripción con quién dijo qué y en una nota redactada útil. En
aura-transcribe esto exige WhisperX, pyannote y una GPU; aquí se delega en
el proveedor de IA del usuario (002), que recibe el audio y devuelve la
transcripción con hablantes, y después redacta la nota según su tipo (005).

El procesado ocurre en dos pasos, como en aura-transcribe:

1. **Transcribir**: audio → segmentos `{hablante, inicio, fin, texto}`.
2. **Redactar**: texto → tipo de nota elegido (clasificación) → nota en
   Markdown con las secciones del tipo.

## Historias de usuario

### HU-003-1 — Procesar automáticamente al terminar de grabar (P1)

Como usuario, quiero que al parar la grabación la app la procese sola, para
encontrarme la nota hecha cuando la necesite.

**Prueba independiente:** grabar una conversación de 10 minutos entre dos
personas y, sin tocar nada más, obtener una nota con su transcripción.

**Escenarios de aceptación:**

1. **Dado** un proveedor configurado y "Procesar automáticamente" activado
   (por defecto), **cuando** paro una grabación, **entonces** su estado pasa
   a "En cola" y después a "Transcribiendo", "Redactando" y "Lista".
2. **Dado** que salgo de la app o bloqueo el móvil durante el procesado,
   **cuando** vuelvo, **entonces** el procesado ha continuado o se retoma
   desde el último paso completado, sin repetir lo ya hecho.
3. **Dado** que no hay conexión, **cuando** termina una grabación,
   **entonces** queda "Esperando conexión" y se procesa sola cuando vuelve.
4. **Dado** que termina el procesado con la app en segundo plano,
   **cuando** ocurre, **entonces** recibo una notificación "Nota lista:
   Reunión — 6 oct" que abre la nota.

### HU-003-2 — Procesar manualmente (P1)

Como usuario con el automático desactivado, o en modo "Solo grabar" con un
proveedor configurado después, quiero procesar las grabaciones que elija.

**Escenarios de aceptación:**

1. **Dado** una grabación sin procesar, **cuando** pulso "Procesar",
   **entonces** entra en la cola como en HU-003-1.
2. **Dado** varias grabaciones seleccionadas, **cuando** pulso "Procesar",
   **entonces** entran todas en la cola y se procesan de una en una.

### HU-003-3 — Entender y resolver un fallo (P1)

Como usuario, quiero que si algo falla me digan qué hacer, para no perder la
grabación ni la paciencia.

**Escenarios de aceptación:**

1. **Dado** un fallo transitorio (red, límite de peticiones, error 5xx),
   **cuando** ocurre, **entonces** la app reintenta sola con esperas
   crecientes hasta un máximo, mostrando "Reintentando…".
2. **Dado** un fallo permanente (clave inválida, sin saldo, cuota agotada,
   audio demasiado largo), **cuando** ocurre, **entonces** el estado pasa a
   "Error" con una frase accionable y un botón ("Revisar clave", "Reintentar
   mañana", "Exportar audio").
3. **Dado** cualquier fallo, **cuando** ocurre, **entonces** el audio queda
   intacto. (Constitución VI)

### HU-003-4 — Regenerar la nota (P2)

Como usuario, quiero volver a redactar la nota con otro tipo de nota o tras
corregir los nombres de los hablantes, sin volver a transcribir.

**Escenarios de aceptación:**

1. **Dado** una nota lista, **cuando** elijo "Cambiar tipo" y selecciono otro
   tipo, **entonces** se rehace solo el paso de redacción con ese tipo.
2. **Dado** una nota lista, **cuando** elijo "Volver a redactar",
   **entonces** se rehace la redacción usando los nombres de hablante
   actuales (ver 004).
3. **Dado** una nota lista, **cuando** elijo "Volver a transcribir" y
   confirmo el aviso de coste, **entonces** se repiten los dos pasos.

## Casos límite

- Grabación silenciosa o sin voz: el resultado es "Sin contenido" y no se
  redacta ninguna nota ni se llama al paso 2 (igual que aura-transcribe).
- Grabación más larga que el máximo procesable: se avisa antes de enviar y
  se ofrece exportar el audio en su lugar.
- La respuesta del proveedor no cumple el formato esperado: se reintenta una
  vez; si vuelve a fallar, error "El proveedor devolvió una respuesta no
  válida" con opción de reintentar.
- El proveedor clasifica en un tipo que no existe: se usa el tipo marcado
  como "por defecto" (005) y se anota.
- La grabación está en un idioma distinto de los configurados: se transcribe
  en el idioma que detecte el proveedor y se anota en la metadata.
- Cambio de proveedor con grabaciones en cola: las pendientes usan el nuevo.
- Se borra la grabación mientras se procesa: se cancela y no queda nada.

## Requisitos funcionales

- **FR-003-01**: La app DEBE procesar cada grabación en dos pasos
  persistidos por separado (transcripción y redacción), de forma que un
  fallo en el paso 2 no obligue a repetir el paso 1.
- **FR-003-02**: El paso 1 DEBE producir una transcripción conforme al
  contrato `contracts/transcripcion.schema.json` (mismo formato que
  aura-transcribe: `metadata` + `segments` + `full_text`). (Constitución VIII)
- **FR-003-03**: Los hablantes DEBEN etiquetarse de forma consistente en
  toda la grabación (`SPEAKER_00`, `SPEAKER_01`…, en orden de primera
  intervención); la UI los muestra como "Hablante 1", "Hablante 2"…
- **FR-003-04**: El paso 1 DEBE pasar al proveedor el idioma principal y el
  secundario (002) como pista, y registrar en la metadata el idioma usado.
- **FR-003-05**: El paso 2 DEBE clasificar la transcripción en uno de los
  tipos de nota configurados usando únicamente su `criterio`, y redactar la
  nota con el `prompt_redaccion` del tipo elegido. (005)
- **FR-003-06**: El procesado DEBE ejecutarse en segundo plano, sobrevivir a
  que el usuario salga de la app, esperar a tener conexión y procesar una
  grabación cada vez, en orden de llegada.
- **FR-003-07**: Los fallos transitorios DEBEN reintentarse automáticamente
  con espera exponencial hasta 5 intentos; los permanentes DEBEN detenerse
  y mostrarse con una acción sugerida. La clasificación transitorio /
  permanente se define en el plan por proveedor.
- **FR-003-08**: Antes de enviar, la app DEBE comprobar que la duración no
  supera el máximo procesable (valor inicial: 3 horas, ajustable en el
  plan tras pruebas) y avisar si lo supera.
- **FR-003-09**: Una transcripción sin texto útil DEBE marcar la grabación
  como "Sin contenido" sin invocar el paso 2.
- **FR-003-10**: La app DEBE notificar al terminar un procesado iniciado
  con la app en segundo plano, y DEBE permitir desactivar esas
  notificaciones.
- **FR-003-11**: El usuario DEBE poder regenerar solo la redacción (con otro
  tipo o con los nombres de hablante actuales) o el procesado completo.
- **FR-003-12**: "Procesar automáticamente" DEBE ser un ajuste, activado por
  defecto cuando hay proveedor configurado.
- **FR-003-13**: La app NO DEBE enviar nada a un proveedor cuyo aviso de
  privacidad no se haya aceptado (FR-002-08).
- **FR-003-14**: Los datos enviados al proveedor DEBEN limitarse al audio de
  la grabación (paso 1) y al texto de la transcripción más el prompt del tipo
  (paso 2). Ningún otro dato del dispositivo o del usuario.

## Entidades clave

- **Trabajo de procesado**: grabación, paso actual, estado (en cola /
  esperando conexión / transcribiendo / redactando / lista / sin contenido /
  error), intentos, último error (tipo + mensaje para el usuario), proveedor
  y modelos usados.
- **Transcripción**: según `contracts/transcripcion.schema.json`.
- **Nota**: tipo de nota, título, cuerpo Markdown, fecha de redacción,
  modelo usado, nombres de hablante aplicados.

## Criterios de éxito

- **CE-003-1**: Una reunión de 30 minutos con 2-3 hablantes en español se
  procesa en menos de 5 minutos con conexión normal.
- **CE-003-2**: En una muestra de 10 grabaciones reales de 2-4 hablantes, la
  persona que estuvo presente considera correcta la atribución de hablantes
  en al menos 8.
- **CE-003-3**: En una muestra de 10 grabaciones, el tipo de nota elegido
  coincide con el que habría elegido el usuario en al menos 8.
- **CE-003-4**: Ningún fallo de procesado provoca pérdida o corrupción de
  audio (test de inyección de fallos en cada paso).

## Fuera de alcance

- Transcripción en directo mientras se graba.
- Procesado en el propio dispositivo (Whisper/pyannote/Parakeet en el
  móvil): candidato para el futuro; la arquitectura DEBE permitir añadir un
  proveedor más sin cambiar el resto (se exige en el plan, no aquí).
- Traducción de la transcripción a otro idioma.
- Reconocer a una persona por su voz entre grabaciones distintas.

## Preguntas abiertas

> Hallazgos preliminares (sin verificar) en `docs/spikes/proveedores-ia.md`.

- Calidad de la diarización del proveedor en gallego: validar con
  grabaciones reales antes de dar la feature por implementada.
