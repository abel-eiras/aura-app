# Arquitectura (plan técnico transversal)

> Marco común que los `plan.md` de cada feature concretan. Si un plan
> necesita contradecir algo de aquí, actualiza este documento (y, si es una
> decisión relevante, añade un ADR) en el mismo PR.

## Visión general

```
┌──────────────────────────── Teléfono (Android 10+) ────────────────────────────┐
│                                                                                 │
│  Mosaico / Widget / UI ──► RecordingService ──► recordings/aura_….ogg  (001)   │
│                                   │                                             │
│                                   ▼                                             │
│                              Room: Recording ──► ProcessingWorker (WorkManager) │
│                                                     │  paso 1: transcribir      │
│                                                     │  paso 2: clasificar+redactar
│                                                     ▼                     (003) │
│                                   Room: Transcript, Note ──► UI de notas (004)  │
│                                                     │                           │
│                                                     ├─► Compartir (sistema)     │
│                                                     └─► ExportWorker → carpeta  │
│                                                         SAF (Syncthing, etc.)   │
└───────────────────────────────┬─────────────────────────────┬───────────────────┘
                                │ HTTPS, cuenta del usuario   │ HTTPS, 1×/día
                                ▼                             ▼
                    Gemini API  /  OpenRouter            GitHub Releases API (006)
```

Únicas conexiones salientes: el proveedor de IA elegido (solo al procesar y
validar la credencial) y la API de GitHub Releases (comprobación de
actualizaciones, desactivable). Ninguna otra.

## Módulos y capas

Dos módulos Gradle (ADR-0007): **`:domain`** (Kotlin/JVM puro: modelos,
reglas, contratos de proveedor; sin `android.*`) y **`:app`** (Android).
Paquetes de `:app`:

```
io.github.abeleiras.aura
├── ui/            Compose: onboarding, record, recordings, note, settings, update
├── data/
│   ├── db/        Room: Recording, ProcessingJob, Transcript, Note, NoteType
│   ├── provider/  GeminiProvider + OpenRouterProvider (implementan AiProvider)
│   ├── secrets/   CredentialStore (AES-GCM con clave del Android Keystore)
│   ├── export/    ExportRepository (SAF)
│   └── update/    UpdateRepository (descarga, verificación, PackageInstaller)
├── recording/     RecordingService, AudioRecorderEngine, CallStateMonitor  (portado de Aura)
├── work/          ProcessingWorker, ExportWorker
├── tile/ widget/  (portados de Aura)
└── di/            Hilt
```

Paquetes de `:domain` (`io.github.abeleiras.aura.domain`): `notes`
(tipos, clasificador, render y rutas de exportación), `transcript`
(modelo y hablantes), `processing` (política de reintentos, `AiProvider`,
`NoteDrafter`), `update` (semver, selección de actualización, SHA-256).

## Stack

| Área | Elección | Motivo |
|---|---|---|
| Lenguaje / UI | Kotlin, Jetpack Compose, Material 3 | Igual que Aura |
| DI | Hilt | Igual que Aura |
| Persistencia | Room (+ DataStore para preferencias) | Notas buscables, estados de trabajos |
| Segundo plano | Servicio en primer plano (grabar), WorkManager (procesar, exportar) | Sobrevive a cierres y espera red |
| Red | OkHttp + kotlinx.serialization | Sin Retrofit: pocas llamadas, menos dependencias |
| Reproducción | Media3 ExoPlayer | Seek preciso en OGG/Opus para "tocar segmento" |
| Markdown | Una librería de render Markdown para Compose (a elegir en el plan de 004) | |
| Secretos | Android Keystore + AES-GCM propio | `security-crypto` deprecada (ADR-0005) |
| Tests | JUnit, Turbine, MockWebServer, Robolectric donde haga falta | Respuestas de proveedores grabadas |

Prohibido: Google Play Services, Firebase, cualquier SDK de analítica o
crash reporting, SDKs propietarios (Constitución III y IV).

## Interfaz de proveedor (003)

```kotlin
interface AiProvider {
    val id: ProviderId                       // GEMINI, OPENROUTER
    suspend fun validateCredential(): CredentialCheck
    suspend fun transcribe(audio: File, hints: LanguageHints): Transcript   // paso 1
    suspend fun complete(system: String, user: String, model: String?): String // paso 2
}
```

- La clasificación y la redacción (paso 2) son lógica de dominio que usa
  `complete()`: el mismo código para los dos proveedores, igual que
  `clasificar.py` y `notas.py` de aura-transcribe.
- Errores tipados: `Transient` (red, 429, 5xx) / `Permanent(reason)`
  (credencial inválida, sin saldo, cuota, audio demasiado largo, formato no
  válido tras reintento). El worker decide reintentar solo con `Transient`.
- Añadir un proveedor futuro (en dispositivo, AssemblyAI…) = una
  implementación nueva de `AiProvider`.

### Gemini (a verificar en el plan de 003)

- Subida del audio con la Files API (subida reanudable), referencia al
  fichero en `generateContent`, y borrado del fichero remoto al terminar
  (expiran solos a las 48 h).
- Salida estructurada (`responseMimeType: application/json` + esquema) para
  obtener los segmentos directamente; el prompt fija las etiquetas
  `SPEAKER_NN` por orden de aparición y pide marcas de tiempo aproximadas.
- Modelos por defecto: el Gemini "Flash" estable vigente para ambos pasos.
  Constante única en `data/provider/Defaults.kt`.

### OpenRouter (a verificar en el plan de 002/003)

- OAuth PKCE: `https://openrouter.ai/auth?callback_url=…&code_challenge=…&code_challenge_method=S256`,
  intercambio del `code` por una clave en `POST /api/v1/auth/keys`.
- Paso 1 con un modelo que acepte audio como entrada; paso 2 con cualquiera.
- Spike pendiente: esquema de `callback_url` admitido y tamaño máximo de
  audio por petición (ver preguntas abiertas de 002 y 003).

## Máquina de estados del procesado (003)

```
QUEUED ─► WAITING_NETWORK ─► TRANSCRIBING ─► DRAFTING ─► READY
   │                             │   │           │
   │                             │   └─► NO_CONTENT (texto vacío, sin paso 2)
   └────────── cualquier paso ───┴──────────────►┴─► ERROR(reason)  ── reintentar ─► QUEUED
```

- El resultado de cada paso se persiste antes de pasar al siguiente
  (FR-003-01): reanudar tras un cierre empieza en el primer paso sin
  resultado.
- Un único worker encadenado (`enqueueUniqueWork`, política APPEND) procesa
  una grabación cada vez.

## Almacenamiento

```
filesDir/
  recordings/aura_AAAAMMDD_HHMMSS.ogg      audio (nunca se toca tras cerrarlo)
  updates/aura-X.Y.Z.apk                   temporal del actualizador
databases/aura.db                          Room: metadatos, transcripciones, notas, tipos
```

- `allowBackup` limitado por `data_extraction_rules.xml`: se respaldan
  audios, base de datos y preferencias; **nunca** el almacén de credenciales.

## Seguridad

- Credenciales: AES-256-GCM con clave no exportable del Android Keystore;
  nunca en logs (un test recorre los logs de los tests de integración).
- HTTPS exclusivamente; sin `cleartextTraffic`.
- APK de actualización: SHA-256 + coincidencia de certificado de firma
  antes de entregarlo a `PackageInstaller`.
- Permisos: `RECORD_AUDIO`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_DATA_SYNC` (si el
  plan de 003 lo necesita), `INTERNET`, `ACCESS_NETWORK_STATE`,
  `READ_PHONE_STATE` (opcional), `REQUEST_INSTALL_PACKAGES`.

## Qué se porta de Aura

| De Aura | A aura-app | Cambios |
|---|---|---|
| `recording/*` | `recording/*` | Quitar rama AAC (minSdk 29); tras parar, encolar procesado/exportación en vez de subida |
| `qstile/`, `widget/` | `tile/`, `widget/` | Sin cambios funcionales |
| `ui/theme`, `AuraOrb`, diálogos de permisos y batería | `ui/` | Sin cambios funcionales |
| `values-es`, `values-gl`, `values` | igual | Añadir cadenas nuevas |
| `data/update/*` | `data/update/*` | De "abrir la página" a descarga + verificación + instalación |
| `data/drive`, `data/auth`, `data/upload` | — | Se eliminan (ADR-0004) |

## CI/CD

- `ci.yml` (cada PR y push a `main`): `./gradlew lint testDebugUnitTest assembleDebug`.
- `release.yml` (etiqueta `v*`): tests → comprobar etiqueta = `versionName`
  → `assembleRelease` firmado con secretos (`RELEASE_KEYSTORE_BASE64`,
  `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) →
  `sha256sum` → GitHub Release (pre-release si la etiqueta tiene sufijo).
- `gradle-wrapper.jar` se versiona (a diferencia de Aura) para que CI y
  quien clone el repo puedan compilar sin pasos manuales.
