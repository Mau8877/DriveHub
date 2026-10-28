# UT-CU22-BE-03 — Controller, errores documentados y diseño de BD

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU22-BE-03` |
| **Nombre** | Controller, errores documentados y diseño de BD |
| **Caso de uso** | CU22-BE |
| **Microservicio** | `academico` |
| **Depende de** | UT-CU22-BE-02 |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La web necesita una API para gestionar los exámenes de la escuela de quien inició sesión.

## Objetivo

`/api/v1/examenes-conduccion` expuesto con el sobre estándar y sus errores y campos documentados.

## Alcance

- `ExamenConduccionController` delgado (siete endpoints, ver README); escuela, autor y Bearer salen del token.
- `docs/errors/academico/ERRORES_ACADEMICO.md` y su índice (`ERR_ACA_24` a `ERR_ACA_27`).
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: campos nuevos de `examenes_conduccion` y `resultados`.
- `.claude/rules/MICROSERVICIOS.md`: la llamada Académico → Identidad de CU22.
- `ExamenConduccionControllerTest` (`@WebMvcTest`).

Archivos esperados (crear/tocar): el controller, los `.md` de errores, de diseño y de rules, y el test.

## Fuera de alcance (no tocar)

- El gateway: `examenes-conduccion` ya está enrutado.
- La pantalla (UT-FRONTEND).

## Resultado esperado

Un Administrador programa, lista, reprograma y cierra exámenes de su escuela; otra escuela no ve nada.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico`.
2. Por el gateway con `adminexample@gmail.com`: programar 201, fecha pasada 400, repetir categoría 409 `ERR_ACA_26`, estudiante ajeno 404 `ERR_ACA_27`, registrar resultado 200 y volver a cancelar 409 `ERR_ACA_25`, sin token 401.
