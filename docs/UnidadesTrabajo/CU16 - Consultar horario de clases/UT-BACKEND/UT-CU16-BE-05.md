# UT-CU16-BE-05 — Errores `ERR_ACA_35` y `ERR_ACA_36`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-05` |
| **Nombre** | Errores `ERR_ACA_35` y `ERR_ACA_36` |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (todo código nuevo se documenta en `docs/errors/` en el mismo cambio). Mirar cómo quedaron `ERR_ACA_32` a `ERR_ACA_34` en `AcademicoErrorCodes` (UT-CU15-BE-03).

## Solicitud de negocio

El instructor tiene que recibir mensajes que hablen de **su** clase, no del estudiante, cuando no puede cancelarla.

## Objetivo

Existen los dos códigos de error de la cancelación del instructor, catalogados y documentados.

## Alcance

- `AcademicoErrorCodes`: `CLASE_INSTRUCTOR_NO_ENCONTRADA = "ERR_ACA_35"` y `CLASE_NO_CANCELABLE_POR_INSTRUCTOR = "ERR_ACA_36"`, a continuación de `ERR_ACA_34` (no se reutiliza ni se salta ningún número).
- `docs/errors/academico/ERRORES_ACADEMICO.md`: dos secciones nuevas con el formato de la guía:
  - `ERR_ACA_35` — 404, `NotFoundException`. Significado: la clase que el instructor quiso cancelar no existe, está borrada o es de otro instructor (CU16); nunca se revela si existe a nombre de otra persona. Dónde se lanza: `AgendaService.cancelar()`.
  - `ERR_ACA_36` — 409, `ConflictException`. Significado: la clase no se puede cancelar por el instructor porque no está `CONFIRMADA` (ya cancelada, cerrada o a medias) o ya empezó (CU16); también cuando otra petición la cambió un instante antes. Dónde se lanza: `AgendaService.cancelar()`.
- `docs/errors/README.md`: los dos links en la sección de Académico, apuntando al ancla de cada sección.
- Un test que compruebe que las constantes tienen el valor y el formato `ERR_ACA_NN` (si ya existe un test del catálogo, se extiende).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/AcademicoErrorCodes.java`
- `docs/errors/academico/ERRORES_ACADEMICO.md`, `docs/errors/README.md`
- `projects/backend/academico/src/test/java/com/drivehub/academico/common/exception/` (el test del catálogo, si existe)

## Fuera de alcance (no tocar)

- Lanzar los errores (UT 07).
- Cambiar el texto o la numeración de `ERR_ACA_32` a `ERR_ACA_34`.

## Resultado esperado

Los dos códigos existen, tienen su documentación y su link en el índice.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. Los dos links del índice de `docs/errors/README.md` llevan a su sección.
