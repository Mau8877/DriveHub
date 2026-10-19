# UT-CU01-BE-11 — Adaptador de persistencia de usuarios y roles

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-11` |
| **Nombre** | Adaptador de persistencia de usuarios y roles |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-09 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (mapper propio, `usuario_rol` como asociación), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (`findByCodigo`, UUID v7 generado por la aplicación) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La cuenta nueva y su rol deben guardarse en la base de Identidad, y un correo repetido debe detectarse incluso si dos registros llegan a la vez.

## Objetivo

`UsuarioRepositoryPort` y `RolRepositoryPort` están implementados sobre PostgreSQL.

## Alcance

- `UsuarioJpaRepository extends JpaRepository<UsuarioModel, Long>` (`existsByEmail`), `RolJpaRepository` (`findByNombre`) y `UsuarioRolJpaRepository` (para insertar la fila de `usuario_rol` con su `codigo_usuario_rol`).
- `UsuarioPersistenceMapper` (dominio ↔ Model) y `RolPersistenceMapper` si hace falta; el dominio nunca ve un Model.
- `UsuarioPersistenceAdapter implements UsuarioRepositoryPort`: `guardar` genera `UuidV7` para `codigo_usuario` (vía `BaseModel`) y para el `codigo_usuario_rol`, persiste el usuario y su asociación con el rol, y devuelve el `Usuario` de dominio. `existePorEmail` compara el correo ya normalizado.
- Si la BD rechaza el `INSERT` por el `UNIQUE` del correo (`DataIntegrityViolationException`), el adaptador lo traduce a `ConflictException` con `ERR_IDE_01` (carrera entre dos registros).
- `RolPersistenceAdapter implements RolRepositoryPort`.
- Tests de integración con Postgres real (Testcontainers con `assumeTrue(Docker disponible)`): guarda y recupera, la asociación `usuario_rol` queda creada, el segundo `INSERT` con el mismo correo lanza `ConflictException` `ERR_IDE_01`.

Archivos esperados (crear/tocar):

- `.../infrastructure/adapter/out/persistence/repository/` (`UsuarioJpaRepository`, `RolJpaRepository`, `UsuarioRolJpaRepository`, `UsuarioPersistenceAdapter`, `RolPersistenceAdapter`, mappers)
- `projects/backend/identidad/src/test/java/.../UsuarioPersistenceAdapterTest.java`

## Fuera de alcance (no tocar)

- Búsquedas por id, por código, por rol o listados (otros CU).
- Persistencia de `refresh_token`, `token_recuperacion` o `rol_permiso`.
- El servicio y el controller.
- El borrado lógico y la actualización de usuarios.

## Resultado esperado

Guardar un `Usuario` con el rol `ESTUDIANTE` deja una fila en `usuario` y otra en `usuario_rol`; un correo repetido produce `ERR_IDE_01`.

## Validación

1. `./mvnw test` en `identidad` pasa (con Docker; sin Docker el test queda SKIPPED).
2. `./mvnw checkstyle:check` sin errores.
3. Tras un guardado de prueba, `SELECT count(*) FROM usuario_rol` aumenta en 1.
