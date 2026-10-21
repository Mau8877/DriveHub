# UT-CU12-BE-10 — Generación diaria de franjas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-10` |
| **Nombre** | Generación diaria de franjas |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-05`, `UT-CU12-BE-07` |

## Antes de implementar

[LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Leer la [decisión de diseño 7](./README.md#decisiones-de-diseño). Reutiliza `FranjaGeneracionService` que crea la UT 07.

## Solicitud de negocio

Sin esto, las franjas se acabarían a las cuatro semanas: el instructor tiene que poder olvidarse de su horario y seguir apareciendo disponible.

## Objetivo

Una tarea programada completa cada día las franjas que faltan de todos los horarios activos, sin duplicar nada.

## Alcance

- `features/horarios/service/FranjaGeneracionService` (ya existe, UT 07): agregar `ResumenExtension extenderHorariosActivos()`:
  - Recorre los horarios activos de **todos** los instructores en lotes (paginado, ver `PlantillaHorarioRepository`, UT 05).
  - Salta el horario cuya `vigenteHasta` es anterior a hoy (con el `Clock`).
  - Para cada uno llama al mismo `generar(PlantillaHorario)` de la UT 07: las franjas que ya existen no se duplican (`insertarSiNoExiste`).
  - Si un horario falla, loguea `ERROR` con su `plantillaCodigo` y **sigue** con los demás.
  - Al terminar, un solo log `INFO` por corrida (`module="horarios"`, `horariosProcesados`, `franjasCreadas`, `horariosConError`).
  - No tiene `@PreAuthorize`: lo ejecuta el sistema, no una persona.
  - `record ResumenExtension(int horariosProcesados, int franjasCreadas, int horariosConError)`.
- `features/horarios/service/ExtensionFranjasJob` (`@Component`, solo si `app.horarios.job-habilitado` es `true`, que es el default): un método con `@Scheduled(cron = "${app.horarios.extension-cron}", zone = "${app.horarios.zona-horaria}")` que llama a `extenderHorariosActivos()`.
- `config/SchedulingConfig` con `@EnableScheduling`.
- Tests unitarios (repositorios y `GeneradorFranjas` mockeados o reales según convenga, `Clock` fijo): completa las franjas que faltan; correr dos veces seguidas crea cero la segunda vez; salta el horario de vigencia vencida; un horario que falla no detiene a los otros y se cuenta en `horariosConError`; sin horarios activos devuelve todo en cero; el job llama al servicio y no corre con `job-habilitado=false`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/FranjaGeneracionService.java` (tocar), `ExtensionFranjasJob.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/config/SchedulingConfig.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/FranjaGeneracionServiceTest.java`, `ExtensionFranjasJobTest.java`

## Fuera de alcance (no tocar)

- Un candado para varias réplicas (ShedLock): follow-up (decisión 7).
- Borrar franjas pasadas o de horarios vencidos: no se limpia nada, solo se agrega.
- Cambiar la lógica de cálculo (`GeneradorFranjas`, UT 04).
- Un endpoint para disparar la generación a mano.

## Resultado esperado

Cada madrugada, todos los horarios activos quedan con sus franjas completas hasta 28 días adelante, y correrlo de más no cambia nada.

## Validación

1. `./mvnw test` en `academico` pasa con `FranjaGeneracionServiceTest` y `ExtensionFranjasJobTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
