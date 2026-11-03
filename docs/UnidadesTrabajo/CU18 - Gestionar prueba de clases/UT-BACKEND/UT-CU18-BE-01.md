# UT-CU18-BE-01 — Errores `ERR_ACA_39`, `ERR_ACA_40` y `ERR_ACA_41`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-01` |
| **Nombre** | Errores `ERR_ACA_39`, `ERR_ACA_40` y `ERR_ACA_41` |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) (todo error está catalogado) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (cada código se documenta en el mismo cambio). Tomar como modelo UT-CU17-BE-03.

## Solicitud de negocio

Cada motivo por el que no se puede evaluar una clase o por el que una prueba es inválida tiene que llegar a la app con un código que la distinga de los demás.

## Objetivo

Los tres códigos nuevos existen en `AcademicoErrorCodes` y están documentados.

## Alcance

- `AcademicoErrorCodes` (se edita, `common/exception/`): tres constantes, a continuación de `ERR_ACA_38` y sin reusar ningún número:
  - `ERR_ACA_39` — la clase no se puede evaluar (`409`).
  - `ERR_ACA_40` — la clase ya tiene una prueba de ese tipo (`409`).
  - `ERR_ACA_41` — los datos de la prueba no son válidos (`400`).
- `docs/errors/academico/ERRORES_ACADEMICO.md` (se edita): una sección por código, con el formato obligatorio de la guía (HTTP, excepción, significado, dónde se lanza). «Dónde se lanza»: `PruebaClaseService.evaluar()` para 39 y 40, y `ReglasDePrueba` (llamada desde `PruebaClaseService.evaluar()`) para 41. Se amplía la sección de `ERR_ACA_35`: también cubre una clase dictada ajena o inexistente al evaluar o consultar sus pruebas.
- `docs/errors/README.md` (se edita): tres links nuevos en la sección de Académico, al ancla de cada sección.
- Prueba: si ya existe un test que verifica el catálogo de constantes, se actualiza; si no, no se crea uno solo para esto.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/AcademicoErrorCodes.java`
- `docs/errors/academico/ERRORES_ACADEMICO.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Lanzar las excepciones: lo hacen las UT 04 y 05.
- Los códigos `ERR_ACA_35` a `ERR_ACA_38` ya existentes, salvo ampliar la descripción de `ERR_ACA_35`.
- Cualquier otro servicio.

## Resultado esperado

Los códigos 39, 40 y 41 están declarados, documentados y enlazados desde el índice.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. Cada código del `AcademicoErrorCodes` tiene su sección en `docs/errors/academico/ERRORES_ACADEMICO.md` y su link en `docs/errors/README.md`.
