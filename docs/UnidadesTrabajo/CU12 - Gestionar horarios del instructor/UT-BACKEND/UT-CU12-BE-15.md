# UT-CU12-BE-15 — Generación de franjas para varios días

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-15` |
| **Nombre** | Generación de franjas para varios días |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-13` |

## Antes de implementar

[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Leer la [decisión de diseño 6](./README.md#decisiones-de-diseño).

## Solicitud de negocio

Un horario con varios días tiene que producir las franjas de todos sus días, sin duplicar ninguna.

## Objetivo

`GeneradorFranjas` y `FranjaGeneracionService.generar` trabajan con la lista de días del horario.

## Alcance

- `GeneradorFranjas.calcular`: el primer parámetro pasa de `int diaSemana` a `Collection<Integer> diasSemana`; genera una franja por cada fecha cuyo día esté en la lista. El resto (horizonte, vigencia, solo futuras, ventana en minutos) no cambia. Resultado ordenado por fecha y hora.
- `FranjaGeneracionService.generar(PlantillaHorario)`: pasa `horario.getDiasSemana()`. La corrida diaria (`extenderHorariosActivos`) usa `findByEstadoAndDeletedAtIsNull(ACTIVA, …)` (repositorio de la UT 16) y por lo tanto ya no procesa pausados ni inactivos.
- Tests: tres días elegidos generan solo en esas fechas; un solo día se comporta como antes; con lunes y miércoles de 08:00 a 12:00 cada 60 min salen 8 franjas por semana; resultado ordenado; la corrida diaria no toca un horario pausado.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/GeneradorFranjas.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/FranjaGeneracionService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/GeneradorFranjasTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/FranjaGeneracionServiceTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/ExtensionFranjasJobTest.java`

## Fuera de alcance (no tocar)

- Validar días o cruces (UT 17).
- Pausar y reanudar (UT 18).

## Resultado esperado

Un horario con varios días genera sus franjas completas y la tarea diaria solo atiende horarios activos.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos en verde.
2. `./mvnw checkstyle:check` sin errores.
