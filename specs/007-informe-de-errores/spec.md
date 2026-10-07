# Especificación: Informe de errores local

**ID:** 007 · **Estado:** Aprobada · **Creada:** 2026-10-07
**Depende de:** —

## Contexto

La primera versión publicada (0.1.0) se cerró nada más abrirla en un Pixel 10 Pro,
y el autor solo pudo decirlo, sin ninguna pista de por qué. Los amigos con los
que se quiere compartir la app no tienen `adb` ni Android Studio. Sin una forma
de ver qué falló, cada error es una conversación a ciegas.

Esta feature hace dos cosas: que un fallo en una tarea de fondo no cierre la app,
y que, cuando la app sí se cierre por un error, enseñe qué pasó **en el propio
móvil**, para que la persona pueda copiarlo o compartirlo si quiere. Nada se envía
automáticamente a ningún sitio (Constitución IV).

## Historias de usuario

### HU-007-1 — Ver qué falló cuando la app se cierra (P1)

Como usuario, quiero que, si la app se cierra por un error, me aparezca una
pantalla que lo explique y me deje copiar o compartir los detalles, para poder
pasárselos a quien mantiene la app.

**Prueba independiente:** forzar una excepción en el arranque y ver la pantalla.

**Escenarios de aceptación:**

1. **Dado** que la app se cierra por una excepción no controlada, **cuando** ocurre,
   **entonces** aparece una pantalla "Aura se ha cerrado por un error" con los
   detalles técnicos, y botones para Copiar, Compartir y Cerrar.
2. **Dado** que la pantalla está abierta, **cuando** pulso Copiar, **entonces** el
   informe completo queda en el portapapeles y se confirma con un aviso.
3. **Dado** que pulso Compartir, **cuando** elijo una app, **entonces** recibe el
   informe como texto plano.
4. **Dado** que el error ocurre durante el arranque, **cuando** se cierra la app,
   **entonces** la pantalla se muestra igualmente (no depende de que la app
   principal haya arrancado).

### HU-007-2 — Que un fallo de fondo no cierre la app (P1)

Como usuario, quiero que un problema en una tarea que ocurre sin que yo haga nada
(recuperar audios, exportar, comprobar actualizaciones) no cierre la app.

**Escenarios de aceptación:**

1. **Dado** que falla una tarea de fondo con una excepción inesperada, **cuando**
   ocurre, **entonces** la app sigue abierta y el error queda solo en el registro
   del sistema.
2. **Dado** que falla la comprobación de actualizaciones que pedí a mano,
   **cuando** ocurre, **entonces** Ajustes muestra un mensaje de error en vez de
   quedarse en "Buscando…".

## Casos límite

- El propio proceso de la pantalla de error falla: no debe volver a intentar
  mostrarse (evitar un bucle).
- Traza muy larga: se recorta a un tamaño razonable.
- Dos errores seguidos: se muestra el último.

## Requisitos funcionales

- **FR-007-01**: La app DEBE capturar las excepciones no controladas del proceso
  principal y mostrar una pantalla de error antes de cerrarse.
- **FR-007-02**: La pantalla de error DEBE ejecutarse en un proceso aparte, para
  mostrarse aunque el proceso principal falle al arrancar.
- **FR-007-03**: El informe DEBE incluir: fecha y hora, versión de la app,
  fabricante y modelo, versión de Android, hilo y traza completa con sus causas.
  NO DEBE incluir grabaciones, notas, credenciales ni identificadores del usuario.
- **FR-007-04**: La pantalla DEBE ofrecer Copiar, Compartir (menú de compartir del
  sistema) y Cerrar, en es/gl/en. Nada se envía sin una acción del usuario.
- **FR-007-05**: Las tareas de fondo lanzadas en el ámbito de la aplicación DEBEN
  tener un manejador de excepciones que las registre sin cerrar la app.
- **FR-007-06**: La comprobación manual de actualizaciones DEBE terminar en un
  estado de error ante cualquier excepción, nunca quedarse en "Buscando…".
- **FR-007-07**: El informe se recorta a 20.000 caracteres.

## Criterios de éxito

- **CE-007-1**: Ante un fallo provocado en el arranque, la persona ve la pantalla y
  puede copiar el informe en menos de 10 segundos.
- **CE-007-2**: Ningún dato personal aparece en un informe de ejemplo.

## Fuera de alcance

- Envío automático de informes, analíticas o cualquier telemetría (Constitución IV).
- Reintento automático tras un cierre.
- Historial de errores anteriores.
