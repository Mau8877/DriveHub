# UT-CU15-BE-03 — Errores, plazo configurable y regla de «a tiempo»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-BE-03` |
| **Nombre** | Errores, plazo configurable y regla de «a tiempo» |
| **Caso de uso** | CU15-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) (todo código nuevo se documenta en `docs/errors/` y en el índice, en el mismo cambio), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md).

## Solicitud de negocio

El plazo de 12 horas es una regla del negocio que puede cambiar: tiene que estar en un solo lugar, decidirse con el reloj del servicio y no repetirse en cada pantalla.

## Objetivo

Existen los códigos `ERR_ACA_32` a `ERR_ACA_34` documentados, la propiedad `app.reservas.horas-para-cancelar` y una pieza que dice si una reserva está a tiempo de cancelarse o reprogramarse.

## Alcance

- `AcademicoErrorCodes`: `RESERVA_NO_ENCONTRADA = "ERR_ACA_32"`, `RESERVA_NO_MODIFICABLE = "ERR_ACA_33"`, `RESERVA_FUERA_DE_PLAZO = "ERR_ACA_34"`.
- Documentar los tres en `docs/errors/academico/ERRORES_ACADEMICO.md` con el formato de la guía (HTTP, excepción, significado, dónde se lanza: `ReservaService.cancelar()` / `reprogramar()`) y el link de cada uno en `docs/errors/README.md`. Significados: 32 → 404 `NotFoundException`, la reserva no existe o es de otro estudiante; 33 → 409 `ConflictException`, la reserva no está `CONFIRMADA` o ya empezó; 34 → 409 `ConflictException`, se quiso reprogramar con menos de 12 horas de anticipación.
- `ReservasProperties`: cuarto componente `@DefaultValue("12") int horasParaCancelar`, con su comentario. `application.properties`: `app.reservas.horas-para-cancelar=12` junto a las otras de reservas.
- `PlazoDeClase` (`features/reservas/service/`, clase sin estado, `@Component` con `ReservasProperties` y `Clock`):
  - `boolean haEmpezado(Reserva reserva)`: la fecha y la `horaInicio` de la reserva ya pasaron en la zona del `Clock`.
  - `boolean aTiempo(Reserva reserva)`: faltan `horasParaCancelar` horas o más para `fecha` + `horaInicio` (el límite exacto cuenta como a tiempo). Una clase que ya empezó nunca está a tiempo.
- Tests unitarios de `PlazoDeClase` con un `Clock` fijo: justo en el límite (a tiempo), un minuto menos (tardía), clase ya empezada, clase de otro día, y que cambiar la propiedad cambia el resultado.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/common/exception/AcademicoErrorCodes.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/ReservasProperties.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/PlazoDeClase.java`
- `projects/backend/academico/src/main/resources/application.properties`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/PlazoDeClaseTest.java`
- `docs/errors/academico/ERRORES_ACADEMICO.md`, `docs/errors/README.md`

## Fuera de alcance (no tocar)

- Lanzar los errores o usar el plazo (UT 07 y 08).
- Cualquier cambio en `ReservaService`, el repositorio o los DTOs.

## Resultado esperado

El plazo y los códigos de error están definidos y probados, listos para que los usen cancelar y reprogramar.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Los tres códigos aparecen en `AcademicoErrorCodes`, en `ERRORES_ACADEMICO.md` y en el índice de `docs/errors/README.md`, con sus anclas funcionando.
