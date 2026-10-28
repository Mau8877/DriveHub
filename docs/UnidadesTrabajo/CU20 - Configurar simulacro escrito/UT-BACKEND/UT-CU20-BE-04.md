# UT-CU20-BE-04 — Controller, errores documentados y seed

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU20-BE-04` |
| **Nombre** | Controller, errores documentados y seed |
| **Caso de uso** | CU20-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU20-BE-03 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La web necesita una API para configurar los simulacros de la escuela de quien inició sesión, y datos de ejemplo para desarrollar.

## Objetivo

`/api/v1/simulacros` expuesto con el sobre estándar, errores documentados y dos simulacros de ejemplo.

## Alcance

- `SimulacroController` delgado (seis endpoints, ver README); la escuela y el autor salen del token (`ERR_ACA_16` si falta); `filter[...]` con `@RequestParam(name = "filter[x]")`.
- `docs/errors/academico/ERRORES_ACADEMICO.md` (`ERR_ACA_21` a `ERR_ACA_23`) y su índice en `docs/errors/README.md`.
- `SimulacroSeeder` (`@Profile("dev")`, idempotente por nombre): dos simulacros de `escuelaexample` con los temas que siembra `PreguntaSeeder`.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: campos nuevos de `simulacros_config`.
- Tests: `SimulacroControllerTest` (`@WebMvcTest`) y `SimulacroSeederTest`.

Archivos esperados (crear/tocar): el controller, el seeder, los `.md` de errores y de diseño y los dos tests.

## Fuera de alcance (no tocar)

- El gateway: `simulacros` ya está enrutado.
- La pantalla (UT-FRONTEND).

## Resultado esperado

Con el stack levantado, un Administrador crea, lista, edita, desactiva y activa simulacros; otra escuela no ve nada.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico`.
2. Por el gateway con `adminexample@gmail.com`: el listado trae 2, crear 201, cantidades que no suman 400 `ERR_SYS_01`, nombre repetido 409 `ERR_ACA_23`, desactivar dos veces 409 `ERR_ACA_22`, código inexistente 404 `ERR_ACA_21`, sin token 401 y `superadmin@gmail.com` 403.
