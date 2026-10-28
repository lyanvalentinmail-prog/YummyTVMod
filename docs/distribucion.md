# Distribución: releases y APK por workflow

Este fork publica sus propios APK en **GitHub Releases**. El proceso está automatizado en
`.github/workflows/release-apk.yml`.

## Dónde descargar la app

1. **Releases (recomendado):** https://github.com/lyanvalentinmail-prog/YummyTVMod/releases/latest
   → descarga el asset que termina en `.apk`.
2. **Artefacto de workflow:** pestaña **Actions** → workflow **Release APK** → abre la última
   ejecución → sección **Artifacts** → `yummytv-apk-<versión>`. Se conserva 90 días y contiene los
   mismos APK que el release.

Para instalarlo, pasa el APK al dispositivo y ábrelo; en Android TV puedes usar
[LocalSend](https://localsend.org/). Activa «Instalar apps desconocidas» si Android lo pide.

## Cómo se publica un release

El workflow hace tres cosas: compila, sube el APK como artefacto y lo publica en Releases.

| Paso | Resultado |
|---|---|
| `assembleDebug` | APK firmado con la clave debug de CI (instalable) |
| `assembleRelease` | APK de release sin firmar, usado como base para la firma opcional |
| `actions/upload-artifact@v4` | `yummytv-apk-<versión>`, descargable desde la ejecución |
| `gh release create` / `upload` | Release con el APK y `SHA256SUMS.txt` |

Disparadores:

- `push` a `main`, `master`, `release/**`, `arena/**`.
- `push` de etiquetas `v*` y `b*`.
- `workflow_dispatch`: **Actions → Release APK → Run workflow** (permite indicar otra etiqueta y
  marcar el release como pre-release).
- `repository_dispatch` con `event_type=build-release`, útil desde la API cuando el token no tiene
  permiso `actions: write`:

  ```bash
  gh api -X POST repos/lyanvalentinmail-prog/YummyTVMod/dispatches \
    -f event_type=build-release
  ```

  Los eventos `repository_dispatch` solo ejecutan workflows que estén en la rama por defecto, así
  que el workflow debe estar fusionado en `main`.

La etiqueta se calcula desde `yummytv.versionName` en `gradle.properties`: `1.26.7` → `v1.26.7`, y
`1.26.8.1-beta` → `b1.26.8.1` (pre-release). Para sacar una versión nueva, sube `versionName` y
`versionCode` en ese fichero. Las reglas completas están en [beta-updates.md](beta-updates.md).

El workflow es **idempotente**: si la etiqueta ya existe, borra los assets `.apk` anteriores (el
actualizador de la app toma el primero) y vuelve a subir el APK con `--clobber`.

## Firma del APK

El repositorio no incluye keystore de release. Por defecto se publica el **APK debug**, firmado con
la clave debug que genera el runner:

- Es instalable, pero usa el `applicationId` `su.afk.yummy.tv.debug`.
- La clave cambia en cada ejecución, así que **no sirve para actualizar** una instalación previa.

Para publicar un APK de release firmado (mismo `applicationId`, actualizable), añade estos secretos
del repositorio (**Settings → Secrets and variables → Actions**):

| Secreto | Valor |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Keystore PKCS12 codificado en base64 |
| `RELEASE_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `RELEASE_KEY_ALIAS` | Alias de la clave (por defecto `yummytv`) |
| `RELEASE_KEY_PASSWORD` | Contraseña de la clave |

Cómo generarlos en local:

```bash
keytool -genkeypair -v -keystore release.p12 -storetype PKCS12 \
  -alias yummytv -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.p12 > release.b64   # en macOS: base64 -i release.p12 -o release.b64

gh secret set RELEASE_KEYSTORE_BASE64      < release.b64
gh secret set RELEASE_KEYSTORE_PASSWORD   # escribe la contraseña
gh secret set RELEASE_KEY_ALIAS           # yummytv
gh secret set RELEASE_KEY_PASSWORD        # escribe la contraseña
```

El workflow usa `zipalign` + `apksigner` del SDK, verifica la firma y publica
`YummyTV-<versión>.apk`. **Guarda el keystore fuera del repositorio**: si se pierde o cambia,
Android no permitirá instalar una actualización sobre la versión anterior.

## Actualizaciones desde la app

`UpdateConfig` apunta a los releases de este fork. La app consulta
`/repos/lyanvalentinmail-prog/YummyTVMod/releases`, descarta borradores y releases sin APK, y elige
la versión más alta según [beta-updates.md](beta-updates.md). Por eso el release debe tener exactamente
un asset `.apk` y una etiqueta `v…`/`b…`.

## Si Actions no arranca

En un fork, GitHub puede dejar Actions desactivado. Actívalo en
**Settings → Actions → General**:

1. *Allow all actions and reusable workflows*.
2. *Workflow permissions* → **Read and write permissions** (necesario para crear releases).

Después lanza el workflow a mano desde la pestaña Actions.
