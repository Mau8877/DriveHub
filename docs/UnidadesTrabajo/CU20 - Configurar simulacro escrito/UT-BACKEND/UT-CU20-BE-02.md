# UT-CU20-BE-02 — Repositorio y listado de simulacros

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-02` |
| **Nombre** | Repositorio y listado de simulacros |
| **Caso de uso** | CU20-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU20-BE-01 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (el repository no tiene lógica) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

El Administrador necesita encontrar rápido sus simulacros, y nunca ver los de otra escuela.

## Objetivo

Acceso a `simulacros_config` siempre dentro de una escuela, con búsqueda, filtros y paginación.

## Alcance

- `SimulacroConfigRepository` (`MongoRepository` + `findByEscuelaCodigoAndCodigoSimulacroAndDeletedAtIsNull` + `existsByEscuelaCodigoAndNombreIgnoreCaseAndDeletedAtIsNull` + `existsByEscuelaCodigoAndNombre`).
- `SimulacroConfigRepositoryCustom` / `...Impl` (`MongoTemplate` + `Criteria`): siempre filtra `escuelaCodigo` y `deletedAt = null`; filtros opcionales de categoría y activo; búsqueda por expresión regular escapada sobre el nombre; el más reciente primero; total con `PageableExecutionUtils`.
- `FiltroSimulacros` (record).

Archivos esperados (crear/tocar): los cuatro del paquete `repository/`.

## Fuera de alcance (no tocar)

- Lógica de negocio y validación (UT-03).
- La consulta de CU21 (simulacro activo por categoría).

## Resultado esperado

Ninguna consulta devuelve simulacros de otra escuela ni eliminados.

## Validación

1. `docker compose up -d academico` y listar por el gateway con dos escuelas distintas: cada una ve solo los suyos.
2. Buscar `.*` no encuentra nada: el texto se busca literal.
