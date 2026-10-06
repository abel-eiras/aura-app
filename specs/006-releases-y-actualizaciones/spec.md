# Especificación: Releases, instalación y actualizaciones fuera de tiendas

**ID:** 006 · **Estado:** Aprobada · **Creada:** 2026-10-06
**Depende de:** —

## Contexto

La app es software libre, gratuito, y no se publica en ninguna tienda
(Constitución II y III). Se distribuye como APK firmado en GitHub Releases y
se actualiza desde dentro de la propia app. Esto tiene que ser tan sencillo
para un amigo no técnico como instalar desde una tienda: una página con un
botón de descarga, un permiso que se concede una vez, y a partir de ahí
actualizaciones con un toque.

Para el autor, publicar una versión tiene que ser crear una etiqueta en git:
todo lo demás (compilar, firmar, publicar) lo hace GitHub Actions gratis.

## Historias de usuario

### HU-006-1 — Instalar la app por primera vez (P1)

Como amigo no técnico al que le han pasado un enlace, quiero instalar la app
siguiendo pasos claros, para no tener miedo a "instalar algo raro".

**Prueba independiente:** una persona no técnica instala la app desde el
enlace en menos de 3 minutos sin ayuda.

**Escenarios de aceptación:**

1. **Dado** el enlace de instalación (página del proyecto), **cuando** lo abro
   en el móvil, **entonces** veo un botón "Descargar Aura para Android" que
   descarga el APK de la última versión estable, y debajo 3 pasos con
   capturas para permitir la instalación desde el navegador y abrir el
   fichero.
2. **Dado** que Android muestra avisos de seguridad (Play Protect, "origen
   desconocido"), **cuando** aparecen, **entonces** la página explica cada
   uno, por qué aparece y qué pulsar, sin restarle importancia.
3. **Dado** que uso Obtainium, **cuando** añado la URL del repositorio,
   **entonces** detecta las versiones y el APK sin configuración adicional.

### HU-006-2 — Enterarme de que hay una versión nueva (P1)

Como usuario, quiero que la app me avise cuando haya una versión nueva y me
cuente qué cambia, para estar al día sin buscar nada.

**Escenarios de aceptación:**

1. **Dado** que hay una versión estable más nueva que la instalada,
   **cuando** abro la app (como mucho una comprobación cada 24 h, o al pulsar
   "Buscar actualizaciones"), **entonces** veo un aviso discreto "Versión
   1.2.0 disponible" con un enlace a las novedades.
2. **Dado** que no hay conexión o GitHub no responde, **cuando** se comprueba,
   **entonces** no se muestra ningún error al usuario (solo en Ajustes →
   Acerca de: "No se pudo comprobar").
3. **Dado** que he desactivado "Buscar actualizaciones automáticamente",
   **cuando** abro la app, **entonces** no se hace ninguna petición a GitHub.

### HU-006-3 — Actualizar con un toque (P1)

Como usuario, quiero actualizar desde la propia app, para no tener que volver
a la web a descargar.

**Escenarios de aceptación:**

1. **Dado** el aviso de nueva versión, **cuando** pulso "Actualizar",
   **entonces** la app descarga el APK mostrando el progreso, comprueba su
   integridad y su firma, y lanza la instalación.
2. **Dado** que es la primera actualización desde la app, **cuando** Android
   pide permitir que Aura instale apps, **entonces** la app explica antes, en
   una frase, por qué se necesita y lleva al ajuste.
3. **Dado** que la descarga está corrupta o la firma no coincide con la de la
   app instalada, **cuando** se comprueba, **entonces** se descarta el fichero,
   no se instala nada y se muestra "La descarga no es válida. Inténtalo más
   tarde".
4. **Dado** que hay una grabación o un procesado en curso, **cuando** pulso
   "Actualizar", **entonces** la app pide esperar a que termine (la
   instalación cierra la app).
5. **Dado** que la actualización termina, **cuando** vuelvo a abrir la app,
   **entonces** conservo todas mis grabaciones, notas, ajustes y credenciales.

### HU-006-4 — Publicar una versión (autor) (P1)

Como autor, quiero publicar una versión creando una etiqueta, para no
compilar ni firmar a mano.

**Escenarios de aceptación:**

1. **Dado** un commit en `main` con CI en verde, **cuando** creo y subo la
   etiqueta `v1.2.0`, **entonces** GitHub Actions compila el APK de release,
   lo firma con la clave guardada en los secretos del repositorio, y publica
   una Release `v1.2.0` con `aura-1.2.0.apk`, `aura-1.2.0.apk.sha256` y las
   notas de versión.
2. **Dado** una etiqueta con sufijo (`v1.3.0-beta.1`), **cuando** se publica,
   **entonces** la Release se marca como pre-release y la app no la ofrece
   salvo que el usuario active "Recibir versiones de prueba".
3. **Dado** que la versión de la etiqueta no coincide con la del código, o
   los tests fallan, **cuando** corre el workflow, **entonces** falla sin
   publicar nada.

## Casos límite

- Límite de peticiones sin autenticar de la API de GitHub: la comprobación
  diaria debe estar muy por debajo; si se recibe un 403/429, se espera al
  siguiente ciclo sin avisar al usuario.
- El usuario instaló una versión firmada con otra clave (p. ej. una
  compilación propia): la app lo detecta y explica que debe desinstalar para
  pasar a la versión oficial (y que eso borra sus datos, salvo que exporte
  antes).
- Bajada de versión (la última Release es menor que la instalada, p. ej. tras
  una pre-release): no se ofrece.
- Espacio insuficiente para descargar: se avisa antes de empezar.
- Datos móviles: la descarga solo empieza al pulsar "Actualizar"; nunca
  descarga APKs en segundo plano por datos móviles.

## Requisitos funcionales

- **FR-006-01**: Cada versión DEBE publicarse como GitHub Release del
  repositorio `abel-eiras/aura-app` con un APK firmado nombrado
  `aura-<versión>.apk` y su suma SHA-256 en `aura-<versión>.apk.sha256`.
- **FR-006-02**: La publicación DEBE ser automática al subir una etiqueta
  `v<semver>`; el workflow DEBE ejecutar tests y lint, verificar que la
  etiqueta coincide con `versionName`, firmar con la clave de los secretos y
  publicar. Las etiquetas con sufijo de pre-release DEBEN publicarse como
  pre-release.
- **FR-006-03**: `versionCode` DEBE derivarse de la versión
  (`MAJOR*10000 + MINOR*100 + PATCH`) para que siempre crezca.
- **FR-006-04**: La app DEBE comprobar la última versión publicada como
  máximo una vez cada 24 h al abrirse, y bajo demanda desde Ajustes → Acerca
  de; DEBE poder desactivarse. Es la única petición de red que la app hace
  por iniciativa propia aparte del proveedor de IA. (Constitución IV)
- **FR-006-05**: La app DEBE mostrar versión y notas de la Release antes de
  actualizar, y ofrecer las pre-releases solo si el usuario lo activa.
- **FR-006-06**: La app DEBE descargar el APK, verificar su SHA-256 contra el
  fichero publicado y verificar que el certificado de firma del APK coincide
  con el de la app instalada antes de instalar.
- **FR-006-07**: La instalación DEBE usar el instalador de paquetes del
  sistema, pidiendo el permiso de instalar apps desconocidas solo cuando el
  usuario pulse "Actualizar" por primera vez, con explicación previa. En las
  versiones de Android que lo permitan, las actualizaciones siguientes
  DEBERÍAN poder instalarse sin confirmación adicional.
- **FR-006-08**: La app NO DEBE iniciar una actualización con una grabación
  o un procesado en curso.
- **FR-006-09**: El repositorio DEBE tener una página de instalación
  (README y GitHub Pages) con enlace directo a la última versión, código QR,
  pasos con capturas en es/gl/en, explicación de los avisos de seguridad y
  la huella SHA-256 del certificado de firma para quien quiera verificarla.
- **FR-006-10**: Las Releases DEBEN ser compatibles con Obtainium sin
  configuración (un único APK universal por Release).
- **FR-006-11**: La clave de firma de release DEBE generarse una sola vez,
  guardarse con copia de seguridad fuera de GitHub, y usarse en CI solo a
  través de secretos cifrados; el procedimiento DEBE estar documentado en
  `docs/releases.md`.

## Entidades clave

- **Release**: versión semver, canal (estable / pre-release), notas, APK,
  suma SHA-256, fecha.
- **Estado de actualización**: última comprobación, resultado, versión
  disponible, progreso de descarga.

## Criterios de éxito

- **CE-006-1**: Publicar una versión requiere solo `git tag vX.Y.Z && git push
  --tags`; la Release aparece en menos de 15 minutos.
- **CE-006-2**: Actualizar desde la app lleva como máximo 3 toques.
- **CE-006-3**: Coste para el autor: 0 € (GitHub Actions y Releases en
  repositorio público).

## Fuera de alcance

- Google Play, Galaxy Store, Amazon u otras tiendas comerciales.
- Actualizaciones delta o APKs por arquitectura.
- Servidor de actualizaciones propio.
- F-Droid: no se descarta (la app no usa dependencias propietarias) pero no
  es objetivo de v1; exigiría builds reproducibles.

## Riesgos

- **Verificación de desarrolladores de Google.** Google anunció que, por
  fases a partir de 2026, los dispositivos Android certificados solo
  instalarán apps de desarrolladores registrados, también fuera de Google
  Play, con una vía específica para estudiantes y aficionados. Antes de la
  v1.0 hay que comprobar el estado real y, si aplica, registrar la clave de
  firma por la vía gratuita. Documentar la conclusión en un ADR.
- **Pérdida de la clave de firma.** Si se pierde, los usuarios no pueden
  actualizar sin desinstalar. Mitigación: FR-006-11.
