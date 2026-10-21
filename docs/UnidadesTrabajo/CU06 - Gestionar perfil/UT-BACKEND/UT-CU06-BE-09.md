# UT-CU06-BE-09 — Pedir el cambio de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-09` |
| **Nombre** | Pedir el cambio de correo |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-01, UT-CU06-BE-05, UT-CU06-BE-08 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La persona pide cambiar su correo y recibe un enlace en la dirección nueva; mientras no lo abra, su correo no cambia.

## Objetivo

`SolicitarCambioCorreoUseCase` y su servicio, con el código `ERR_IDE_18`.

## Alcance

- `IdentidadErrorCodes.CORREO_IGUAL_AL_ACTUAL = ERR_IDE_18` (409, `ConflictException`). El correo en uso reutiliza `ERR_IDE_01`.
- `domain/port/in/SolicitarCambioCorreoComando` (`record`: `usuarioCodigo`, `passwordActual`, `correoNuevo`) y `SolicitarCambioCorreoUseCase`: `void solicitar(SolicitarCambioCorreoComando)`.
- `application/service/SolicitarCambioCorreoService`, `@Transactional` (si el envío falla, la excepción se propaga y el enlace no queda guardado, igual que CU04b):
  1. cuenta inexistente → `UnauthorizedException` `ERR_IDE_09`;
  2. contraseña actual incorrecta → `ERR_IDE_14` (400, sin sumar intentos);
  3. el correo nuevo se normaliza (sin espacios, minúsculas); igual al actual → `ERR_IDE_18`; ya usado por otra cuenta (`existePorEmail`) → `ERR_IDE_01`;
  4. `invalidarPendientes(usuario, CAMBIO_CORREO)`; `GeneradorTokenOpacoPort.generar()` y `hashear()`; guarda el token (`tipo = CAMBIO_CORREO`, `correoNuevo`, vigencia `app.auth.cambio-correo-ttl-horas` = 24);
  5. arma `${app.web.url}/account/change-email?token=<token en claro>` y llama a `EnviadorCorreoPort.enviarCambioCorreo(correoNuevo, nombres, enlace)` — **al correo nuevo**. El correo de la cuenta no cambia.
- `application.properties`: `app.auth.cambio-correo-ttl-horas=24`.
- Logs: `INFO` con `module="auth"` y `usuarioCodigo`; nunca el token, el enlace ni la contraseña. El correo nuevo no se loguea completo.
- Documentar `ERR_IDE_18` en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y su índice en `docs/errors/README.md`.
- Tests unitarios: envía el enlace correcto al correo nuevo y no modifica la cuenta; invalida los pendientes antes de guardar; guarda solo el hash, con tipo y `correoNuevo`; actual incorrecta → `ERR_IDE_14` sin enviar; igual al actual (aun con mayúsculas) → `ERR_IDE_18`; en uso → `ERR_IDE_01`; falla el envío → se propaga y no queda token.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/SolicitarCambioCorreoComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/SolicitarCambioCorreoUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/SolicitarCambioCorreoService.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/SolicitarCambioCorreoServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Confirmar el enlace (UT 10) y los endpoints (UT 13).
- El aviso al correo anterior (se envía al confirmar, UT 10).
- Límite de reenvíos por cuenta (lo cubre el límite de peticiones del gateway).

## Resultado esperado

Pedir el cambio deja un enlace guardado y un correo enviado a la dirección nueva, sin tocar el correo de la cuenta.

## Validación

1. `./mvnw test` en `identidad` pasa con `SolicitarCambioCorreoServiceTest` en verde.
2. `ERR_IDE_18` documentado con HTTP 409.
3. `./mvnw checkstyle:check` sin errores.
