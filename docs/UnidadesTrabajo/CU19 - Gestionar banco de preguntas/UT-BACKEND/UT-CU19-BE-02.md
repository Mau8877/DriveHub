# UT-CU19-BE-02 — Repositorio y listado de preguntas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-02` |
| **Nombre** | Repositorio y listado de preguntas |
| **Caso de uso** | CU19-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU19-BE-01 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (el repository no tiene lógica) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

El Administrador necesita encontrar rápido sus preguntas, y nunca ver las de otra escuela.

## Objetivo

Acceso a `banco_preguntas` siempre dentro de una escuela, con búsqueda, filtros y paginación.

## Alcance

- `PreguntaRepository` (`MongoRepository` + `findByEscuelaCodigoAndCodigoPreguntaAndDeletedAtIsNull` + `existsByEscuelaCodigoAndEnunciado`).
- `PreguntaRepositoryCustom` / `PreguntaRepositoryCustomImpl` (`MongoTemplate` + `Criteria`): siempre filtra `escuelaCodigo` y `deletedAt = null`; filtros opcionales de categoría, tema, tipo, dificultad y activa; búsqueda por expresión regular escapada sobre enunciado y tema; la más reciente primero; total con `PageableExecutionUtils`.
- `FiltroPreguntas` (record).

Archivos esperados (crear/tocar): los cuatro del paquete `repository/`.

## Fuera de alcance (no tocar)

- Lógica de negocio y validación (UT-03).
- Consultas de CU20 (preguntas activas por categoría y tema para armar un simulacro).

## Resultado esperado

Ninguna consulta devuelve preguntas de otra escuela ni eliminadas.

## Validación

1. `GestionPreguntasIntegrationTest` (MongoDB real con Testcontainers): `ca01_cadaEscuelaVeYTocaSoloSuBanco` y `ca01_elListadoBuscaFiltraYPagina`.
2. Buscar `.*` no encuentra nada: el texto se busca literal.
