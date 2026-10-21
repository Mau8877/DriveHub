# UT-CU04b-BE-04 — `SolicitarVerificacionCorreoService` y `ERR_IDE_10`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-04` |
| **Nombre** | `SolicitarVerificacionCorreoService` y `ERR_IDE_10` |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04b-BE-01, UT-CU04b-BE-02, UT-CU04b-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La persona con sesión pide el correo de verificación y recibe un enlace de un solo uso que reemplaza a los anteriores.

## Objetivo

`SolicitarVerificacionCorreoService` genera el enlace y manda el correo a la cuenta.

## Alcance

- `domain/port/in/SolicitarVerificacionCorreoUseCase` con `void solicitar(UUID usuarioCodigo)`.
- `IdentidadErrorCodes.CORREO_YA_VERIFICADO = ERR_IDE_10` (409 `ConflictException`).
- `application/service/SolicitarVerificacionCorreoService`, `@Transactional`: busca la cuenta (si no existe → `UnauthorizedException` con `ERR_IDE_09`); si `correoVerificado` → `ERR_IDE_10`; `GeneradorTokenOpacoPort.generar()` y `hashear()`; `invalidarPendientes(usuario, VERIFICACION)`; guarda el token (`tipo = VERIFICACION`, vigencia `app.auth.verificacion-ttl-horas` = 24 h); arma el enlace `${app.web.url}/account/verify-email?token=<token en claro>` y llama a `EnviadorCorreoPort`. Si el envío falla, la excepción se propaga y la transacción se revierte (el enlace no queda guardado).
- Logs: `INFO` con `module="auth"` y `usuarioCodigo`. Nunca el token ni el enlace.
- Documentar `ERR_IDE_10` en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y su índice en `docs/errors/README.md`.
- Tests unitarios: envía a la dirección de la cuenta con el enlace correcto; invalida los pendientes antes de guardar; guarda solo el hash con tipo `VERIFICACION` y 24 h; ya verificado → `ERR_IDE_10` sin enviar; cuenta inexistente → `ERR_IDE_09`; fallo del envío se propaga.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/SolicitarVerificacionCorreoUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/SolicitarVerificacionCorreoService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/SolicitarVerificacionCorreoServiceTest.java`
- `projects/backend/identidad/src/main/resources/application.properties`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Confirmar el enlace (UT 05) y los endpoints (UT 07).
- Límite de reenvíos por cuenta (lo cubre el límite de peticiones del gateway).

## Resultado esperado

Pedir la verificación deja un token válido guardado y un correo enviado a la cuenta.

## Validación

1. `./mvnw test` en `identidad` pasa con `SolicitarVerificacionCorreoServiceTest` en verde.
2. `ERR_IDE_10` documentado con HTTP 409.
3. `./mvnw checkstyle:check` sin errores.
