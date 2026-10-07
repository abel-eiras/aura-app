# Publicar versiones y custodiar la clave de firma

Procedimiento de la [spec 006](../specs/006-releases-y-actualizaciones/spec.md)
(FR-006-01…03, FR-006-11). Todo cuesta 0 €: GitHub Actions y Releases en un
repositorio público.

## Publicar una versión

1. Sube `aura.versionName` en `gradle.properties` (semver: `1.2.0`, o
   `1.3.0-beta.1` para una pre-release). `versionCode` se deriva solo:
   `MAJOR*10000 + MINOR*100 + PATCH`, así que `minor` y `patch` van de 0 a 99.
2. Commit y PR a `main` con CI en verde.
3. Desde `main`:
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```
4. El workflow `release.yml` comprueba que la etiqueta coincide con
   `aura.versionName`, pasa lint y tests, compila `assembleRelease`, lo firma,
   verifica la firma con `apksigner` y publica la Release con
   `aura-1.2.0.apk`, `aura-1.2.0.apk.sha256`, las notas generadas y la huella
   del certificado. Las etiquetas con sufijo (`-beta.1`) salen como
   pre-release y la app solo las ofrece a quien activa "Recibir versiones de
   prueba".

Si el workflow falla, no se publica nada. Se arregla en `main`, se borra la
etiqueta (`git push --delete origin v1.2.0 && git tag -d v1.2.0`) y se vuelve a
crear.

## La clave de firma (una sola vez)

La clave identifica a la app: si cambia o se pierde, **nadie puede actualizar
sin desinstalar** (y desinstalar borra sus datos). Se genera una vez, fuera del
repositorio:

```bash
keytool -genkeypair -v -keystore aura-release.keystore -alias aura \
  -keyalg RSA -keysize 4096 -validity 10000
```

- **Copia de seguridad** del `.keystore` y de sus contraseñas en al menos dos
  sitios fuera de GitHub (gestor de contraseñas + otro soporte). Sin copia, la
  clave es un punto único de fallo.
- Nunca se sube al repositorio (`*.keystore` y `*.jks` están en `.gitignore`).

### Secretos del repositorio

En *Settings → Secrets and variables → Actions*:

| Secreto | Contenido |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 aura-release.keystore` |
| `RELEASE_STORE_PASSWORD` | Contraseña del almacén |
| `RELEASE_KEY_ALIAS` | `aura` |
| `RELEASE_KEY_PASSWORD` | Contraseña de la clave |

Para compilar una release en local, pon las mismas cuatro variables
(`RELEASE_STORE_FILE` con la ruta absoluta) en el entorno o en
`local.properties` (ignorado por git).

## Verificar una descarga

Cada Release lleva la huella SHA-256 del certificado de firma en sus notas. Quien
quiera comprobarla:

```bash
apksigner verify --print-certs aura-1.2.0.apk
sha256sum -c aura-1.2.0.apk.sha256
```

La huella debe ser siempre la misma entre versiones. Se copia también en la
página de instalación (FR-006-09) cuando exista la primera Release.

## Pendientes conocidos

- **Fijar las acciones de GitHub por SHA** en `release.yml` (hoy van por
  etiqueta mayor) antes de la 1.0: ese workflow tiene acceso a la clave.
- **Verificación de desarrolladores de Android**: comprobar el estado real
  antes de la 1.0 y registrar la clave por la vía gratuita si aplica; dejar la
  conclusión en un ADR (spec 006, Riesgos).
- La primera Release (`v0.1.0`) exige haber creado la clave y los secretos.
