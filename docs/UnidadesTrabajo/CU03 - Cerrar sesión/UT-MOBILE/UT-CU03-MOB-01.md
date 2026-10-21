# UT-CU03-MOB-01 — `AuthService.cerrar` y `SesionNotifier.cerrarSesion`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-MOB-01` |
| **Nombre** | `AuthService.cerrar` y `SesionNotifier.cerrarSesion` |
| **Caso de uso** | CU03-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Al cerrar sesión, la app debe invalidar la sesión en el servidor y olvidar todo lo guardado en el teléfono, pase lo que pase con la red.

## Objetivo

`sesionProvider.notifier.cerrarSesion()` cierra la sesión y deja la app sin sesión.

## Alcance

- `features/auth/services/auth_service.dart`: `Future<void> cerrar(String refreshToken)` → `POST /auth/logout` con `{ 'dispositivo': 'MOVIL', 'refreshToken': refreshToken }`, mismo manejo de `ApiException` que `_post`.
- `features/auth/providers/sesion_provider.dart`: `Future<void> cerrarSesion()`: si hay `_refreshToken`, intenta `authService.cerrar(...)` dentro de `try/catch` que ignora todo error; después `await _perderSesion(null)` (cancela el temporizador, borra token en memoria y almacenamiento seguro, estado `sinSesion` sin `motivo`). Si no hay refresh token, solo la parte local.
- Tests: `cerrar` envía el cuerpo correcto; `cerrarSesion` llama a `cerrar` con el token vigente, borra el almacenamiento y deja `FaseSesion.sinSesion` sin `motivo`; si `cerrar` lanza (`ApiException` y error de red) igual queda sin sesión y con el almacenamiento borrado.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/services/auth_service.dart`
- `projects/mobile/lib/features/auth/providers/sesion_provider.dart`
- `projects/mobile/test/features/auth/services/auth_service_test.dart`, `.../providers/sesion_provider_test.dart` (los existentes)

## Fuera de alcance (no tocar)

- El botón (UT 02) y el router.
- Confirmación previa.
- `ApiClient` y su interceptor.

## Resultado esperado

Llamar a `cerrarSesion()` deja la app sin sesión, con el almacenamiento seguro vacío.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
