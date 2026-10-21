# UT-CU12-BE-13 — Modelo del horario con nombre, varios días y estado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-13` |
| **Nombre** | Modelo del horario con nombre, varios días y estado |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB: soft delete explícito en cada query), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md). Leer el cambio en [`CU12`](../../../CasosDeUso/CU12%20-%20Gestionar%20horarios%20del%20instructor.md) (reglas «Horario» y «Estados de un horario») y la [decisión de diseño 11](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El instructor da un nombre a cada horario, lo arma con varios días y puede pausarlo, así que el horario necesita guardar esos datos.

## Objetivo

`PlantillaHorario` guarda `nombre`, `diasSemana` y `estado`, y los horarios que ya existían se convierten a la forma nueva al arrancar.

## Alcance

- `enums/EstadoPlantilla`: `ACTIVA`, `PAUSADA`, `INACTIVA`.
- `PlantillaHorario`: quitar `diaSemana` y `activa`; agregar `nombre` (`String`), `diasSemana` (`List<Integer>`, 1 a 7, sin repetir y ordenada) y `estado` (`EstadoPlantilla`, persistido como texto). El constructor recibe `nombre` y `diasSemana` y deja el horario `ACTIVA`.
- `service/HorariosMigracion` (`ApplicationRunner`, idempotente): a cada documento de `plantillas_horario` que todavía tiene `diaSemana` le pone `diasSemana = [diaSemana]`, `nombre` armado como «{Día} {horaInicio} – {horaFin}» (ej. «Lunes 08:00 – 12:00») y `estado` = `ACTIVA` o `INACTIVA` según su `activa`; después quita `diaSemana` y `activa`. Un documento ya convertido no se toca. Se usa `MongoTemplate`, sin pasar por el modelo viejo.
- Tests: constructor (estado inicial `ACTIVA`, días ordenados y sin repetir); la migración convierte un documento viejo activo, uno viejo inactivo, y no cambia uno ya convertido (con Testcontainers, patrón de `MongoIntegrationTestBase`).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/enums/EstadoPlantilla.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/model/PlantillaHorario.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/HorariosMigracion.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/model/PlantillaHorarioTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/HorariosMigracionTest.java`

## Fuera de alcance (no tocar)

- Los repositorios, servicios, DTOs y controllers que usan el modelo (UT 16 a 20).
- Cambios en `Franja`: no lleva el día ni el estado del horario.

## Resultado esperado

El modelo de horario tiene nombre, lista de días y estado, y una base que ya tenía horarios queda convertida sin perder datos.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos en verde.
2. `./mvnw checkstyle:check` sin errores.
