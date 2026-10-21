# UT-CU02-BE-07 — Adaptadores de persistencia: usuarios y sesiones renovables

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-BE-07` |
| **Nombre** | Adaptadores de persistencia: usuarios y sesiones renovables |
| **Caso de uso** | CU02-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU02-BE-04 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

El inicio de sesión necesita leer y actualizar cuentas y guardar, buscar y revocar las sesiones renovables en PostgreSQL.

## Objetivo

`UsuarioRepositoryPort` ampliado y `RefreshTokenRepositoryPort` están implementados sobre PostgreSQL.

## Alcance

- `UsuarioPersistenceAdapter`: `buscarPorEmail`, `buscarPorCodigo` (con sus roles cargados) y `actualizar` (intentos, estado, `bloqueadoHasta`, `ultimoLogin`) sin tocar el correo ni el hash.
- `RefreshTokenJpaRepository` (`findByTokenHash`, consulta para revocar los activos de un usuario y dispositivo), `RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort` y mapper propio. `RefreshTokenModel`: constructor y `@Setter` de `revocado`.
- `guardar` genera el `codigo_refresh_token` con UUID v7 (por `BaseModel`) y asigna el `usuario_id` desde el `usuarioCodigo`.
- Tests con Postgres real (base `PostgresIntegrationTestBase`): `buscarPorEmail` devuelve roles y escuela; `actualizar` persiste el bloqueo; `revocarActivos` revoca solo los del dispositivo pedido y deja los del otro; `buscarPorHash` encuentra y no encuentra.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/` (adaptadores, JpaRepository y mappers)
- `.../persistence/model/RefreshTokenModel.java`
- `src/test/java/.../RefreshTokenPersistenceAdapterTest.java`, `UsuarioPersistenceAdapterTest.java`

## Fuera de alcance (no tocar)

- Borrar sesiones vencidas (job pendiente).
- Persistencia de `token_recuperacion` o dispositivos push.
- Lógica de reglas: viven en los servicios.

## Resultado esperado

El servicio puede guardar, buscar y revocar sesiones renovables y actualizar cuentas en la base.

## Validación

1. `./mvnw test` en `identidad` pasa (con Docker disponible).
2. `./mvnw checkstyle:check` sin errores.
