# Constitución de aura-app

> Principios que no se negocian feature a feature. Toda especificación, plan,
> tarea y PR se contrasta contra este documento. Si algo choca con un
> principio, se cambia la constitución primero (con su ADR), no se hace una
> excepción silenciosa.

**Versión:** 1.0.0 · **Ratificada:** 2026-10-06 · **Última enmienda:** 2026-10-06

---

## I. Usable por alguien no técnico, sin ayuda

Una persona que sabe instalar una app y copiar un texto debe poder pasar de
"no tengo nada" a "tengo mi primera reunión resumida" en **menos de 10
minutos y sin ayuda**.

- Nada de terminales, ficheros de configuración, consolas de Google Cloud ni
  tokens de servicios de terceros que no sean el del proveedor de IA elegido.
- Cada paso que pida algo al usuario explica, en lenguaje llano, **para qué**
  y **de dónde lo saca**, con un botón que le lleva allí.
- Los errores se muestran como frases accionables ("Tu clave de Gemini no es
  válida. Pulsa aquí para revisarla"), nunca como trazas o códigos.

## II. Sin servidores propios, sin costes para el autor

El proyecto no opera **ninguna** infraestructura en tiempo de ejecución.

- La app habla directamente con el proveedor de IA que el usuario elija,
  usando **la cuenta del usuario** (su clave o su login OAuth). El autor no
  paga, no intermedia y no ve ningún dato.
- Lo único "en la nube" del proyecto es GitHub (repositorio, Actions,
  Releases), dentro de su capa gratuita para proyectos públicos.
- Cualquier feature que requiera un backend propio, una cuenta de pago del
  autor o una cuota en una tienda queda **fuera de alcance** por definición.

## III. Fuera de las tiendas oficiales

- Solo Android. iPhone queda fuera de alcance (ver ADR-0001).
- Distribución exclusiva por **GitHub Releases** (APK firmado) con un
  **actualizador integrado** en la app. Compatible con Obtainium.
- Sin Google Play Services ni ninguna dependencia propietaria: la app debe
  funcionar en un Android sin servicios de Google (ver ADR-0004) y no
  descartar una eventual inclusión en F-Droid.

## IV. Privacidad por defecto y honesta

- Sin telemetría, analíticas, crash reporting remoto ni publicidad. Ninguno.
- Las grabaciones, transcripciones y notas viven **en el teléfono** salvo que
  el usuario las exporte o elija procesarlas con un proveedor de IA.
- Antes de enviar audio a un proveedor por primera vez, la app dice **qué** se
  envía, **a quién**, y si ese proveedor puede usar los datos (p. ej. capa
  gratuita de Gemini). El usuario lo acepta explícitamente.
- Las credenciales se guardan cifradas con una clave del Android Keystore.
  Nunca se escriben en logs, exportaciones ni copias de seguridad.

## V. Nada oculto mientras se graba

Heredado de Aura: si se está grabando, **siempre** se ve (notificación fija,
mosaico de Ajustes rápidos, pantalla principal). No hay grabación sin
indicador, ni arranques automáticos sin acción del usuario.

## VI. Nunca perder una grabación

- Una grabación terminada se persiste en el almacenamiento interno **antes**
  de cualquier otra operación (procesado, exportación).
- Un fallo de red, de proveedor o de cuota nunca borra ni corrompe el audio:
  se marca, se explica y se puede reintentar.
- Solo el usuario borra grabaciones (salvo el límite de almacenamiento
  explícito y configurable, que avisa antes).

## VII. Simplicidad antes que opciones

- Cada ajuste nuevo debe justificar por qué no puede ser un buen valor por
  defecto. Los ajustes avanzados van en una sección "Avanzado" plegada.
- Una sola forma recomendada de hacer cada cosa. Las alternativas existen
  solo si cubren un caso que la recomendada no puede cubrir.
- Preferir APIs de la plataforma a librerías; preferir una librería a dos.

## VIII. Compatibilidad con aura-transcribe

Los ficheros que exporta la app (JSON de transcripción y notas Markdown)
siguen **el mismo formato** que produce
aura-transcribe, y los tipos
de nota usan el mismo modelo de categorías. Quien quiera procesar en su PC
puede usar la app solo como grabadora y exportar el audio a la carpeta que
vigila aura-transcribe.

## IX. Desarrollo guiado por especificaciones (SDD)

- **Ningún código sin especificación.** Todo cambio funcional parte de un
  `specs/NNN-*/spec.md` aprobado, seguido de `plan.md` y `tasks.md`.
- La especificación describe **qué y por qué** (historias, requisitos,
  criterios de aceptación), nunca el cómo. El cómo vive en `plan.md`.
- Cada requisito tiene un identificador estable (`FR-001-03`) que se cita en
  tareas, tests y PRs. Un requisito sin test que lo cubra no está terminado.
- Si al implementar se descubre que la especificación estaba mal, se corrige
  **primero la especificación** y después el código, en el mismo PR.
- Las decisiones de arquitectura se registran como ADR en `docs/adr/`.

## X. Calidad mínima exigible

- Kotlin + Jetpack Compose, una sola Activity, arquitectura por capas
  (UI → dominio → datos) como en Aura.
- Tests unitarios para toda la lógica de dominio y de datos que no dependa
  del hardware (parsers, máquina de estados del procesado, actualizador,
  exportación). Las llamadas a proveedores se testean con respuestas
  grabadas, nunca contra la API real en CI.
- CI en cada PR: compilación, lint, tests. Un PR en rojo no se fusiona.
- Interfaz en español, gallego e inglés desde el primer día (todas las
  cadenas en recursos, ninguna literal en código).
- Accesible: TalkBack funcional en las pantallas principales, tamaños
  táctiles ≥ 48dp, contraste AA.

---

## Gobernanza

- Esta constitución prevalece sobre cualquier otra práctica del repositorio.
- Las enmiendas requieren: un PR que modifique este fichero, un ADR que
  explique el porqué, y subir la versión (MAJOR si se elimina o redefine un
  principio, MINOR si se añade uno, PATCH para aclaraciones).
- Cada `plan.md` incluye una sección "Comprobación de la constitución" que
  recorre los principios y justifica cualquier tensión.
