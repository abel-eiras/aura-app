# ADR-0008: Sin Hilt; contenedor de dependencias manual

**Estado:** Aceptada · **Fecha:** 2026-10-06 · **Modifica:** `docs/arquitectura.md` (stack)

## Contexto

Aura usa Hilt. Para aura-app, el grafo de dependencias es pequeño (estado de
grabación, base de datos, preferencias, reproductor, y más adelante proveedores
y actualizador) y la mayoría de la lógica ya está en `:domain`, sin Android.
Hilt aporta un plugin, KSP y código generado que ralentizan la compilación y
complican servicios, tiles y widgets (cada uno necesita su `@AndroidEntryPoint`).

## Decisión

- Un `AppContainer` creado por `AuraApplication` expone los singletons
  (perezosos). Servicios, `TileService`, `AppWidgetProvider` y ViewModels lo
  obtienen de `(applicationContext as AuraApplication).container`.
- Los ViewModels se crean con `viewModelFactory { initializer { … } }`.
- KSP solo se usa para Room.

## Consecuencias

- Menos dependencias y menos magia (Constitución VII); el grafo se lee de un
  vistazo.
- Hay que cablear a mano cada nueva dependencia; aceptable con este tamaño. Si
  el grafo crece mucho, se reconsidera con un ADR nuevo.
