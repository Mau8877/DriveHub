# UT-CU04-BE-04 — `RestablecerContrasenaService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-04` |
| **Nombre** | `RestablecerContrasenaService` |
| **Caso de uso** | CU04-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04-BE-02 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Este CU se apoya en lo que construye [CU04b](../../CU04b%20-%20Verificar%20correo/UT-BACKEND/README.md) (`EnviadorCorreoPort`, tokens con `tipo`, `correo_verificado`, `ERR_IDE_11`, rutas `/account`): debe estar implementado antes de empezar.

## Solicitud de negocio

La persona fija una contraseña nueva con el enlace; las sesiones anteriores dejan de servir y la cuenta se desbloquea.

## Objetivo

`RestablecerContrasenaService` valida el enlace, cambia la contraseña y cierra las sesiones.

## Alcance

- `domain/port/in/RestablecerContrasenaUseCase` con `void restablecer(String token, String nuevaPassword)`.
- `application/service/RestablecerContrasenaService`, `@Transactional(noRollbackFor = BusinessException.class)`: hashea el token y lo busca; si no existe, `usado`, vencido, de tipo `VERIFICACION` o la cuenta está `INACTIVO` → `ERR_IDE_11` (el de CU04b; no se agrega código nuevo). Si sirve: guarda la contraseña nueva con `PasswordHasherPort` (BCrypt, igual que el registro), reinicia `intentosFallidos`, quita `bloqueadoHasta` y, si la cuenta estaba `BLOQUEADO`, la deja `ACTIVO`; `marcarUsado`; `invalidarPendientes(RECUPERACION)`; `RefreshTokenRepositoryPort.revocarTodos`.
- Logs: `INFO` al restablecer (`usuarioCodigo`), `WARN` con `errorCode` si el enlace no sirve. Nunca la contraseña ni el token.
- `UsuarioPersistenceAdapter.actualizar` ahora también persiste `passwordHash` (hasta ahora solo guardaba estado, intentos, bloqueo, último ingreso y correo verificado), con su test en `UsuarioPersistenceAdapterTest`.
- Tests unitarios: caso exitoso (nueva contraseña hasheada y distinta, contador en 0, sin bloqueo, `BLOQUEADO` pasa a `ACTIVO`, token usado, sesiones revocadas); token inexistente, usado, vencido, de verificación y cuenta inactiva → `ERR_IDE_11` sin cambios; la contraseña en claro nunca se guarda.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/RestablecerContrasenaUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/RestablecerContrasenaService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioPersistenceAdapter.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/RestablecerContrasenaServiceTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioPersistenceAdapterTest.java`

## Fuera de alcance (no tocar)

- Validar el formato de la contraseña: lo hace el DTO del endpoint (UT 05) con las mismas reglas del registro.
- Iniciar sesión automáticamente tras restablecer.
- Códigos de error nuevos.

## Resultado esperado

Con un enlace válido la contraseña cambia, el enlace se consume y todas las sesiones previas quedan revocadas.

## Validación

1. `./mvnw test` en `identidad` pasa con `RestablecerContrasenaServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
