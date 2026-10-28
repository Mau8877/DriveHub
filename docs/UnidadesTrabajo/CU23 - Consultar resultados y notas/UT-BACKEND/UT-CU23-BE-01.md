# UT-CU23-BE-01 — Repositorio y servicio de resultados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-01` |
| **Nombre** | Repositorio y servicio de resultados |
| **Caso de uso** | CU23-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md).

## Solicitud de negocio

Cada quien ve solo las notas que le corresponden.

## Objetivo

Consultas de notas por estudiante y por escuela, con filtros y paginación.

## Alcance

- `FiltroResultados`, `ResultadoRepositoryCustom` y `...Impl` (`MongoTemplate`: alcance por estudiante o por escuela, sin eliminadas, filtros de origen y estudiante, la más reciente primero), y `ResultadoRepository` que los extiende.
- `ResultadoResponse` y `ResultadoService` (`listarMios`, `listarDeEscuela` con nombres de Identidad, página 1 y 20 por defecto, 100 máximo).
- `Resultado`: índice `idx_resultado_escuela_fecha`.
- `ResultadoServiceTest` (3 casos).

Archivos esperados (crear/tocar): los de `resultados/{repository,dto,service}`, `Resultado.java` y el test.

## Fuera de alcance (no tocar)

- El controller (UT-02) y la escritura de notas.

## Resultado esperado

Ninguna consulta devuelve notas de otro estudiante ni de otra escuela.

## Validación

1. `./mvnw test -Dtest=ResultadoServiceTest` y `./mvnw checkstyle:check` en `academico`.
