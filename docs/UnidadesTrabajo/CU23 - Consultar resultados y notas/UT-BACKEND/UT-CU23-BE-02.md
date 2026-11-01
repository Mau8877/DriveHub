# UT-CU23-BE-02 — Controller y diseño de BD

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-BE-02` |
| **Nombre** | Controller y diseño de BD |
| **Caso de uso** | CU23-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU23-BE-01 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La app y la web necesitan una API para consultar notas.

## Objetivo

`/api/v1/resultados/mios` y `/api/v1/resultados` con el sobre estándar.

## Alcance

- `ResultadoController` delgado (dos endpoints, ver README); la persona y la escuela salen del token; el Administrador sin escuela en el token recibe `ERR_ACA_16`.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: el índice nuevo de `resultados`.
- `ResultadoControllerTest` (`@WebMvcTest`, 5 casos).

Archivos esperados (crear/tocar): el controller, el `.md` de diseño y el test.

## Fuera de alcance (no tocar)

- El gateway (`resultados` ya está enrutado), las pantallas y los errores (no hay códigos nuevos).

## Resultado esperado

Un estudiante lista sus notas y un Administrador las de su escuela con nombres.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico`.
2. Por el gateway: estudiante en `/resultados/mios` 200, en `/resultados` 403; administrador al revés; sin token 401.
