# UT-CU04b-BE-02 — Persistencia de los tokens de correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-BE-02` |
| **Nombre** | Persistencia de los tokens de correo |
| **Caso de uso** | CU04b-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU04b-BE-01 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md)

## Solicitud de negocio

Los enlaces de verificación (y luego los de recuperación) deben guardarse solo como hash, de un solo uso y reemplazables.

## Objetivo

Existe el puerto y el adaptador para guardar, buscar, invalidar y consumir tokens de correo.

## Alcance

- `domain/port/out/TokenRecuperacionRepositoryPort`: `TokenRecuperacion guardar(TokenRecuperacion token)`, `Optional<TokenRecuperacion> buscarPorHash(String tokenHash)`, `void invalidarPendientes(UUID usuarioCodigo, TipoTokenCorreo tipo)` (marca `usado = true` los vigentes de ese usuario y tipo) y `void marcarUsado(TokenRecuperacion token)`.
- Spring Data `TokenRecuperacionJpaRepository` y `TokenRecuperacionPersistenceAdapter` con su mapper: resuelve el `usuario_id` interno a partir de `usuarioCodigo`; el id Long no sale del adaptador.
- Tests de persistencia (Testcontainers con el patrón `assumeTrue`): guardar y buscar por hash; `invalidarPendientes` solo toca el usuario y el tipo indicados; `marcarUsado` deja `usado = true`; el hash es único.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/port/out/TokenRecuperacionRepositoryPort.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/TokenRecuperacionJpaRepository.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/TokenRecuperacionPersistenceAdapter.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/repository/TokenRecuperacionPersistenceAdapterTest.java`

## Fuera de alcance (no tocar)

- Servicios de verificación o recuperación y el envío de correo.
- Un job que borre tokens vencidos (seguimiento).

## Resultado esperado

Los tokens de correo se guardan y consumen de forma segura, listos para los casos de uso.

## Validación

1. `./mvnw test` en `identidad` pasa con el test del adaptador en verde.
2. `./mvnw checkstyle:check` sin errores.
