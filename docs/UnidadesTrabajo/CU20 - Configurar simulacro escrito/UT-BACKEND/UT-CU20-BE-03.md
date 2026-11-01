# UT-CU20-BE-03 — Servicio y reglas de los simulacros

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-03` |
| **Nombre** | Servicio y reglas de los simulacros |
| **Caso de uso** | CU20-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU20-BE-01, UT-CU20-BE-02 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (`@PreAuthorize`).

## Solicitud de negocio

Un simulacro mal armado (cantidades que no suman, temas repetidos, nombre duplicado) no puede llegar a los estudiantes.

## Objetivo

La lógica de crear, listar, obtener, editar y activar o desactivar simulacros de una escuela.

## Alcance

- DTOs: `CrearSimulacroRequest`, `EditarSimulacroRequest`, `ReglaRequest`, `SimulacroResponse` (con `ReglaResponse`).
- `ReglasSimulacro` (package-private): nombre, categoría, duración, número de preguntas, puntaje y reglas (temas distintos, cantidades que suman el número).
- `SimulacroService`: `crear`, `listar` (página 1 y 20 por defecto, 100 máximo), `obtener`, `editar` (parcial con revalidación) y `cambiarActivo` (409 `ERR_ACA_22` si ya estaba así); nombre único por escuela (`ERR_ACA_23`); 404 `ERR_ACA_21`; `@PreAuthorize("hasRole('ADMIN_ESCUELA')")`; logs `INFO` y `WARN` con `errorCode`.
- Tests: `SimulacroServiceTest` (14 casos, repositorio mockeado).

Archivos esperados (crear/tocar): cuatro DTOs, `ReglasSimulacro.java`, `SimulacroService.java`, `SimulacroServiceTest.java`.

## Fuera de alcance (no tocar)

- El controller (UT-04).
- Consultar el banco de preguntas para avisar de temas con pocas preguntas.

## Resultado esperado

Ningún dato inválido se guarda y cada rechazo tiene su código.

## Validación

1. `./mvnw test -Dtest=SimulacroServiceTest` en `academico`.
2. `./mvnw checkstyle:check` sin errores.
