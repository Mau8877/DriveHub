# UT-CU02-MOB-01 — Modelos y `AuthService` (login y renovación)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-01` |
| **Nombre** | Modelos y `AuthService` (login y renovación) |
| **Caso de uso** | CU02-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La app necesita enviar las credenciales y la sesión renovable al backend y recibir la sesión como objetos tipados.

## Objetivo

La feature `auth` tiene los DTOs de la sesión y un service con `login` y `renovar`.

## Alcance

- `features/auth/models/`: `LoginRequest` (`email`, `password`, `dispositivo` fijo `MOVIL`, con `toJson`), `UsuarioSesion` (`codigo`, `nombres`, `apellidos`, `roles`, `escuelaCodigo?`, con `fromJson`) y `SesionRespuesta` (`accessToken`, `refreshToken`, `expiraEnSegundos`, `usuario`, con `fromJson`). Inmutables, escritos a mano (convención de CONSUMO_API_MOBILE.md).
- `features/auth/services/auth_service.dart`: `Future<SesionRespuesta> login(String email, String password)` → `POST /auth/login`; `Future<SesionRespuesta> renovar(String refreshToken)` → `POST /auth/refresh` con `{ dispositivo: 'MOVIL', refreshToken }`. Usa `ApiClient.instance` (inyectable por constructor, como `RegistroService`) y relanza el `ApiException` con el código de negocio.
- Tests con el adaptador de Dio falso de `registro_service_test.dart`: cuerpo exacto de login (con `dispositivo: 'MOVIL'`) y de renovación; parseo de `SesionRespuesta`; un `401 ERR_IDE_04` y un `423 ERR_IDE_05` llegan como `ApiException` con su código.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/models/` (3 archivos)
- `projects/mobile/lib/features/auth/services/auth_service.dart`
- `projects/mobile/test/features/auth/auth_service_test.dart`

## Fuera de alcance (no tocar)

- Guardar la sesión (UT 02) y el estado (UT 03).
- El interceptor Bearer (UT 04).
- Cerrar sesión (CU03).

## Resultado esperado

La app puede iniciar y renovar sesión por HTTP con objetos tipados.

## Validación

1. `flutter analyze` y `flutter test` en `projects/mobile` pasan.
