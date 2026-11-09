# UT-CU36-MOB-01 — Firebase y `applicationId` en Android

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-MOB-01` |
| **Nombre** | Firebase y `applicationId` en Android |
| **Caso de uso** | CU36-MOB |
| **Microservicio** | app móvil |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md). Antes de empezar hay que registrar la app Android en la consola de Firebase (proyecto de DriveHub, el mismo del panel web) con el nombre de paquete definitivo y bajar `google-services.json`.

## Solicitud de negocio

La app tiene que poder recibir avisos push de Firebase y tener una identidad propia, no la de ejemplo.

## Objetivo

La app se compila con Firebase configurado, con el identificador `com.drivehub.app` y el nombre «DriveHub».

## Alcance

- `android/app/google-services.json` (de la consola de Firebase; `package_name` = `com.drivehub.app`).
- `android/settings.gradle.kts` y `android/app/build.gradle.kts`: el plugin `com.google.gms.google-services`; `applicationId = "com.drivehub.app"` (el `namespace` no cambia).
- `AndroidManifest.xml`: el permiso `POST_NOTIFICATIONS` (Android 13 o más) y la etiqueta `DriveHub`.
- `pubspec.yaml`: `firebase_core` y `firebase_messaging`.

**Archivos esperados (crear/tocar):** los de arriba.

## Fuera de alcance (no tocar)

- El código Dart del registro (UT 02 y 03).
- La firma de release y la publicación en Google Play.
- Firebase para iOS.

## Resultado esperado

`flutter build apk --debug` compila; la app se instala como «DriveHub» con el identificador nuevo.

## Validación

1. `flutter build apk --debug` termina sin errores.
2. `adb shell pm list packages | grep com.drivehub.app` muestra la app instalada.
3. Si `applicationId` y `package_name` no coinciden, el plugin de Google falla al compilar: así se detecta.
