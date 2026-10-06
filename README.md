# aura-app

Grabadora de reuniones y notas de voz para Android que **transcribe
separando quién habla y redacta la nota por ti**, usando tu propia cuenta de
IA. Sin servidores, sin suscripciones, sin tiendas. Software libre (MIT).

Es la evolución de [Aura](https://github.com/abel-eiras/Aura) y
[aura-transcribe](https://github.com/abel-eiras/aura-transcribe) para que
cualquiera pueda usarla sin saber de informática: instalas, pegas una clave
(o inicias sesión en OpenRouter) y grabas.

> **Estado: especificación.** Todavía no hay código ni versiones
> descargables. Este repositorio se desarrolla con *Spec-Driven
> Development*: primero se acuerda qué hace la app, luego se construye.

## Qué hará

1. **Grabar** con un toque, también con el móvil bloqueado, desde la app, el
   mosaico de Ajustes rápidos o un widget. Siempre se ve que está grabando.
2. **Transcribir con hablantes** ("Hablante 1", "Hablante 2"… a los que luego
   pones nombre) usando Google Gemini (gratis con su capa gratuita) u
   OpenRouter (pago por uso, eliges el modelo).
3. **Redactar la nota** según su tipo (acta de reunión, minuta de visita,
   nota personal… o los tuyos).
4. **Compartirla** por WhatsApp, correo, Obsidian… o exportarla sola a una
   carpeta (compatible con aura-transcribe).
5. **Actualizarse** desde la propia app, sin tienda.

Tus grabaciones se quedan en tu móvil. Solo salen hacia el proveedor de IA
que tú elijas, cuando tú lo elijas, y la app te avisa antes de qué implica.

## Documentación del proyecto

| Documento | Qué contiene |
|---|---|
| [Constitución](.specify/memory/constitution.md) | Principios no negociables |
| [Hoja de ruta](ROADMAP.md) | Hitos hasta la 1.0 |
| [Arquitectura](docs/arquitectura.md) | Plan técnico transversal |
| [Decisiones (ADR)](docs/adr/) | Por qué solo Android, sin backend, sin tiendas, sin Drive… |
| [AGENTS.md](AGENTS.md) | Cómo se trabaja en este repo (personas y agentes de IA) |

### Especificaciones

| ID | Feature | Estado |
|---|---|---|
| [001](specs/001-grabacion/spec.md) | Grabación de audio | Aprobada |
| [002](specs/002-configuracion-inicial/spec.md) | Configuración inicial y proveedores de IA | Aprobada |
| [003](specs/003-procesado-ia/spec.md) | Procesado con IA (transcripción con hablantes y nota) | Aprobada |
| [004](specs/004-notas-y-exportacion/spec.md) | Notas, hablantes y exportación | Aprobada |
| [005](specs/005-tipos-de-nota/spec.md) | Tipos de nota | Aprobada |
| [006](specs/006-releases-y-actualizaciones/spec.md) | Releases, instalación y actualizaciones fuera de tiendas | Aprobada |

## Licencia

[MIT](LICENSE).
