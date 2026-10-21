# UT-CU05-BE-03 — `CrearAdministradorEscuelaService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-03` |
| **Nombre** | `CrearAdministradorEscuelaService` |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El Super admin crea la escuela y su Administrador a la vez; la cuenta del Administrador vive en Identidad.

## Objetivo

Identidad sabe crear una cuenta `ADMIN_ESCUELA` asociada a un código de escuela.

## Alcance

- Puerto de entrada `CrearAdministradorEscuelaUseCase` con un método `Usuario crear(CrearAdministradorEscuelaComando)`; `CrearAdministradorEscuelaComando` (record: `nombres`, `apellidos`, `fechaNacimiento`, `email`, `password`, `telefono`, `escuelaCodigo`). Sin campo de rol a propósito: siempre es `ADMIN_ESCUELA`.
- `application/service/CrearAdministradorEscuelaService`, `@Transactional` y `@PreAuthorize("hasRole('SUPER_ADMIN')")` (la decisión de rol vive en el caso de uso, no en el controller). Orden: normaliza el correo (`trim` + minúsculas); edad mínima con la misma propiedad que el registro (`app.registro.edad-minima`) → `ValidationException` `ERR_IDE_02`; `existePorEmail` → `ConflictException` `ERR_IDE_01`; `existeAdministradorDeEscuela(escuelaCodigo)` → `ConflictException` `ERR_IDE_12`; busca el rol `ADMIN_ESCUELA`; guarda la cuenta `ACTIVO`, `correoVerificado = false`, contraseña hasheada con `PasswordHasherPort`.
- **No consulta a Escuela.** La escuela todavía no existe en el momento de la llamada (Escuela confirma su transacción después de que este servicio responde); mientras no exista, la cuenta no puede iniciar sesión porque el login verifica la escuela.
- Log `INFO` "Administrador de escuela creado" con `usuarioCodigo` y `escuelaCodigo`; **nunca** la contraseña ni su hash.
- `UsuarioRepositoryPort.existeAdministradorDeEscuela(UUID escuelaCodigo)` y su implementación en `UsuarioPersistenceAdapter` (una cuenta con rol `ADMIN_ESCUELA` y ese `escuela_codigo`).
- `IdentidadErrorCodes.ADMIN_YA_EXISTE_EN_ESCUELA = "ERR_IDE_12"`, documentado en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y en el índice `docs/errors/README.md` (HTTP 409, `ConflictException`, "La escuela ya tiene un Administrador", lo lanza `CrearAdministradorEscuelaService.crear()`).
- Tests unitarios con los puertos mockeados: caso feliz (rol, `escuelaCodigo`, estado, hash distinto de la contraseña, correo normalizado); correo en uso; menor de edad; escuela con Administrador; el servicio no depende de `EscuelaConsultaPort`; la contraseña no aparece en ningún log.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/CrearAdministradorEscuelaUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/CrearAdministradorEscuelaComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/CrearAdministradorEscuelaService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/UsuarioRepositoryPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioPersistenceAdapter.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/CrearAdministradorEscuelaServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint HTTP (UT 05) y revertir la cuenta (UT 04).
- Enviar correo de invitación o de verificación: la cuenta nace sin verificar y la contraseña la pone el Super admin.
- Cambios en el registro del estudiante ni en el login.

## Resultado esperado

Existe el caso de uso que crea al Administrador de una escuela con sus reglas de negocio.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
