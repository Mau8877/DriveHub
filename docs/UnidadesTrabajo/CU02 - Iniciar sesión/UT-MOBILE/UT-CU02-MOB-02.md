# UT-CU02-MOB-02 — Almacenamiento seguro de la sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-02` |
| **Nombre** | Almacenamiento seguro de la sesión |
| **Caso de uso** | CU02-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU02-MOB-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La sesión móvil debe sobrevivir al cierre de la app sin quedar en texto plano en el teléfono.

## Objetivo

Existe `SesionStorage`, que guarda y lee la sesión en `flutter_secure_storage`.

## Alcance

- `features/auth/services/sesion_storage.dart`: clase abstracta con `guardar(SesionGuardada)`, `leer()` y `borrar()`, e implementación `SesionStorageSegura` sobre `FlutterSecureStorage` (ya está en `pubspec.yaml`). `SesionGuardada` (`refreshToken`, `usuario`) se guarda como un JSON bajo una clave propia del proyecto. El access token no se guarda: dura 15 minutos y se obtiene renovando al abrir la app.
- `sesionStorageProvider` (`Provider<SesionStorage>`) para poder sustituirlo en los tests.
- Si lo guardado está corrupto, `leer()` devuelve `null` y borra la clave (no rompe el arranque).
- Tests con un `SesionStorage` en memoria y, para la implementación segura, `FlutterSecureStorage.setMockInitialValues`: guarda y lee lo mismo; `borrar` deja `null`; un JSON corrupto devuelve `null`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/services/sesion_storage.dart`
- `projects/mobile/test/features/auth/sesion_storage_test.dart`

## Fuera de alcance (no tocar)

- El estado de la sesión y los temporizadores (UT 03).
- Guardar tokens en `SharedPreferences` o en archivos.
- Borrar la sesión al cerrarla (CU03 la usa, pero `borrar` ya existe).

## Resultado esperado

La sesión se guarda y se recupera de forma segura, y un valor corrupto no rompe nada.

## Validación

1. `flutter analyze` y `flutter test` en `projects/mobile` pasan.
