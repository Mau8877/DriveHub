# UT-CU21-BE-03 — Controller, errores documentados y diseño de BD

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-BE-03` |
| **Nombre** | Controller, errores documentados y diseño de BD |
| **Caso de uso** | CU21-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU21-BE-02 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La app móvil necesita una API para rendir simulacros desde la cuenta del estudiante.

## Objetivo

`/api/v1/simulacros/disponibles` y `/api/v1/intentos-simulacro` expuestos con el sobre estándar y documentados.

## Alcance

- `IntentoSimulacroController` delgado (cinco endpoints, ver README); estudiante y escuela salen del token (`@CurrentUserId`, `@CurrentEscuelaId`).
- `docs/errors/academico/ERRORES_ACADEMICO.md` y su índice (`ERR_ACA_28` a `ERR_ACA_31`).
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: campos nuevos de `intentos_simulacro`.
- `IntentoSimulacroControllerTest` (`@WebMvcTest`).

Archivos esperados (crear/tocar): el controller, los `.md` de errores y de diseño y el test.

## Fuera de alcance (no tocar)

- El gateway y la pantalla móvil.

## Resultado esperado

Un estudiante lista, inicia, finaliza y consulta intentos de su escuela; las respuestas correctas no salen al iniciar.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico`.
2. Por el gateway como estudiante: disponibles 200, iniciar 201 sin `correcta` en las opciones, iniciar de nuevo 409 `ERR_ACA_29`, finalizar 200 con corrección, administrador 403, sin token 401.
