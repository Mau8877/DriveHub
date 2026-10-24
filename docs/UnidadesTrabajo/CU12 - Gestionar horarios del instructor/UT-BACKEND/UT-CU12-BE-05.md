# UT-CU12-BE-05 — Repositorios de horarios y franjas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-05` |
| **Nombre** | Repositorios de horarios y franjas |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-02` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (el repository no tiene lógica), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb-académico-contenido) (**en Spring Data Mongo no hay filtro automático de soft delete**: cada query filtra `deletedAt: null`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers) (patrón `assumeTrue`; modelo: `TestcontainersSmokeTest`).

## Solicitud de negocio

El servicio necesita consultar y modificar los horarios y las franjas del instructor, incluido el cambio de estado de una franja sin que dos peticiones simultáneas se pisen.

## Objetivo

Existen los repositorios con todas las consultas que usan los servicios del CU12, probados contra un MongoDB real.

## Alcance

- `features/horarios/repository/PlantillaHorarioRepository` (`MongoRepository<PlantillaHorario, Long>`), todas las consultas con `deletedAt` nulo:
  - Buscar por `codigoPlantillaHorario` e `instructorCodigo`.
  - Listar por `instructorCodigo` paginado (`Pageable`), con el filtro de `activa` opcional (`Boolean`, `null` = todos). El orden (`diaSemana`, `horaInicio`) lo pone el servicio en el `Pageable`.
  - Listar las activas de un instructor y un día de la semana (para el cruce de horarios).
  - Listar todas las activas, paginado o en lotes (para la generación diaria).
- `features/horarios/repository/FranjaRepository` (`MongoRepository<Franja, Long>`) más una extensión propia (`FranjaRepositoryCustom` + `FranjaRepositoryImpl`, con `MongoTemplate`), con `deletedAt` nulo:
  - Buscar por `codigoFranja` e `instructorCodigo`.
  - Listar por `instructorCodigo` paginado, con filtros opcionales `fecha` exacta, `estado`, y `fechaDesde` (para "desde hoy").
  - `cambiarEstado(codigoFranja, instructorCodigo, estadoEsperado, estadoNuevo)`: **un solo** `findAndModify` atómico que cambia el estado solo si todavía es el esperado; devuelve la franja ya actualizada o vacío si no coincidía. Así dos peticiones simultáneas no pisan el estado.
  - `eliminarFuturasNoReservadas(plantillaId, hoy, horaActual)`: borra **físicamente** las franjas de ese horario en estado `LIBRE` o `BLOQUEADA` cuya fecha es posterior a `hoy`, o es `hoy` con `horaInicio` posterior a `horaActual` (cadenas `HH:mm` con cero a la izquierda, se comparan bien). Devuelve cuántas borró. Dejar el motivo del borrado físico en un comentario (decisión 5 del README).
  - `existe(instructorCodigo, fecha, horaInicio)` o equivalente, y `insertarSiNoExiste(Franja)` que captura `DuplicateKeyException` y devuelve `false` si ya existía (lo que hace idempotente la generación). Cada inserción es independiente, sin transacción.
- Tests de integración con MongoDB real (Testcontainers, `mongo:8`, patrón `assumeTrue`; contexto mínimo de Spring Data Mongo con los callbacks de identificadores — `AsignarIdentificadoresCallback` y `SecuenciaService` — para que se asignen `_id` y códigos): cada consulta con y sin filtros; las filas con `deletedAt` no aparecen; `cambiarEstado` cambia solo si el estado coincide y devuelve vacío si no (y no toca franjas de otro instructor); `eliminarFuturasNoReservadas` borra libres y bloqueadas futuras, **no** borra reservadas ni pasadas ni de otro horario; `insertarSiNoExiste` devuelve `true` la primera vez y `false` la segunda sin lanzar.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/repository/PlantillaHorarioRepository.java`, `FranjaRepository.java`, `FranjaRepositoryCustom.java`, `FranjaRepositoryImpl.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/repository/PlantillaHorarioRepositoryTest.java`, `FranjaRepositoryTest.java`

## Fuera de alcance (no tocar)

- Reglas de negocio, validaciones y errores catalogados (UT 07 a 09).
- Consultas para reservar o para el estudiante (CU11, CU13, CU14).
- Los repositorios de los demás módulos de Académico.

## Resultado esperado

Los servicios del CU12 pueden leer y escribir horarios y franjas con consultas ya probadas, incluido el cambio de estado atómico.

## Validación

1. `./mvnw test` en `academico` pasa (las pruebas con MongoDB real quedan `SKIPPED` si Docker no está disponible).
2. `./mvnw checkstyle:check` sin errores.
