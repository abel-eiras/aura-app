# Especificación: Tipos de nota

**ID:** 005 · **Estado:** Aprobada · **Creada:** 2026-10-06
**Depende de:** —

## Contexto

No es lo mismo el acta de una reunión que una nota personal. aura-transcribe
ya resuelve esto con categorías configurables (`[[notas.categorias]]`): cada
una tiene un criterio para que el modelo la elija y un prompt de redacción
con las secciones de la nota. Aquí se reutiliza el mismo modelo, con los
mismos tres tipos de ejemplo, en un formato JSON común a los dos proyectos.

Los tipos vienen preconfigurados y funcionan sin tocar nada; editarlos es
opcional y está pensado para quien quiera adaptarlos.

## Historias de usuario

### HU-005-1 — Tipos útiles desde el primer momento (P1)

Como usuario nuevo, quiero que mis grabaciones salgan ya como acta de
reunión, minuta de visita o nota personal sin configurar nada.

**Prueba independiente:** con una instalación limpia, una reunión y una nota
personal grabadas acaban en tipos distintos con sus secciones.

**Escenarios de aceptación:**

1. **Dado** una instalación nueva, **cuando** proceso una grabación,
   **entonces** se clasifica en uno de los tres tipos predefinidos
   (Reunión, Visita cliente, Nota personal) y se redacta con sus secciones.
2. **Dado** que el idioma de la interfaz es gallego o inglés, **cuando** se
   instalan los tipos predefinidos, **entonces** sus etiquetas, criterios y
   prompts están en ese idioma, y la nota se redacta en el idioma de la
   grabación.

### HU-005-2 — Crear o editar un tipo de nota (P3)

Como usuario avanzado, quiero crear un tipo "Clase" o cambiar las secciones
de "Reunión", para que las notas encajen con lo que hago.

**Escenarios de aceptación:**

1. **Dado** Ajustes → Tipos de nota, **cuando** pulso "Nuevo", **entonces**
   relleno nombre, cuándo usarlo (criterio) e instrucciones de redacción, con
   un ejemplo de ayuda en cada campo, y el resto (id, carpeta, tag) se
   deriva del nombre (editable en Avanzado).
2. **Dado** un tipo, **cuando** lo edito y guardo, **entonces** las
   grabaciones nuevas lo usan y las ya procesadas no cambian salvo que las
   vuelva a redactar.
3. **Dado** un tipo, **cuando** lo borro, **entonces** las notas existentes
   conservan su contenido, y debe quedar al menos un tipo.
4. **Dado** que he estropeado los tipos, **cuando** pulso "Restaurar los
   tipos predefinidos" y confirmo, **entonces** vuelven los tres originales.

### HU-005-3 — Compartir tipos con aura-transcribe u otros usuarios (P3)

Como usuario de aura-transcribe o que quiere pasar sus tipos a un amigo,
quiero exportar e importar los tipos como un fichero.

**Escenarios de aceptación:**

1. **Dado** Ajustes → Tipos de nota, **cuando** pulso "Exportar",
   **entonces** obtengo un `categorias.json` conforme al contrato.
2. **Dado** un `categorias.json` válido, **cuando** lo importo, **entonces**
   se me muestra qué tipos se añaden o reemplazan y confirmo.
3. **Dado** un fichero no válido, **cuando** lo importo, **entonces** se
   explica qué está mal y no se modifica nada.

## Casos límite

- Dos tipos con el mismo `id`: no se permite guardar ni importar.
- El modelo devuelve un `id` que no existe: se usa el tipo marcado como
  `por_defecto` (o el primero si ninguno lo está). (FR-003-05)
- Prompt de redacción vacío: no se permite guardar.

## Requisitos funcionales

- **FR-005-01**: La app DEBE incluir los tres tipos predefinidos de
  `contracts/categorias.default.json`, traducidos a es/gl/en.
- **FR-005-02**: Los tipos DEBEN seguir el contrato
  `contracts/categorias.schema.json`, compatible campo a campo con
  `[[notas.categorias]]` de aura-transcribe.
- **FR-005-03**: La clasificación DEBE ver solo el `criterio` de cada tipo y
  la transcripción; la redacción DEBE usar el `prompt_redaccion` del tipo
  elegido como instrucción de sistema. (Igual que aura-transcribe)
- **FR-005-04**: Si un tipo define `modelo`, la redacción de ese tipo DEBE
  usar ese modelo del proveedor activo en lugar del de por defecto.
- **FR-005-05**: El usuario DEBE poder crear, editar, borrar y restaurar
  tipos; DEBE existir siempre al menos uno.
- **FR-005-06**: La app DEBE exportar e importar los tipos como
  `categorias.json`, validando contra el contrato antes de aplicar.
- **FR-005-07**: Todos los prompts predefinidos DEBEN indicar al modelo que
  redacte en el idioma de la transcripción salvo que el usuario lo cambie.

## Entidades clave

- **Tipo de nota**: id, etiqueta, carpeta, tag, criterio, prompt de
  redacción, modelo opcional, marca de "por defecto".

## Criterios de éxito

- **CE-005-1**: Un `categorias.json` exportado por la app, convertido 1:1 a
  TOML, es aceptado por aura-transcribe (y viceversa).
- **CE-005-2**: Crear un tipo nuevo lleva menos de 2 minutos usando los
  ejemplos de ayuda.

## Fuera de alcance

- Plantillas de nota con variables o campos estructurados.
- Varias notas distintas para una misma grabación.
