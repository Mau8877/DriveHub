# UT-CU04b-BE-05 — `ConfirmarCorreoService` y `ERR_IDE_11`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-05` |
| **Nombre** | `ConfirmarCorreoService` y `ERR_IDE_11` |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04b-BE-01, UT-CU04b-BE-02 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Abrir el enlace del correo debe dejar el correo verificado, una sola vez.

## Objetivo

`ConfirmarCorreoService` valida el enlace y marca el correo como verificado.

## Alcance

- `domain/port/in/ConfirmarCorreoUseCase` con `void confirmar(String token)`.
- `IdentidadErrorCodes.ENLACE_INVALIDO = ERR_IDE_11` (400 `ValidationException`, mensaje "El enlace no es válido o ya venció"; lo reutiliza CU04).
- `application/service/ConfirmarCorreoService`, `@Transactional(noRollbackFor = BusinessException.class)`: hashea el token y lo busca; si no existe, `usado`, vencido o su `tipo` no es `VERIFICACION` → `ERR_IDE_11`. Si sirve: `marcarUsado`, y actualiza la cuenta con `correoVerificado = true` (`UsuarioRepositoryPort.actualizar`).
- Logs: `INFO` al verificar (`usuarioCodigo`), `WARN` con `errorCode` si el enlace no sirve. Nunca el token.
- Documentar `ERR_IDE_11` en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y su índice.
- Tests unitarios: enlace válido verifica y consume; token inexistente, usado, vencido y de tipo `RECUPERACION` → `ERR_IDE_11` sin tocar la cuenta; reutilizar el mismo enlace → `ERR_IDE_11`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/ConfirmarCorreoUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ConfirmarCorreoService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/ConfirmarCorreoServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Endpoints (UT 07).
- Restablecer contraseña con tokens de tipo `RECUPERACION` (CU04).

## Resultado esperado

Un enlace válido verifica el correo una sola vez; cualquier otro responde `ERR_IDE_11`.

## Validación

1. `./mvnw test` en `identidad` pasa con `ConfirmarCorreoServiceTest` en verde.
2. `ERR_IDE_11` documentado con HTTP 400.
3. `./mvnw checkstyle:check` sin errores.
