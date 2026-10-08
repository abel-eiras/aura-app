<p align="center"><img src="docs/assets/logo.png" alt="Logo de Aura" width="120"></p>

# Aura

**Tú grabas. Aura apunta lo importante.**

Aura graba tus reuniones, visitas y notas de voz con el móvil, **separa quién dice qué** y te deja la **nota ya redactada**: el acta de la reunión, la minuta de la visita, lo que tengas que recordar. Tú solo te preocupas de pulsar grabar.

Funciona **en tu móvil, no en la nube de nadie**: no hay servidor mío, ni cuenta que crear, ni cuotas. Para transcribir usas tu propia clave gratuita de Google AI Studio (sin tarjeta). Y las grabaciones solo salen del móvil hacia ese servicio, cuando tú lo activas y después de avisarte de lo que implica.

Gratis. Y libre, código incluido. Sin letra pequeña, sin versión de pago escondida, sin "en beta gratis y luego pagas".

## 🤔 Por qué existe esto

Es un proyecto personal que forma parte de [Formula Farma](https://formulafarma.com/), bajo el mismo paraguas (y el mismo pretexto) que el resto de sus soluciones de software: la herramienta que no encontraba hecha, así que me la construí.

Empezó como una grabadora y un transcriptor para PC que solo yo sabía usar, porque exigían un ordenador con tarjeta gráfica y un montón de claves. Aura es lo mismo, pero para que cualquiera lo use: instalas, pegas una clave y grabas.

## ✨ Qué puedes hacer

### Grabar
- Un toque para empezar y otro para parar, **también con el móvil bloqueado**.
- Desde la app, desde el mosaico de Ajustes rápidos o desde un widget en la pantalla de inicio.
- Siempre se ve que está grabando: etiqueta, cronómetro y notificación fija.
- Pausa y reanudación, y pausa automática si entra una llamada (si lo permites).
- Si el móvil se queda sin espacio, o la app se cierra por sorpresa, **no pierdes lo grabado**.

### Transcribir y redactar
- La transcripción separa a las personas ("Hablante 1", "Hablante 2"…), y luego les pones nombre o unes dos que en realidad eran la misma.
- La nota se redacta según su tipo: **reunión**, **visita de cliente** o **nota personal**, en castellano, gallego o inglés.
- Todo ocurre solo, en segundo plano, al terminar de grabar. Te avisa cuando la nota está lista. Si no hay conexión o se agota la cuota del día, **espera y lo reintenta sola**.
- ¿No te convence el tipo? Cámbialo, o pídele que vuelva a redactar, sin volver a transcribir.

### Tus notas, donde las uses
- Compártelas por WhatsApp, correo o la app que quieras.
- O deja que Aura las **copie sola a una carpeta**, también de Google Drive, sin que hagas nada. Desde ahí las lee tu gestor de notas o tu chatbot habitual con su conector de Drive.
- Borrar una grabación en Aura nunca borra lo que ya exportaste.

### Tus datos
- Las claves se guardan **cifradas** en el móvil y no viajan en las copias de seguridad.
- Nada de analíticas, ni informes de fallos enviados a ningún sitio, ni servicios de Google Play. Si la app falla, te enseña el error en pantalla para que lo copies tú.

## 💾 Descargar e instalar

Necesitas un móvil **Android 10 o posterior**. Entra en la página de [**descargas (Releases)**](https://github.com/abel-eiras/aura-app/releases), y en la versión más reciente descarga el archivo que termina en **`.apk`**.

> **La primera vez verás un aviso de seguridad.** Es normal: Android avisa de toda app que no viene de una tienda, y esta no está en ninguna (ni lo estará: es gratuita y sin ánimo de lucro).

1. Abre el `.apk` que has descargado.
2. Si Android te dice que no puedes instalar apps de esta fuente, pulsa **Ajustes** y activa **"Permitir de esta fuente"** para tu navegador o gestor de archivos.
3. Vuelve atrás y pulsa **Instalar**.

Cada versión trae también un archivo `.sha256` y la huella del certificado de firma en las notas, por si quieres comprobar que el APK es el original.

## 🚀 Primeros pasos

1. **Abre Aura.** La bienvenida te guía; puedes saltar cualquier paso.
2. **Elige cómo procesar tus grabaciones.** "Solo grabar" no necesita nada. Con **Gemini** necesitas una clave gratuita:
   1. Pulsa **Abrir AI Studio** e inicia sesión con tu cuenta de Google.
   2. Pulsa **Crear clave de API** y cópiala.
   3. Vuelve a Aura: te ofrecerá pegarla con un toque. Pulsa **Guardar y comprobar**.
3. **Acepta el aviso de privacidad.** Hasta que lo aceptes, no se envía nada.
4. **Elige tu idioma** (y, si quieres, un segundo idioma).
5. **Graba.** Eso es todo.

Opcional: en **Ajustes → Exportar**, elige una carpeta (por ejemplo, una de Google Drive) y Aura copiará allí cada transcripción y cada nota.

## 🔄 Actualizar a una versión nueva

**La app te avisa sola.** Al abrirla, si hay una versión nueva, verás un mensaje con las novedades: pulsa **Actualizar** y se descarga, se comprueba y se instala. Android te pedirá confirmar. Nunca se actualiza mientras estás grabando. Si prefieres no recibir avisos, desactívalos en Ajustes.

**Tus grabaciones y notas se conservan** al actualizar.

## ❓ Preguntas frecuentes

### ¿Cuánto cuesta?
Nada. Ni la app, ni la clave de Google AI Studio: su capa gratuita no pide tarjeta.

### ¿Cuántas grabaciones puedo procesar al día?
Depende de los límites que Google ponga a tu clave gratuita, que cambian con el tiempo y según el modelo. Al escribir esto, con el modelo que elige Aura por defecto (uno de la familia "Flash-Lite") la capa gratuita admitía unos cientos de peticiones al día, y cada grabación gasta dos; con otros modelos, solo unas pocas decenas. Si se agota, Aura **espera a que se renueve y lo reintenta sola**.

### ¿Salen mis grabaciones del móvil?
Solo hacia Google, para transcribirlas, y solo si has aceptado el aviso. En la capa gratuita, Google puede usar lo que envías para mejorar sus productos: te lo dice la app antes de enviar nada. Si no quieres eso, usa **Solo grabar**.

### ¿Por qué no puedo usar mi suscripción de ChatGPT, Claude o Gemini?
Esas suscripciones solo funcionan dentro de las apps de cada empresa; reutilizarlas desde otra app va contra sus condiciones. La alternativa legítima es la clave gratuita de Google.

### ¿Funciona sin internet?
Grabar, sí. La transcripción y la nota necesitan conexión; si no la hay, esperan.

### ¿Y en iPhone?
No. Solo Android: Apple no permite instalar apps fuera de su tienda con facilidad, y este proyecto no va a entrar en ninguna.

### Algo no funciona bien
En la lista de grabaciones, una grabación con error tiene un botón **Detalles** con el motivo. Si no lo resuelves, copia ese texto y [abre una incidencia](https://github.com/abel-eiras/aura-app/issues).

---

# 🛠️ Para desarrolladores

Aura se desarrolla con *Spec-Driven Development*: primero se acuerda qué hace cada parte (`specs/`), luego se construye y se prueba. Lee antes la [constitución](.specify/memory/constitution.md) y [AGENTS.md](AGENTS.md) (cómo se trabaja en este repositorio, para personas y agentes de IA).

## Cómo está hecho

- **Android nativo** (Kotlin, Jetpack Compose), sin backend, sin servicios de Google Play y sin analíticas.
- Dos módulos: `:domain` (Kotlin puro, con los clientes HTTP y casi todos los tests) y `:app` (Android).
- El procesado va por WorkManager en dos pasos persistidos por separado (transcribir, redactar), con reintentos.
- Versión y actualizaciones sin tienda: GitHub Releases, APK firmado con clave propia, comprobación de huella y de certificado.

## Comandos

```bash
./gradlew lint testDebugUnitTest :domain:test assembleDebug   # lo que corre el CI
./gradlew -Paura.domainOnly=true :domain:test                  # solo el dominio, sin SDK de Android
```

`-Paura.domainOnly=true` permite probar la lógica en entornos sin acceso al SDK de Android. La parte Android la valida el CI de GitHub (que además abre el APK en un emulador).

## Publicar una versión

Sube la versión en `gradle.properties` (`aura.versionName`), fusiona en `main` y empuja una etiqueta `vX.Y.Z` (o `vX.Y.Z-beta.N`, que sale como pre-release) que coincida con ella. El flujo [`release.yml`](.github/workflows/release.yml) firma el APK, comprueba la firma y crea la release. Detalles en [docs/releases.md](docs/releases.md).

## Documentación del proyecto

| Documento | Qué contiene |
|---|---|
| [Constitución](.specify/memory/constitution.md) | Principios no negociables |
| [Hoja de ruta](ROADMAP.md) | Hitos hasta la 1.0 |
| [Arquitectura](docs/arquitectura.md) | Plan técnico transversal |
| [Decisiones (ADR)](docs/adr/) | Por qué solo Android, sin backend, sin tiendas… |
| [Licencias de terceros](docs/licencias-de-terceros.md) | Tipografías incluidas |

### Especificaciones

| ID | Feature | Estado |
|---|---|---|
| [001](specs/001-grabacion/spec.md) | Grabación de audio | Implementada |
| [002](specs/002-configuracion-inicial/spec.md) | Configuración inicial y proveedor de IA | Implementada |
| [003](specs/003-procesado-ia/spec.md) | Procesado con IA (transcripción con hablantes y nota) | Implementada |
| [004](specs/004-notas-y-exportacion/spec.md) | Notas, hablantes y exportación | En curso |
| [005](specs/005-tipos-de-nota/spec.md) | Tipos de nota | Parcial (editor de tipos pendiente) |
| [006](specs/006-releases-y-actualizaciones/spec.md) | Releases, instalación y actualizaciones fuera de tiendas | En curso |
| [007](specs/007-informe-de-errores/spec.md) | Pantalla de error local | Implementada |
| [008](specs/008-identidad-visual/spec.md) | Identidad visual | Implementada |

## Estado y limitaciones conocidas

> **Versión 0.4.1.** Estable para el uso diario, con estas limitaciones:

- El único proveedor de IA es Google Gemini con clave propia; en su capa gratuita hay límites diarios que fija Google.
- Falta la búsqueda en las notas y reproducir el audio desde un punto de la transcripción.
- No se pueden crear tipos de nota propios todavía (hay tres incluidos).
- La calidad de la separación de hablantes y del gallego depende del modelo; con grabaciones muy largas aún no está medida.

## 📄 Licencia

[MIT](LICENSE). Las tipografías incluidas y sus licencias están en [docs/licencias-de-terceros.md](docs/licencias-de-terceros.md).

El logotipo de Formula Farma (`docs/assets/logo.png`, `app/src/main/res/drawable-nodpi/ic_aura_logo.png`, `ic_aura_mono.png` e `ic_launcher_foreground.png`) es una marca de su titular: **no** está incluido en la licencia MIT y no puede usarse para distribuir versiones modificadas.
