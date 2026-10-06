# Cómo se trabaja en este repositorio

Instrucciones para cualquier persona o agente de IA que contribuya. Este
proyecto sigue **Spec-Driven Development (SDD)**; lee primero la
[constitución](.specify/memory/constitution.md).

## Reglas

1. **Ningún código sin especificación aprobada.** Si te piden algo que no
   está en `specs/`, el primer entregable es la spec (desde
   `.specify/templates/spec-template.md`), no el código.
2. **Flujo por feature:** `spec.md` (Aprobada) → `plan.md` → `tasks.md` →
   implementación → tests → marcar la spec como "Implementada".
   - `plan.md` sale de `.specify/templates/plan-template.md`, debe rellenar
     la tabla "Comprobación de la constitución" y apoyarse en
     [docs/arquitectura.md](docs/arquitectura.md).
   - `tasks.md` sale de `.specify/templates/tasks-template.md`; cada tarea
     cita los `FR-…` que cubre.
3. **Trazabilidad:** cada test nombra el requisito que verifica (en el
   nombre o en un comentario: `// FR-003-07`). Cada PR lista los requisitos
   que implementa.
4. **La spec manda.** Si al implementar descubres que la spec está mal o
   incompleta, corrígela en el mismo PR y explícalo; no implementes otra
   cosa en silencio. Las "Preguntas abiertas" se resuelven en el plan (con
   un spike si hace falta) y se reflejan en la spec.
5. **Decisiones de arquitectura** → `docs/adr/NNNN-titulo.md`.
6. **Nunca:** añadir Google Play Services, Firebase, analíticas o crash
   reporting remoto; añadir un backend propio; escribir credenciales en
   logs; borrar audio del usuario en una ruta de error.

## Convenciones

- Documentación y specs en español. Código, identificadores y commits en
  inglés (como en Aura). Cadenas de UI siempre en recursos, en es/gl/en.
- Kotlin con el estilo oficial; Compose con estado elevado a ViewModels.
- Ramas `feat/NNN-descripcion`, `fix/…`, `spec/…`. PRs pequeños, uno por
  bloque de tareas.

## Comandos (cuando exista el proyecto Android)

```bash
./gradlew lint testDebugUnitTest      # lo que corre CI
./gradlew assembleDebug               # APK de desarrollo
```
