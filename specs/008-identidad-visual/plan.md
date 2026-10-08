# Plan de implementación: Identidad visual

**Spec:** `specs/008-identidad-visual/spec.md` · **Fecha:** 2026-10-08

## Resumen

Tema de Compose propio (paleta, tipografía, formas) más un pequeño conjunto de componentes con el estilo
("AuraButton", "AuraCard", "AuraTag", `hardShadow`) que sustituyen a los de Material en todas las pantallas.
Sin cambios de comportamiento ni de datos.

## Contexto técnico

- **Módulo:** solo `:app` (`ui/theme`, `ui/components`, recursos).
- **Dependencias nuevas:** ninguna. Fuentes en `res/font` (Familjen Grotesk es variable, se cargan los pesos con
  `FontVariation`, minSdk 29).
- **Logotipo:** PNG con transparencia convertido a máscara de un solo color; capa adaptable 432 px con la marca
  dentro de la zona segura; la misma máscara sirve de capa monocromo, de icono de notificación y de logotipo
  teñido en la app (se pinta con el color de texto del tema).
- **Widget:** cuadrado lima (reposo), morado (grabando) o gris (en pausa) con el logotipo; el fondo se cambia con
  `setBackgroundResource` desde el proveedor.

## Comprobación de la constitución

| Principio | ¿Cumple? | Notas |
|---|---|---|
| I. Usable sin ayuda | Sí | Misma estructura; estados escritos, botones grandes |
| III. Fuera de tiendas / sin GMS | Sí | Fuentes dentro del APK, no descargables (las fuentes "descargables" de Compose usan Play Services) |
| V. Nada oculto al grabar | Sí | Etiqueta de estado, cronómetro, color y icono del botón; la notificación no cambia |
| VII. Simplicidad | Sí | Sin animaciones; el tema es un solo sitio |
| X. Calidad | Parcial | Solo se valida con CI (compilación, lint, prueba de humo) y a ojo en el móvil |

## Trazabilidad

| Requisito | Componente |
|---|---|
| FR-008-01, 03 | `ui/theme/Color.kt`, `Theme.kt`, `res/values/colors.xml` |
| FR-008-02 | `ui/theme/Type.kt`, `res/font/` |
| FR-008-04 | `ui/main/MainScreen.kt` (etiqueta y cronómetro) |
| FR-008-05 | `res/mipmap-anydpi`, `res/drawable-nodpi`, `widget/` |
| FR-008-06 | `app_name`, README |
| FR-008-07 | README, constitución, ADR-0005 |
