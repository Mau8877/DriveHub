# UT-CU06-BE-06 — Ver y editar el perfil

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-BE-06` |
| **Nombre** | Ver y editar el perfil |
| **Caso de uso** | CU06-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

La persona ve sus datos y corrige los personales sin pasar por nadie.

## Objetivo

Dos casos de uso, `ObtenerPerfilUseCase` y `EditarPerfilUseCase`, con sus servicios, que devuelven el perfil completo.

## Alcance

- `domain/port/in/PerfilUsuario` (`record`): `codigo`, `nombres`, `apellidos`, `fechaNacimiento`, `telefono`, `email`, `correoVerificado`, `roles` (`List<String>`), `escuelaCodigo`, `escuelaNombre`, `fotoUrl`.
- `domain/port/in/ObtenerPerfilUseCase`: `PerfilUsuario obtener(UUID usuarioCodigo)`.
- `domain/port/in/EditarPerfilComando` (`record`: `usuarioCodigo`, y `nombres`, `apellidos`, `telefono`, `fechaNacimiento` con "presente/ausente", por ejemplo `Optional` o una bandera por campo) y `EditarPerfilUseCase`: `PerfilUsuario editar(EditarPerfilComando)`.
- `application/service/PerfilArmador` (clase interna reutilizable de `application/service`, sin `@Service` propio si no hace falta): arma el `PerfilUsuario` desde `Usuario`; `fotoUrl` = `AlmacenFotosPort.urlDeLectura(clave)` si hay foto; `escuelaNombre` con `EscuelaConsultaPort.buscarPorCodigo` solo si la cuenta tiene escuela — si Escuela falla, `escuelaNombre = null` y `WARN`. Lo usan también las UT 07 y 11.
- `application/service/ObtenerPerfilService` (`@Transactional(readOnly = true)`): cuenta inexistente → `UnauthorizedException` con `ERR_IDE_09` (igual que CU04b).
- `application/service/EditarPerfilService` (`@Transactional`): aplica solo los campos presentes; nombres y apellidos recortados y no vacíos; teléfono vacío o `null` lo borra; si cambia la fecha de nacimiento, edad ≥ `app.registro.edad-minima` (18) o `ValidationException` `ERR_IDE_02` (el mismo código del registro). Guarda con `UsuarioRepositoryPort.actualizar` (si hoy solo persiste el estado de acceso, extender su alcance a estos campos, sin cambiar su firma).
- Logs: `INFO` al editar con `module="perfil"` y `usuarioCodigo`, y los **nombres de los campos** modificados; nunca los valores de nombres, teléfono o fecha.
- Tests unitarios (puertos mockeados): obtiene con y sin escuela; Escuela caído → `escuelaNombre` nulo sin error; edita solo los campos presentes; teléfono vacío lo borra; menor de 18 → `ERR_IDE_02` sin guardar; cuenta inexistente → `ERR_IDE_09`; el `PerfilUsuario` devuelto no incluye `passwordHash` ni el id Long.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/PerfilUsuario.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/ObtenerPerfilUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/EditarPerfilComando.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/in/EditarPerfilUseCase.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/PerfilArmador.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/ObtenerPerfilService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/EditarPerfilService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/UsuarioPersistenceAdapter.java` (si `actualizar` hay que ampliarlo)
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/ObtenerPerfilServiceTest.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/EditarPerfilServiceTest.java`

## Fuera de alcance (no tocar)

- Foto, contraseña y correo (UT 07, 08, 09, 10). `editar` no recibe `email`, `passwordHash` ni la foto.
- Los endpoints HTTP (UT 12) y `fotoUrl` en login (UT 11).
- Cambiar roles, escuela o estado de la cuenta.

## Resultado esperado

Obtener y editar el perfil de la cuenta propia funciona desde los casos de uso, con la edad mínima validada.

## Validación

1. `./mvnw test` en `identidad` pasa con `ObtenerPerfilServiceTest` y `EditarPerfilServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
