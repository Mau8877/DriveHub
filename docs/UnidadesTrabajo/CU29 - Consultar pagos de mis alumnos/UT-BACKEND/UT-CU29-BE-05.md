# UT-CU29-BE-05 — Controller de horas de los alumnos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-BE-05` |
| **Nombre** | Controller de horas de los alumnos |
| **Caso de uso** | CU29-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU29-BE-04 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La app del instructor necesita pedir sus alumnos y el detalle de cada uno.

## Objetivo

`GET /api/v1/horas-alumnos` y `GET /api/v1/horas-alumnos/{estudianteCodigo}`.

## Alcance

- `HorasAlumnosController` (`features/billetera/controller/`): `GET /api/v1/horas-alumnos` (`page`, `pageSize`, `search`) con `data` array y `meta`; `GET /api/v1/horas-alumnos/{estudianteCodigo}`. Con `@CurrentUserId` (el instructor) y `@CurrentEscuelaId`. Delgado, sin lógica ni logs.
- Tests `@WebMvcTest`: el sobre, la paginación, la búsqueda; el detalle `200`, `404 ERR_PAG_12`, `503 ERR_PAG_15`; `401` sin sesión y `403` para `ESTUDIANTE` y `ADMIN_ESCUELA`; el cuerpo no trae ids internos.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/controller/HorasAlumnosController.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/HorasAlumnosControllerTest.java

## Fuera de alcance (no tocar)

- El gateway (UT 06).
- Acciones sobre el alumno: es una pantalla de solo lectura.

## Resultado esperado

El instructor puede listar a sus alumnos con sus horas y abrir el detalle de uno.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el token del instructor de ejemplo, `GET /api/v1/horas-alumnos` lista a sus alumnos asignados.
