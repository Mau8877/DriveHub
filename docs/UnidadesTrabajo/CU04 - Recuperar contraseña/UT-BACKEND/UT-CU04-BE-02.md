# UT-CU04-BE-02 — Cerrar todas las sesiones de una cuenta

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-BE-02` |
| **Nombre** | Cerrar todas las sesiones de una cuenta |
| **Caso de uso** | CU04-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Al cambiar la contraseña, ninguna sesión abierta con la anterior debe seguir sirviendo.

## Objetivo

`RefreshTokenRepositoryPort.revocarTodos` revoca todas las sesiones renovables de una cuenta.

## Alcance

- `RefreshTokenRepositoryPort`: agregar `void revocarTodos(UUID usuarioCodigo)` (marca `revocado = true` en todas las sesiones activas del usuario, web y móvil; hoy `revocarActivos` distingue el dispositivo).
- `RefreshTokenPersistenceAdapter` (y su repositorio Spring Data): implementarlo con una sola actualización.
- Tests de persistencia (Testcontainers, patrón `assumeTrue`): revoca las de los dos dispositivos, no toca las de otro usuario ni las ya revocadas.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/RefreshTokenRepositoryPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/`

## Fuera de alcance (no tocar)

- El servicio que lo usa (UT 04).
- Cambios en login, renovación o cierre de sesión.

## Resultado esperado

Existe la operación de revocar todas las sesiones de una cuenta, probada.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
