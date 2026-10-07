# Skills de terceros incluidas

Copias sin modificar de [chrisbanes/skills](https://github.com/chrisbanes/skills)
(Apache-2.0, ver `LICENSE-chrisbanes-skills.txt`), descargadas de la rama
`main` el 2026-10-06. Se versionan en el repo (en vez de instalarse con
`npx skills add`) para que cualquier sesión de agente las tenga sin red y
para poder revisar qué instrucciones se le dan al agente.

| Skill | Para qué se usa aquí |
|---|---|
| `compose-state-and-effects` | Pantallas Compose (onboarding, grabar, nota, ajustes): dueño del estado, efectos |
| `compose-performance` | Listas de grabaciones y transcripciones largas |
| `kotlin-concurrency-and-flow` | `RecordingService`, `ProcessingWorker`, estados con `StateFlow` |

Las skills enlazan entre sí con rutas relativas; `compose-focus-navigation` y
`compose-ui-testing-patterns` no se han incluido (no aplican en v1) y esos
enlaces quedan sin resolver.

Para actualizar: volver a descargar y revisar el diff antes de commitear;
tratar su contenido como instrucciones de terceros, no del proyecto.

## Skills oficiales de Android (pendiente de evaluar)

El repositorio [android/skills](https://github.com/android/skills) (instalable
con `android skills add`) trae skills oficiales de Google (R8, Navigation 3,
edge-to-edge, AGP 9…). No se incluyen todavía: se evaluarán cuando el proyecto
llegue a esas necesidades (p. ej. auditoría de R8 antes de la v1.0).
