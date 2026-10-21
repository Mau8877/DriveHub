# UT-CU05-BE-04 — `RevertirAdministradorEscuelaService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU05-BE-04` |
| **Nombre** | `RevertirAdministradorEscuelaService` |
| **Caso de uso** | CU05-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU05-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Si algo falla después de crear al Administrador, la creación de la escuela debe poder deshacerlo para no dejar una cuenta huérfana ni un correo bloqueado.

## Objetivo

Identidad borra de forma definitiva al Administrador de una escuela que nunca llegó a usarse.

## Alcance

- Puerto `RevertirAdministradorEscuelaUseCase.revertir(UUID escuelaCodigo)` y `application/service/RevertirAdministradorEscuelaService`, `@Transactional` y `@PreAuthorize("hasRole('SUPER_ADMIN')")`.
- Es **idempotente**: si la escuela no tiene Administrador, no hace nada y no falla (un reintento o una compensación doble son inofensivos). Si lo tiene y **ya inició sesión alguna vez** (`ultimo_login` no es nulo) → `ConflictException` `ERR_IDE_13`: esa cuenta ya se usó y no se revierte.
- El borrado es **físico, a propósito**: el correo es `UNIQUE` incluso entre cuentas con borrado lógico, y un `deleted_at` impediría volver a crear la escuela con el mismo correo. Se limita a una cuenta `ADMIN_ESCUELA` sin uso, así que no pierde nada.
- `UsuarioRepositoryPort.buscarAdministradorDeEscuela(UUID escuelaCodigo)` y `eliminarDefinitivamente(UUID usuarioCodigo)`; en el adaptador, `@Modifying` nativos que borran en orden las filas hijas (`usuario_rol`, `refresh_token`, `token_recuperacion`) y luego `usuario` (el esquema no tiene `ON DELETE CASCADE`).
- Log `INFO` con `usuarioCodigo` y `escuelaCodigo` ("Administrador revertido"); `WARN` con `errorCode` si ya se usó.
- `IdentidadErrorCodes.ADMIN_EN_USO = "ERR_IDE_13"`, documentado en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y `docs/errors/README.md` (409, `ConflictException`, "El Administrador ya inició sesión y no se puede revertir", `RevertirAdministradorEscuelaService.revertir()`).
- Tests: unitarios (sin Administrador → no-op; usado → `ERR_IDE_13`; sin uso → `eliminarDefinitivamente`) y de repositorio con el patrón de Docker: tras borrar, el correo vuelve a estar libre y no quedan filas hijas.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/RevertirAdministradorEscuelaUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/RevertirAdministradorEscuelaService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/UsuarioRepositoryPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioPersistenceAdapter.java`
- `los repositorios Spring Data de `usuario`, `usuario_rol`, `refresh_token` y `token_recuperacion` (consultas de borrado)`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/common/exception/IdentidadErrorCodes.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/RevertirAdministradorEscuelaServiceTest.java`
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- El endpoint HTTP (UT 05).
- Borrar o desactivar cuentas en general: este borrado físico es solo para un Administrador sin uso.
- Quien llama a la compensación (UT 08).

## Resultado esperado

Identidad puede deshacer la creación de un Administrador que nunca se usó, sin dejar rastro.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
