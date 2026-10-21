# UT-CU12-BE-20 — Controllers, tarea diaria y prueba de integración

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-20` |
| **Nombre** | Controllers, tarea diaria y prueba de integración |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-17`, `UT-CU12-BE-18`, `UT-CU12-BE-19` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). El gateway ya enruta `plantillas-horario` y `franjas`.

## Solicitud de negocio

Todo lo anterior tiene que estar disponible por HTTP y probado de punta a punta.

## Objetivo

Los endpoints del contrato nuevo responden bien y el CU12 está cubierto por una prueba de integración.

## Alcance

- `PlantillaHorarioController`: `GET` con `filter[estado]` repetible (`ACTIVA`, `PAUSADA`, `INACTIVA`; sin él, activos y pausados) en lugar de `filter[activa]`; `POST /{codigo}/pausar` y `POST /{codigo}/reanudar` (`200`, `data: null`, mensajes «Horario pausado correctamente» y «Horario reanudado correctamente»).
- `FranjaController`: `GET` con `filter[fecha]` (exacto), o `filter[fechaDesde]` y `filter[fechaHasta]` (rango); `POST /dias/{fecha}/bloquear` y `POST /dias/{fecha}/desbloquear` (`200`, `data`: `DiaBloqueadoResponse`). El controller no tiene lógica ni logging.
- `HorariosIntegracionTest`: actualizar a la forma nueva (varios días, nombre, estado) y agregar pausar → reanudar, bloquear día y la migración del documento viejo.
- Tests de controller (`@WebMvcTest`, forma del sobre y códigos HTTP) de los endpoints nuevos y de los que cambiaron.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/controller/PlantillaHorarioController.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/controller/FranjaController.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/controller/PlantillaHorarioControllerTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/controller/FranjaControllerTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/HorariosIntegracionTest.java`

## Fuera de alcance (no tocar)

- Cambios en el gateway: las rutas ya existen.
- Lógica o logging en los controllers.

## Resultado esperado

El contrato nuevo del CU12 funciona de punta a punta y está probado.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos en verde.
2. `./mvnw checkstyle:check` sin errores.
