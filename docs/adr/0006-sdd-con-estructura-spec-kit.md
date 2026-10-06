# ADR-0006: Desarrollo guiado por especificaciones con la estructura de Spec Kit

**Estado:** Aceptada · **Fecha:** 2026-10-06

## Contexto

El proyecto se desarrollará en buena parte con agentes de IA. Sin una
especificación estable, cada sesión reinterpreta qué hay que hacer.

## Decisión

Se sigue la estructura de [GitHub Spec Kit](https://github.com/github/spec-kit)
para que sea compatible con sus comandos si se quieren usar, pero sin
depender de ellos:

```
.specify/memory/constitution.md   principios no negociables
.specify/templates/               plantillas de spec, plan y tareas
specs/NNN-nombre/spec.md          qué y por qué (aprobada antes de codificar)
specs/NNN-nombre/plan.md          cómo (se escribe al empezar la feature)
specs/NNN-nombre/tasks.md         tareas trazables a requisitos
specs/NNN-nombre/contracts/       formatos de datos y API
docs/arquitectura.md              plan técnico transversal
docs/adr/                         decisiones de arquitectura
```

Flujo: spec (aprobada) → plan (con comprobación de la constitución) →
tasks → implementación con tests → spec marcada "Implementada".

## Consecuencias

- Toda tarea, test y PR cita los IDs de requisito (`FR-003-05`).
- Cambiar el comportamiento exige cambiar antes la spec en el mismo PR.
