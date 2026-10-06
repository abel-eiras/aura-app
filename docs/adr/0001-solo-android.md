# ADR-0001: Solo Android; iPhone fuera de alcance

**Estado:** Aceptada · **Fecha:** 2026-10-06

## Contexto

El objetivo es compartir la app con amigos no técnicos sin gastar dinero
(Constitución II). En iPhone, sin pagar el Apple Developer Program
(99 €/año) no hay TestFlight ni App Store. Las vías gratuitas son:

- **AltStore / SideStore** con el Apple ID de cada usuario: la app caduca
  cada 7 días, hay un máximo de 3 apps así instaladas, y el proceso inicial
  no es apto para alguien no técnico.
- **PWA** desde Safari: gratis y sin caducidad, pero iOS suspende la página
  al bloquear el móvil o cambiar de app, así que no puede grabar en segundo
  plano. Para una grabadora de reuniones eso la invalida.
- AltStore PAL (UE) exige igualmente una cuenta de desarrollador de pago.

## Decisión

El proyecto es **solo Android**. No se especifica, diseña ni mantiene nada
para iOS.

## Consecuencias

- Stack nativo Kotlin + Jetpack Compose, reutilizando el código de Aura; sin
  capa multiplataforma (KMP, Flutter, React Native) que añada complejidad sin
  beneficio.
- Si en el futuro cambiara la situación (p. ej. alguien aporta una cuenta de
  Apple), se reabriría con un ADR nuevo; los contratos de datos (`specs/*/contracts`)
  son agnósticos de plataforma y servirían.
