# UT-CU12-BE-14 — Errores ERR_ACA_11 a ERR_ACA_13 y su documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-14` |
| **Nombre** | Errores ERR_ACA_11 a ERR_ACA_13 y su documentación |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (formato obligatorio de `docs/errors/`).

## Solicitud de negocio

Pausar, reanudar y consultar por rango pueden fallar de formas nuevas y cada una necesita su código documentado.

## Objetivo

Los tres códigos nuevos existen en `AcademicoErrorCodes` y están en `docs/errors/`.

## Alcance

- `AcademicoErrorCodes`: `HORARIO_NO_ESTA_ACTIVO = "ERR_ACA_11"`, `HORARIO_NO_ESTA_PAUSADO = "ERR_ACA_12"`, `RANGO_FECHAS_INVALIDO = "ERR_ACA_13"`.
- `docs/errors/academico/ERRORES_ACADEMICO.md`: una sección por código con el formato de la guía. `ERR_ACA_11`: 409, `ConflictException`, pausar un horario que no está activo, `PlantillaHorarioService.pausar()`. `ERR_ACA_12`: 409, `ConflictException`, reanudar uno que no está pausado, `PlantillaHorarioService.reanudar()`. `ERR_ACA_13`: 400, `ValidationException`, rango de fechas con la inicial posterior a la final, de más de 7 días o con solo una de las dos fechas, `FranjaService.listar()`.
- `docs/errors/README.md`: un link por código nuevo.
- Actualizar la sección de `ERR_ACA_06` (aplica a un horario ya inactivo, sea desde activo o pausado) y la de `ERR_ACA_10` (también cuando se bloquea o desbloquea un día que ya pasó).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/AcademicoErrorCodes.java`
- `docs/errors/academico/ERRORES_ACADEMICO.md`
- `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Lanzar las excepciones (UT 18 y 19).

## Resultado esperado

Los códigos 11 a 13 están en el catálogo y documentados, con su índice al día.

## Validación

1. Cada código de `AcademicoErrorCodes` tiene su sección en `ERRORES_ACADEMICO.md` y su link en el índice.
2. `./mvnw checkstyle:check` sin errores.
