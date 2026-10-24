# UT-CU19-BE-01 — Modelo por escuela, errores y manejo de 400

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU19-BE-01` |
| **Nombre** | Modelo por escuela, errores y manejo de 400 |
| **Caso de uso** | CU19-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: índices en el Model, soft delete) y [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md).

## Solicitud de negocio

Cada escuela debe tener su propio banco, y un pedido mal formado debe responder un error claro, no un 500.

## Objetivo

La pregunta sabe a qué escuela pertenece, el servicio tiene su catálogo de errores y responde 400 ante pedidos ilegibles.

## Alcance

- `Pregunta`: campo `escuelaCodigo` (UUID) y setter de `creadaPorCodigo`; el índice compuesto pasa a `idx_pregunta_escuela_categoria_tema_activa (escuelaCodigo, categoria, tema, activa)`.
- `AcademicoErrorCodes` (`ERR_ACA_14` a `ERR_ACA_16` y `ERR_SYS_01`).
- `GlobalExceptionHandler`: 400 `ERR_SYS_01` para `HttpMessageNotReadableException` (cuerpo ilegible o enumeración desconocida) y `MethodArgumentTypeMismatchException` (código que no es UUID, filtro con valor inválido).
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: campo, índice y reglas de opciones.

Archivos esperados (crear/tocar): `Pregunta.java`, `AcademicoErrorCodes.java`, `GlobalExceptionHandler.java`, `DISENO_BASE_DE_DATOS.md`.

## Fuera de alcance (no tocar)

- `escuelaCodigo` en `simulacros_config` e `intentos_simulacro` (CU20, CU21).
- Migrar preguntas existentes: la colección estaba vacía.

## Resultado esperado

El servicio arranca y crea el índice nuevo; un cuerpo o parámetro inválido responde 400.

## Validación

1. `./mvnw test`: `AcademicoApplicationTests` carga el contexto contra la base real.
2. `GET /api/v1/preguntas/abc` responde 400 `ERR_SYS_01`.
