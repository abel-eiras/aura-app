# Plan de implementación: Tipos de nota

**Spec:** `specs/005-tipos-de-nota/spec.md` · **Fecha:** 2026-10-06

## Resumen

El catálogo de tipos es un dato (`categorias.json`) con validación y lógica de
clasificación puras, todo en `:domain`. La app lo persiste en Room (hito 2) y
la UI de edición llega después de la 1.0.

## Contexto técnico

- **Módulos:** `:domain` (`notes/`), `:app` (Room + ajustes, hito 2).
- **Dependencias nuevas:** ninguna.
- **Fuente única:** `specs/005-tipos-de-nota/contracts/categorias.default.json`, copiado a recursos de `:domain` por `syncContracts`.
- **Tests:** `NoteTypeCatalogsTest`, `NoteClassifierTest` (JVM).

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| I. Usable sin ayuda | Sí | Tipos útiles por defecto; editar es opcional |
| VII. Simplicidad | Sí | Sin plantillas con variables |
| VIII. Compatibilidad | Sí | Mismos nombres de campo que `[[notas.categorias]]` |
| X. Calidad | Sí | Validación completa con tests |

## Diseño

- `NoteType` / `NoteTypeCatalog` serializables con los nombres en español del contrato (`etiqueta`, `criterio`, `prompt_redaccion`…).
- `NoteTypeCatalogs.parse` devuelve `Valid` o `Invalid(problemas)` sin resultados parciales; valida ids únicos, forma de id/tag, campos no vacíos, ≤1 predeterminado, ≥1 tipo (FR-005-05/06). Campos desconocidos → inválido.
- `NoteClassifier` replica `clasificar.py`: el modelo solo ve `id` + `criterio`; respuesta desconocida → tipo predeterminado o el primero (FR-003-05, FR-005-03).
- Los prompts predefinidos terminan con "Redacta la nota en el mismo idioma que la transcripción." (FR-005-07).

## Decisión pendiente

- **Traducción de los tipos predefinidos a gl/en (FR-005-01)**: hoy solo existe la versión en español (la de aura-transcribe). Se hará en el hito 2 junto a la instalación de tipos en Room, con los textos como recursos por idioma de la interfaz.

## Trazabilidad

| Requisito | Componente | Test |
|---|---|---|
| FR-005-01 | `NoteTypeCatalogs.defaults` | `NoteTypeCatalogsTest` (parcial: solo es) |
| FR-005-02 | `NoteType`, contrato | `…defaults are the same file as the spec contract` |
| FR-005-03 | `NoteClassifier` | `NoteClassifierTest` |
| FR-005-05/06 | `NoteTypeCatalogs.validate/parse/toJson` | `NoteTypeCatalogsTest` |
| FR-005-07 | `categorias.default.json` | `NoteTypeCatalogsTest` |
| FR-005-04 | `NoteDrafter` (`type.model ?: defaultModel`) | `NoteDrafterTest` |
