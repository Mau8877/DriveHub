# UT-CU12-BE-18 — PlantillaHorarioService: pausar, reanudar y desactivar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-18` |
| **Nombre** | PlantillaHorarioService: pausar, reanudar y desactivar |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-14`, `UT-CU12-BE-15`, `UT-CU12-BE-16`, `UT-CU12-BE-17` |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Las [decisiones de diseño 4 y 5](./README.md#decisiones-de-diseño) siguen vigentes; leer también la 13.

## Solicitud de negocio

El instructor puede parar un horario por un tiempo y retomarlo, sin perder las clases ya reservadas.

## Objetivo

`PlantillaHorarioService` pausa, reanuda y desactiva horarios con el estado nuevo.

## Alcance

- `pausar(UUID instructorCodigo, UUID plantillaCodigo)`: busca el horario del instructor (`ERR_ACA_05` si no existe o es ajeno); si no está `ACTIVA` → `ConflictException` `ERR_ACA_11`; elimina las franjas futuras `LIBRE`/`BLOQUEADA` (`eliminarFuturasNoReservadas`) y **después** pasa a `PAUSADA`. Log `INFO`.
- `reanudar(...)`: `ERR_ACA_05` si no existe o es ajeno; si no está `PAUSADA` → `ERR_ACA_12`; vuelve a comprobar el cruce contra los horarios `ACTIVA` del instructor (`ERR_ACA_04`, y sigue pausado); pasa a `ACTIVA`, guarda y llama a `generacion.generar`. Si la generación falla, el horario ya quedó activo y la corrida diaria completa lo que falte. Un horario con la vigencia vencida se reanuda igual (no genera nada). Log `INFO` con las franjas generadas.
- `desactivar`: ahora acepta un horario `ACTIVA` o `PAUSADA` (`ERR_ACA_06` solo si ya es `INACTIVA`) y lo deja `INACTIVA`; el resto es igual.
- Los tres llevan `@PreAuthorize("hasRole('INSTRUCTOR')")` y **no** `@Transactional`.
- Tests: pausar un activo y no cambiar el estado si falla el borrado; pausar uno pausado o inactivo → `ERR_ACA_11`; reanudar un pausado genera franjas; reanudar uno activo → `ERR_ACA_12`; reanudar con cruce → `ERR_ACA_04` y sigue pausado; desactivar desde pausado; ajeno e inexistente → `ERR_ACA_05`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/PlantillaHorarioService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/PlantillaHorarioServiceTest.java`

## Fuera de alcance (no tocar)

- Reactivar un horario `INACTIVA` (sigue siendo definitivo).
- Los controllers (UT 20).

## Resultado esperado

Un horario se pausa, se reanuda y se desactiva con sus reglas y sus errores.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos en verde.
2. `./mvnw checkstyle:check` sin errores.
