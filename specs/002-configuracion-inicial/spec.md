# Especificación: Configuración inicial y proveedores de IA

**ID:** 002 · **Estado:** Aprobada · **Creada:** 2026-10-06
**Depende de:** —

## Contexto

Hoy, para usar Aura + aura-transcribe hace falta un proyecto de Google Cloud,
un PC con Linux y GPU, un token de Hugging Face y una clave de OpenRouter en
un fichero. Para alguien no técnico eso es imposible.

Esta feature define el **primer arranque** y la gestión de **proveedores de
IA**: la persona elige cómo quiere que se procesen sus grabaciones y lo deja
listo en un par de minutos, sin salir de la app más que para conseguir una
clave o iniciar sesión.

Hay tres modos de procesado:

| Modo | Para quién | Qué necesita |
|---|---|---|
| **Google Gemini** (recomendado) | La mayoría. Gratis dentro de los límites de la capa gratuita. | Una clave de Google AI Studio (sin tarjeta). |
| **OpenRouter** | Quien prefiera pagar por uso y elegir modelo (Claude, GPT, Gemini…). | Iniciar sesión en OpenRouter (OAuth, sin copiar claves) y tener créditos. |
| **Solo grabar** | Quien procese en su PC con aura-transcribe o no quiera IA. | Nada. |

No es posible usar las suscripciones de consumo (Claude Pro, ChatGPT Plus,
Gemini Advanced) desde una app de terceros de forma legítima; ver
"Fuera de alcance" y el botón "Abrir en…" de la feature 004.

## Historias de usuario

### HU-002-1 — Primer arranque guiado (P1)

Como persona no técnica que acaba de instalar la app, quiero que me guíe en
pocos pasos hasta poder grabar y procesar, para no tener que leer ninguna
documentación.

**Prueba independiente:** una persona que nunca ha usado la app completa la
configuración con Gemini sin ayuda en menos de 5 minutos.

**Escenarios de aceptación:**

1. **Dado** que abro la app por primera vez, **cuando** arranca,
   **entonces** veo una bienvenida de una pantalla que explica qué hace la
   app en tres frases y un botón "Empezar".
2. **Dado** que pulso "Empezar", **cuando** llego a "¿Cómo quieres procesar
   tus grabaciones?", **entonces** veo los tres modos con una línea de
   explicación cada uno, Gemini marcado como recomendado.
3. **Dado** que elijo un modo y completo su paso, **cuando** termino,
   **entonces** elijo idioma principal (preseleccionado el del sistema) y,
   opcionalmente, un segundo idioma, y llego a la pantalla de grabar.
4. **Dado** que pulso "Saltar" en cualquier paso, **cuando** llego a la
   pantalla principal, **entonces** la app queda en modo "Solo grabar" y un
   aviso discreto me ofrece configurar el procesado más tarde.

### HU-002-2 — Configurar Gemini con una clave (P1)

Como usuario, quiero que la app me lleve a conseguir la clave y la compruebe,
para no equivocarme.

**Escenarios de aceptación:**

1. **Dado** que elijo Gemini, **cuando** veo el paso, **entonces** hay tres
   instrucciones numeradas con capturas ("Abre AI Studio → pulsa Crear clave
   → cópiala") y un botón que abre la página de claves en el navegador.
2. **Dado** que he copiado una clave, **cuando** vuelvo a la app,
   **entonces** la app detecta una clave con formato válido en el
   portapapeles y ofrece pegarla con un toque.
3. **Dado** que pego una clave, **cuando** pulso "Comprobar", **entonces** la
   app hace una llamada mínima al proveedor y me dice "Clave correcta" o un
   error accionable (clave inválida, sin conexión, API no disponible en tu
   país…).
4. **Dado** que la clave es correcta, **cuando** continúo, **entonces** veo el
   aviso de privacidad del proveedor (ver FR-002-08) y lo acepto o vuelvo.

### HU-002-3 — Conectar OpenRouter sin copiar claves (P2)

Como usuario de OpenRouter, quiero iniciar sesión y volver a la app ya
conectado, para no manejar claves.

**Escenarios de aceptación:**

1. **Dado** que elijo OpenRouter, **cuando** pulso "Conectar",
   **entonces** se abre el navegador en la página de autorización de
   OpenRouter.
2. **Dado** que autorizo, **cuando** el navegador vuelve a la app,
   **entonces** la app obtiene una clave a mi nombre, la guarda cifrada y
   muestra "Conectado" con mi saldo si el proveedor lo expone.
3. **Dado** que cancelo o falla la autorización, **cuando** vuelvo,
   **entonces** la app lo explica y permite reintentar o elegir otro modo.
4. **Dado** que estoy conectado, **cuando** abro Ajustes → Proveedor,
   **entonces** puedo elegir el modelo de transcripción y el de redacción de
   una lista corta recomendada, con opción "Otro" (identificador libre) en
   Avanzado.

### HU-002-4 — Cambiar o quitar el proveedor más tarde (P2)

Como usuario, quiero cambiar de modo, rotar mi clave o desconectarme desde
Ajustes, para tener el control.

**Escenarios de aceptación:**

1. **Dado** que tengo un proveedor configurado, **cuando** voy a Ajustes →
   Procesado, **entonces** veo el modo actual, su estado ("Clave correcta,
   comprobada hoy") y botones para cambiar, comprobar y desconectar.
2. **Dado** que desconecto, **cuando** confirmo, **entonces** la credencial se
   borra del dispositivo y la app pasa a "Solo grabar".

## Casos límite

- Clave revocada o caducada después de configurarla: el siguiente procesado
  falla con un error accionable que lleva a Ajustes → Procesado.
- Sin conexión durante la configuración: se puede guardar la clave sin
  comprobar, marcada como "sin comprobar".
- El proveedor no está disponible en el país del usuario: mensaje claro y
  sugerencia del otro proveedor.
- Copia de seguridad / migración de móvil: las credenciales NO viajan en las
  copias de seguridad; tras restaurar, la app pide reconfigurar el proveedor.

## Requisitos funcionales

- **FR-002-01**: En el primer arranque la app DEBE mostrar un asistente:
  bienvenida → modo de procesado → paso del proveedor → idioma(s) →
  pantalla principal. Cada paso DEBE poder saltarse.
- **FR-002-02**: La app DEBE soportar exactamente tres modos de procesado:
  Gemini (clave), OpenRouter (OAuth PKCE) y Solo grabar.
- **FR-002-03**: Para Gemini, la app DEBE ofrecer un botón que abra la
  página de creación de claves, detectar en el portapapeles una cadena con
  el formato de clave al volver, y validar la clave con una llamada mínima.
- **FR-002-04**: Para OpenRouter, la app DEBE implementar el flujo OAuth PKCE
  (S256) del proveedor y recibir el retorno mediante un enlace que abra la
  app; DEBE guardar la clave resultante y nunca mostrarla completa.
- **FR-002-05**: Las credenciales DEBEN almacenarse cifradas con una clave
  del Android Keystore, excluidas de las copias de seguridad y de cualquier
  exportación o log. (Constitución IV)
- **FR-002-06**: La app DEBE permitir elegir idioma principal (por defecto
  el del sistema) y un segundo idioma opcional; ambos se usan como pista en
  el procesado (003).
- **FR-002-07**: Cada proveedor DEBE tener un modelo de transcripción y uno
  de redacción por defecto, definidos en un único lugar del código y
  modificables por el usuario en Ajustes → Avanzado.
- **FR-002-08**: Antes del primer envío a un proveedor, la app DEBE mostrar
  un aviso de privacidad específico de ese proveedor: qué se envía (audio y
  texto de las grabaciones que se procesen), a quién, si puede usarse para
  entrenar (en particular la capa gratuita de Gemini), y un enlace a su
  política. Sin aceptación explícita no se envía nada.
- **FR-002-09**: Ajustes → Procesado DEBE mostrar el modo y el estado de la
  credencial, y permitir comprobar, cambiar y desconectar.
- **FR-002-10**: La interfaz de la app DEBE estar disponible en español,
  gallego e inglés, siguiendo el idioma del sistema o uno elegido en Ajustes.

## Entidades clave

- **Proveedor**: modo (Gemini / OpenRouter / Solo grabar), credencial
  (cifrada), estado de la credencial (sin comprobar / correcta / inválida +
  fecha), modelo de transcripción, modelo de redacción, aviso de privacidad
  aceptado (sí/no + fecha).
- **Preferencias de idioma**: idioma principal, idioma secundario opcional.

## Criterios de éxito

- **CE-002-1**: 4 de cada 5 personas no técnicas completan la configuración
  con Gemini sin ayuda en menos de 5 minutos (prueba con amigos).
- **CE-002-2**: La conexión con OpenRouter requiere como máximo 3 toques
  fuera de la app.
- **CE-002-3**: Ninguna credencial aparece en logcat, ficheros exportados ni
  copias de seguridad (verificado por test).

## Fuera de alcance

- Iniciar sesión con la suscripción de consumo de Claude, ChatGPT o Gemini:
  esos logins son exclusivos de las apps de cada empresa y reutilizarlos
  viola sus condiciones. La alternativa legítima es el botón "Abrir en…"
  (feature 004).
- Proveedores adicionales (AssemblyAI, Deepgram, Ollama en red local…):
  candidatos para el futuro, no en v1.
- Procesado en el propio dispositivo: candidato para una versión futura
  (ver ROADMAP).

## Preguntas abiertas

- ¿Acepta OpenRouter un esquema propio (`aura://`) como `callback_url` del
  PKCE, o exige `https`? Si exige `https`, haría falta una página puente en
  GitHub Pages y App Links. Se resuelve con un spike al inicio del plan.
- Lista corta de modelos recomendados en OpenRouter para transcribir audio
  (tienen que aceptar audio como entrada): se fija en el plan con pruebas
  reales en español y gallego.
