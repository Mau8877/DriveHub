# UT-CU19-BE-04 — Controller, errores documentados y seed

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-04` |
| **Nombre** | Controller, errores documentados y seed |
| **Caso de uso** | CU19-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU19-BE-03 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La web necesita una API para gestionar el banco de la escuela de quien inició sesión, y datos de ejemplo para desarrollar.

## Objetivo

`/api/v1/preguntas` expuesto con el sobre estándar, errores documentados y 12 preguntas de ejemplo.

## Alcance

- `PreguntaController` delgado (seis endpoints, ver README); la escuela y el autor salen del token (`ERR_ACA_03` si falta); `filter[...]` con `@RequestParam(name = "filter[x]")`.
- `docs/errors/academico/ERRORES_ACADEMICO.md` y su índice en `docs/errors/README.md`.
- `PreguntaSeeder` (`@Profile("dev")`, idempotente por enunciado): 12 preguntas de `escuelaexample`, los tres tipos, cuatro categorías, tres dificultades.
- Tests: `PreguntaControllerTest` (`@WebMvcTest`), `GestionPreguntasIntegrationTest` (MongoDB real, un método por criterio de aceptación) y `PreguntaSeederTest`.

Archivos esperados (crear/tocar): el controller, el seeder, los `.md` de errores y los tres tests.

## Fuera de alcance (no tocar)

- El gateway: `preguntas` ya está enrutado.
- La pantalla (UT-FRONTEND).

## Resultado esperado

Con el stack levantado, un Administrador crea, lista, edita, desactiva y activa preguntas; otra escuela no ve nada.

## Validación

1. `./mvnw test` (39 tests) y `./mvnw checkstyle:check` en `academico`.
2. `docker compose up -d academico`; por el gateway con `adminexample@gmail.com`: el listado trae 12, crear 201, opción única sin correcta 400, desactivar dos veces 409 `ERR_ACA_02`, código inexistente 404 `ERR_ACA_01`, sin token 401 y `superadmin@gmail.com` 403.
