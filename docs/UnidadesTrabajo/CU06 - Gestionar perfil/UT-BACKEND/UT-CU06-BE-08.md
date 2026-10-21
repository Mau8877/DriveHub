# UT-CU06-BE-08 — Cambiar la contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-08` |
| **Nombre** | Cambiar la contraseña |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La persona con sesión cambia su contraseña demostrando que conoce la actual, y las sesiones de sus otros dispositivos dejan de servir.

## Objetivo

`CambiarContrasenaUseCase` con su servicio y los códigos `ERR_IDE_14` y `ERR_IDE_15`.

## Alcance

- `IdentidadErrorCodes.PASSWORD_ACTUAL_INCORRECTA = ERR_IDE_14` y `PASSWORD_NUEVA_IGUAL_A_ACTUAL = ERR_IDE_15` (ambos 400, `ValidationException`).
- `domain/port/in/CambiarContrasenaComando` (`record`: `usuarioCodigo`, `passwordActual`, `passwordNueva`, `dispositivo` — el del cliente) y `CambiarContrasenaUseCase`: `void cambiar(CambiarContrasenaComando)`.
- `application/service/CambiarContrasenaService` (`@Transactional(noRollbackFor = BusinessException.class)`, como `RestablecerContrasenaService`):
  1. cuenta inexistente → `UnauthorizedException` `ERR_IDE_09`;
  2. `PasswordHasherPort` comprueba la actual; si no coincide → `ERR_IDE_14` (400). **No** suma `intentos_fallidos` ni cambia el estado;
  3. si la nueva coincide con la actual (se comprueba contra el hash) → `ERR_IDE_15`;
  4. guarda el hash de la nueva con `UsuarioRepositoryPort.actualizar`;
  5. revoca la sesión del **otro** dispositivo con `RefreshTokenRepositoryPort.revocarActivos(usuarioCodigo, otro)` (si `dispositivo = WEB`, el otro es `MOVIL`, y al revés). La sesión del dispositivo del cliente sigue activa.
- Las reglas de formato de la contraseña nueva (9 a 64 caracteres) las valida `@Valid` en el endpoint (UT 12), no este servicio.
- Logs: `INFO` con `module="auth"` y `usuarioCodigo` al cambiar; `WARN` con `errorCode` en los rechazos. **Nunca** ninguna de las dos contraseñas.
- Documentar `ERR_IDE_14` y `ERR_IDE_15` en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y su índice en `docs/errors/README.md`.
- Tests unitarios: cambia y guarda un hash nuevo; actual incorrecta → `ERR_IDE_14` sin cambiar nada ni tocar los intentos; igual a la actual → `ERR_IDE_15`; revoca solo el otro dispositivo en ambos sentidos; cuenta inexistente → `ERR_IDE_09`; ningún log contiene las contraseñas.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/CambiarContrasenaComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/CambiarContrasenaUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/CambiarContrasenaService.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/CambiarContrasenaServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint y las reglas de `@Valid` (UT 12).
- Cerrar la sesión del dispositivo actual o cambiar el JWT.
- Recuperar la contraseña sin la actual ([CU04](../../CU04%20-%20Recuperar%20contrase%C3%B1a/UT-BACKEND/README.md)).

## Resultado esperado

La contraseña cambia con la actual correcta; la sesión del otro dispositivo se cierra y la actual sigue.

## Validación

1. `./mvnw test` en `identidad` pasa con `CambiarContrasenaServiceTest` en verde.
2. `ERR_IDE_14` y `ERR_IDE_15` documentados con HTTP 400.
3. `./mvnw checkstyle:check` sin errores.
