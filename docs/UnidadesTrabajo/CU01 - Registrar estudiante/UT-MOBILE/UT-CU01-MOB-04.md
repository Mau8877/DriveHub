# UT-CU01-MOB-04 — Modelo y `RegistroService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-04` |
| **Nombre** | Modelo y `RegistroService` |
| **Caso de uso** | CU01-MOB |
| **Aplicación** | `mobile` |
| **Depende de** | UT-CU01-MOB-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (el `service` recibe el `ApiClient`, no crea su propio `Dio`; convención de DTOs de UT 01) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) (se prueba el parseo de respuestas).

## Solicitud de negocio

La app debe enviar el registro al backend y recibir la cuenta creada o un error de negocio con su código.

## Objetivo

La feature `auth` tiene el DTO de registro y un service que llama a `POST /api/v1/usuarios`.

## Alcance

- `lib/features/auth/models/registro_estudiante_request.dart`: clase inmutable con `nombres`, `apellidos`, `fechaNacimiento` (`DateTime`), `email`, `password`, `telefono?`, `escuelaSlug`; `toJson()` serializa la fecha como `yyyy-MM-dd`. **No tiene campo de rol.**
- `lib/features/auth/models/usuario_registrado.dart`: `codigo`, `email`, `nombres`, `apellidos`, con `factory fromJson`.
- `lib/features/auth/services/registro_service.dart`: `Future<UsuarioRegistrado> registrar(RegistroEstudianteRequest)` con `POST /usuarios` usando `ApiClient.instance` (inyectable por constructor como `Dio` para poder probarlo). Un `DioException` cuyo `error` es `ApiException` se relanza como esa `ApiException` (con el código de negocio); otros errores de red se relanzan tal cual.
- Tests (`test/features/auth/registro_service_test.dart`) con un adaptador/`Dio` falso: el cuerpo enviado tiene exactamente los campos esperados y la fecha como `yyyy-MM-dd`, sin `rol`; el parseo de `UsuarioRegistrado`; un `409` con `ERR_IDE_01` llega como `ApiException(code: 'ERR_IDE_01')`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/models/registro_estudiante_request.dart`, `usuario_registrado.dart`
- `projects/mobile/lib/features/auth/services/registro_service.dart`
- `projects/mobile/test/features/auth/registro_service_test.dart`

## Fuera de alcance (no tocar)

- `core/network/api_client.dart` (ya desempaqueta el sobre y lanza `ApiException`).
- Providers y pantallas.
- Login, refresh o cualquier otro endpoint; consulta del nombre de la escuela.

## Resultado esperado

`RegistroService.registrar` envía la petición correcta y expone los errores de negocio con su código.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
