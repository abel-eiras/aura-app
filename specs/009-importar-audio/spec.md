# Especificación: Importar audio

**ID:** 009 · **Estado:** Aprobada (2026-10-12, a petición del autor) · **Creada:** 2026-10-12
**Depende de:** 001, 003, 004

## Contexto

Hay audios que no se han grabado con Aura: una nota de voz de WhatsApp, una grabación de otra app, un fichero
que alguien te pasa. El autor quiere poder **importarlos** para que se transcriban y se redacte la nota
**exactamente como si los hubiera grabado Aura**: misma lista, mismo procesado, misma nota, misma exportación.

## Historias de usuario

### HU-009-1 — Importar desde la app (P1)

Como usuario, quiero elegir uno o varios ficheros de audio de mi móvil, para que Aura los procese igual que mis
grabaciones.

**Prueba independiente:** importar una nota de voz de 2 minutos y obtener su nota sin hacer nada más.

**Escenarios de aceptación:**

1. **Dado** que estoy en la lista de grabaciones, **cuando** pulso "Importar audio", **entonces** se abre el
   selector de ficheros del sistema, y puedo elegir varios.
2. **Dado** que elijo un audio válido, **cuando** termina la importación, **entonces** aparece en la lista como
   cualquier grabación (fecha, duración, tamaño, estado), y si el procesado automático está activo y el
   proveedor está listo, se procesa solo.
3. **Dado** que elijo un fichero que no es un audio admitido o que no se puede leer, **cuando** se intenta,
   **entonces** se me explica, no se añade nada a la lista y mi fichero original no se toca.
4. **Dado** que hay una carpeta de exportación configurada, **cuando** se importa un audio, **entonces** se
   exporta igual que uno grabado (audio, transcripción y nota).

### HU-009-2 — Compartir un audio con Aura (P2)

Como usuario, quiero compartir una nota de voz desde otra app (WhatsApp, el gestor de archivos…) hacia Aura,
para importarla sin buscarla en el selector.

**Escenarios de aceptación:**

1. **Dado** que comparto un audio desde otra app, **cuando** elijo Aura en el menú de compartir, **entonces**
   se importa como en HU-009-1 y se me dice que se ha importado.

## Casos límite

- Dos audios con la misma fecha: el nombre se desambigua y no se sobrescribe nada.
- El fichero original no se mueve, ni se borra, ni se modifica nunca (Constitución VI). Aura trabaja con una
  copia.
- Poco espacio libre: se avisa y no se importa. Una copia a medias nunca aparece en la lista.
- Audios de más de 3 horas: se importan, y al procesar se avisa del límite como en cualquier grabación (003).
- La app se cierra durante la copia: no queda un fichero a medias en la lista.

## Requisitos funcionales

- **FR-009-01**: La lista de grabaciones DEBE ofrecer "Importar audio" con el selector del sistema, con
  selección múltiple.
- **FR-009-02**: Se DEBEN admitir los formatos que el proveedor acepta: ogg/oga/opus, mp3, wav, flac, aac, m4a y
  aiff. Cualquier otro se rechaza con un mensaje que lo dice.
- **FR-009-03**: Importar DEBE copiar el audio al almacenamiento de la app con el nombre
  `aura_AAAAMMDD_HHMMSS.<ext>` (FR-001-08) y registrarlo en la base de datos igual que una grabación; la fecha
  es la de modificación del fichero si el origen la da, y si no, la actual.
- **FR-009-03b**: Un fichero importado DEBE aparecer sin ninguna marca especial en el procesado, la lista, la
  nota y la exportación.
- **FR-009-04**: La copia DEBE ser atómica (fichero temporal y renombrado) y el fichero DEBE poder leerse como
  audio con duración mayor que cero antes de registrarse; si no, se descarta la copia.
- **FR-009-05**: El original NUNCA DEBE modificarse ni borrarse. (Constitución VI)
- **FR-009-06**: Tras registrar, DEBE seguir el flujo de una grabación nueva: procesado automático si procede
  (FR-003-12) y exportación (FR-004-07).
- **FR-009-07**: Aura DEBE aparecer como destino de "Compartir" para audios y aceptar uno o varios.
- **FR-009-08**: El resultado de una importación DEBE comunicarse (cuántos se importaron y por qué falló cada
  uno que no lo hizo).

## Entidades clave

- **Grabación**: la misma entidad que en 001; un audio importado es indistinguible de uno grabado.

## Criterios de éxito

- **CE-009-1**: Un audio de WhatsApp compartido con Aura tiene su nota lista sin más pasos.
- **CE-009-2**: Ninguna importación fallida deja ficheros a medias ni toca el original.

## Fuera de alcance

- Importar vídeo, o extraer el audio de un vídeo.
- Convertir formatos no admitidos por el proveedor (se rechazan).
- Recortar o editar el audio.

## Preguntas abiertas

- ¿Acepta el proveedor el audio `m4a` con el tipo `audio/aac`? **Sin verificar** con una clave real; si lo
  rechaza, el procesado de ese audio falla con un error claro y se decidirá si convertir antes de subir.
