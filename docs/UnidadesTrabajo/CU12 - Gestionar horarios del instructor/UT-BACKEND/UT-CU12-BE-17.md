# UT-CU12-BE-17 — DTOs y PlantillaHorarioService: crear y listar con varios días

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-17` |
| **Nombre** | DTOs y PlantillaHorarioService: crear y listar con varios días |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-13`, `UT-CU12-BE-14`, `UT-CU12-BE-15`, `UT-CU12-BE-16` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Leer las [decisiones de diseño 11 y 12](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El instructor crea un horario con nombre y varios días y lo ve en su lista junto con el estado.

## Objetivo

Crear y listar horarios funcionan con la forma nueva y con el cruce calculado sobre días compartidos.

## Alcance

- `CrearPlantillaHorarioRequest`: quitar `diaSemana`; agregar `@NotBlank @Size(max = 60) String nombre` y `@NotEmpty @Size(max = 7) List<@NotNull @Min(1) @Max(7) Integer> diasSemana`. El resto no cambia.
- `PlantillaHorarioResponse` y `PlantillaHorarioCreadaResponse`: quitar `diaSemana` y `activa`; agregar `nombre`, `diasSemana` (`List<Integer>`) y `estado` (`EstadoPlantilla`).
- `PlantillaHorarioService.crear`: nombre con `trim`; días sin repetir y ordenados; la comprobación de cruce compara contra todos los horarios `ACTIVA` del instructor y falla con `ERR_ACA_04` solo si **comparten algún día**, se solapan las horas y se solapan las vigencias.
- `PlantillaHorarioService.listar(instructorCodigo, Set<EstadoPlantilla> estados, page, pageSize)`: sin estados pide `ACTIVA` y `PAUSADA`. Devuelve los horarios ordenados por estado (activos, pausados, inactivos), luego por primer día de la semana y hora de inicio, y pagina en memoria (un instructor tiene pocos horarios): es la única forma de ordenar por estado como pide el CU.
- Tests: request válido e inválido (nombre vacío o de 61 caracteres, sin días, día 8); días repetidos; cruce con un día en común frente a mismas horas sin día en común; los pausados no cuentan para el cruce; orden y paginación del listado; el log de alta.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/dto/CrearPlantillaHorarioRequest.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/dto/PlantillaHorarioResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/dto/PlantillaHorarioCreadaResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/PlantillaHorarioService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/dto/CrearPlantillaHorarioRequestTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/PlantillaHorarioServiceTest.java`

## Fuera de alcance (no tocar)

- Pausar, reanudar y desactivar (UT 18).
- El controller y el filtro por estado de la URL (UT 20).

## Resultado esperado

Se crea un horario con varios días y se lista con su estado, y dos horarios solo chocan si comparten día.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos en verde.
2. `./mvnw checkstyle:check` sin errores.
