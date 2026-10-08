# Especificación: Identidad visual "Aura by Formula Farma"

**ID:** 008 · **Estado:** Aprobada (2026-10-08) · **Creada:** 2026-10-08
**Depende de:** 001, 002, 003, 004

## Contexto

La interfaz inicial usaba el estilo por defecto de Material y un logotipo provisional. El autor eligió, entre
cuatro propuestas ("Calma", "Medianoche", "Cuaderno", "Océano"), la **"Cuaderno"** (brutalista) porque se parece a
la web de Formula Farma, y pidió usar su paleta y su logotipo, y que la app pase a llamarse
**"Aura by Formula Farma"**. Esta feature no cambia ningún comportamiento: solo la apariencia y la marca.

## Historias de usuario

### HU-008-1 — Una app con identidad (P2)

Como persona que usa la app, quiero que se vea cuidada y coherente (misma marca en el icono, la bienvenida,
la pantalla principal y los ajustes), para reconocerla y fiarme de ella.

**Escenarios de aceptación:**

1. **Dado** que instalo la app, **cuando** miro el icono en el lanzador, **entonces** veo el logotipo de la
   marca sobre fondo lima, y en Android 13+ se adapta al tema de iconos del sistema.
2. **Dado** que abro la app por primera vez, **cuando** veo la bienvenida, **entonces** aparece el logotipo y
   "Aura by Formula Farma".
3. **Dado** que estoy en la pantalla principal, **cuando** grabo, **entonces** el estado ("GRABANDO", "EN PAUSA",
   "LISTO") y el cronómetro son visibles y el botón de grabar cambia de color y de icono.
4. **Dado** que el sistema está en modo oscuro, **cuando** abro la app, **entonces** se ve con la variante
   oscura de la misma paleta, igual de legible.

## Requisitos funcionales

- **FR-008-01**: La paleta DEBE ser la de la web de Formula Farma: tinta `#0D0D0D`, morado `#8E2FB8`, lima
  `#B0F425` sobre gris claro `#F7F7F7` (medidos de la web), con variante oscura.
- **FR-008-02**: La app DEBE usar Familjen Grotesk (títulos y texto) e IBM Plex Mono (etiquetas y datos),
  incluidas en el APK (licencia SIL OFL, ver `docs/licencias-de-terceros.md`); sin fuentes descargadas.
- **FR-008-03**: Esquinas rectas, bordes de 2 dp y sombra dura en los elementos elevados; no se usa el color
  dinámico del sistema.
- **FR-008-04**: El estado de grabación DEBE seguir siendo visible por texto además de por color
  (Constitución V); el contraste de texto DEBE ser de al menos 4,5:1 (3:1 en texto grande).
- **FR-008-05**: El logotipo DEBE usarse como icono adaptable (con capa monocromo), icono de notificación y del
  mosaico, widget y marca de la pantalla principal y de la bienvenida.
- **FR-008-06**: El nombre visible de la marca es "Aura by Formula Farma" (README, bienvenida, ajustes y
  pantalla principal); el identificador de la app no cambia (actualizaciones desde versiones anteriores).
- **FR-008-07**: El repositorio NO DEBE contener enlaces a los proyectos anteriores (son privados).

## Fuera de alcance

- Cambiar el nombre del paquete o del repositorio.
- Un sitio web o página de instalación (feature 006, FR-006-09).
- Animaciones; el cronómetro y el cambio de color son la única señal dinámica.

## Preguntas abiertas

- Los tonos exactos de la web se midieron sobre una captura de pantalla; si hay una guía de marca con los
  valores oficiales, se sustituyen en `ui/theme/Color.kt` y `res/values/colors.xml`.
