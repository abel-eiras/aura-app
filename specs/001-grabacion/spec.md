# Especificación: Grabación de audio

**ID:** 001 · **Estado:** Aprobada · **Creada:** 2026-10-06
**Depende de:** —

## Contexto

Es la parte que ya resuelve Aura y que funciona bien: grabar con el móvil,
de forma fiable, también con la pantalla apagada, y sin que haya ninguna duda
de cuándo se está grabando. Esta feature la conserva casi intacta, pero
desacoplada de Google Drive: una grabación termina en el almacenamiento del
teléfono y desde ahí la recogen el procesado (003) y la exportación (004).

## Historias de usuario

### HU-001-1 — Grabar una reunión con el móvil bloqueado (P1)

Como persona que asiste a una reunión, quiero empezar a grabar con un toque y
bloquear el móvil, para no tener que estar pendiente de él.

**Prueba independiente:** grabar 60 minutos con la pantalla apagada y obtener
un fichero reproducible de ~60 minutos.

**Escenarios de aceptación:**

1. **Dado** que la app tiene permiso de micrófono, **cuando** pulso el botón
   de grabar, **entonces** empieza la grabación, aparece una notificación fija
   "Aura — Grabando" con un contador, y la pantalla principal muestra el
   tiempo transcurrido.
2. **Dado** que estoy grabando, **cuando** bloqueo el móvil durante 60
   minutos y vuelvo, **entonces** la grabación sigue en curso y el contador
   refleja los 60 minutos.
3. **Dado** que estoy grabando, **cuando** pulso "Parar" (en la app, la
   notificación, el mosaico o el widget), **entonces** la grabación se guarda
   y aparece en la lista de grabaciones con su duración.

### HU-001-2 — Empezar a grabar sin abrir la app (P1)

Como usuario, quiero arrancar y parar la grabación desde Ajustes rápidos o un
widget, para no perder el comienzo de una conversación.

**Escenarios de aceptación:**

1. **Dado** que he añadido el mosaico "Aura" a Ajustes rápidos, **cuando** lo
   toco, **entonces** empieza a grabar y el mosaico se muestra activo.
2. **Dado** que tengo el widget en la pantalla de inicio, **cuando** lo toco,
   **entonces** alterna entre grabar y parar, y su aspecto refleja el estado.

### HU-001-3 — Pausar y reanudar (P2)

Como usuario, quiero pausar la grabación (y que se pause sola si entra una
llamada), para no grabar lo que no toca.

**Escenarios de aceptación:**

1. **Dado** que estoy grabando, **cuando** pulso "Pausar", **entonces** el
   contador se detiene, la notificación indica "En pausa" y al reanudar el
   audio continúa en el mismo fichero.
2. **Dado** que concedí el permiso opcional de estado del teléfono, **cuando**
   entra o empieza una llamada, **entonces** la grabación se pausa y, al
   colgar, se reanuda automáticamente.
3. **Dado** que denegué ese permiso, **cuando** entra una llamada,
   **entonces** la grabación sigue sin cambios (comportamiento documentado).

### HU-001-4 — Ver y escuchar mis grabaciones (P1)

Como usuario, quiero una lista de mis grabaciones con fecha, duración y
estado, y poder escucharlas, para saber qué tengo.

**Escenarios de aceptación:**

1. **Dado** que tengo grabaciones, **cuando** abro "Grabaciones",
   **entonces** las veo ordenadas de más reciente a más antigua con fecha,
   duración y estado de procesado (ver 003).
2. **Dado** una grabación, **cuando** pulso reproducir, **entonces** suena con
   controles de pausa y posición.
3. **Dado** una grabación, **cuando** elijo "Borrar" y confirmo,
   **entonces** se eliminan el audio y todo lo derivado (transcripción, nota).

## Casos límite

- El sistema mata la app durante una grabación: el audio grabado hasta ese
  momento debe poder recuperarse como grabación válida al volver a abrir la
  app (o, como mínimo, no dejar un fichero corrupto listado como bueno).
- Se queda sin espacio en disco mientras graba: se para la grabación, se
  conserva lo grabado y se avisa.
- Se deniega el permiso de micrófono: el botón de grabar explica por qué hace
  falta y lleva a Ajustes del sistema.
- Optimización de batería agresiva del fabricante: la app detecta el caso y
  ofrece, una vez, una explicación con un botón para desactivarla.
- Grabación accidental de menos de 1 segundo: se descarta sin listarla.

## Requisitos funcionales

- **FR-001-01**: La app DEBE grabar audio del micrófono en un servicio en
  primer plano de tipo micrófono que siga activo con la pantalla apagada o la
  app en segundo plano.
- **FR-001-02**: Mientras haya una grabación activa o en pausa, DEBE existir
  una notificación fija no descartable que lo indique, con acciones de
  pausar/reanudar y parar. (Constitución V)
- **FR-001-03**: La app DEBE ofrecer un mosaico de Ajustes rápidos y un widget
  de pantalla de inicio que inicien/paren la grabación y reflejen su estado.
- **FR-001-04**: El formato de grabación DEBE ser Opus en contenedor OGG,
  mono, con dos calidades seleccionables: "Normal" (~32 kbps) y "Alta"
  (~64 kbps); por defecto "Normal".
- **FR-001-05**: La app DEBE permitir pausar y reanudar dentro del mismo
  fichero.
- **FR-001-06**: Con el permiso opcional de estado del teléfono concedido, la
  app DEBE pausar al entrar/empezar una llamada y reanudar al terminar.
- **FR-001-07**: Al parar, la grabación DEBE quedar persistida en el
  almacenamiento interno de la app y registrada en la base de datos local
  antes de que se dispare cualquier otra acción. (Constitución VI)
- **FR-001-08**: Los nombres de fichero DEBEN seguir el patrón
  `aura_AAAAMMDD_HHMMSS.ogg` (hora local de inicio), compatible con la
  detección de fecha de aura-transcribe.
- **FR-001-09**: La app DEBE listar las grabaciones con fecha, duración,
  tamaño y estado, permitir reproducirlas y borrarlas con confirmación.
- **FR-001-10**: La app DEBE pedir los permisos (micrófono, notificaciones,
  estado del teléfono) justo cuando se necesitan, con una explicación en
  lenguaje llano antes del diálogo del sistema; el de estado del teléfono es
  opcional y su denegación no bloquea nada.
- **FR-001-11**: Grabaciones de menos de 1 segundo DEBEN descartarse.
- **FR-001-12**: Si el almacenamiento libre del dispositivo baja de un umbral
  durante una grabación, la app DEBE pararla, conservar lo grabado y avisar.

## Entidades clave

- **Grabación**: fichero de audio + fecha/hora de inicio, duración, tamaño,
  calidad, estado de procesado (ver 003) y estado de exportación (ver 004).

## Criterios de éxito

- **CE-001-1**: 60 minutos de grabación con pantalla apagada en un Pixel y en
  un Samsung de gama media producen un fichero completo y reproducible.
- **CE-001-2**: Desde el mosaico, la grabación arranca en menos de 1 segundo.
- **CE-001-3**: Una hora en calidad "Normal" ocupa ≤ 20 MB.

## Fuera de alcance

- Grabar llamadas telefónicas (Android no lo permite a apps normales).
- Grabación por voz/automática o programada.
- Editar el audio (recortar, unir).

## Preguntas abiertas

- ¿Mantener el límite de almacenamiento local de Aura (borrar las más
  antiguas)? Propuesta: ya no tiene sentido como límite automático, porque
  las grabaciones no son "pendientes de subir" sino el archivo del usuario;
  sustituirlo por un aviso cuando ocupen más de X GB. Decidir en el plan.
