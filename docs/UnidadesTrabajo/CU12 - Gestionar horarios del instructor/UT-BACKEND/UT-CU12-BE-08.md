# UT-CU12-BE-08 — `PlantillaHorarioService`: desactivar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-BE-08` |
| **Nombre** | `PlantillaHorarioService`: desactivar |
| **Caso de uso** | CU12-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU12-BE-03`, `UT-CU12-BE-05`, `UT-CU12-BE-07` |

## Antes de implementar

[EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Leer las [decisiones de diseño 4 y 5](./README.md#decisiones-de-diseño).

## Solicitud de negocio

Cuando un horario ya no le sirve, el instructor lo da de baja sin perder las clases que ya tienen un estudiante.

## Objetivo

`PlantillaHorarioService.desactivar` deja el horario inactivo y quita sus franjas futuras no reservadas.

## Alcance

- Agregar a `PlantillaHorarioService` el método `desactivar(UUID instructorCodigo, UUID plantillaCodigo)` con `@PreAuthorize("hasRole('INSTRUCTOR')")`, **sin `@Transactional`**:
  1. Busca el horario por código **y** por instructor; si no existe → `NotFoundException` `ERR_ACA_05` (también si es de otro instructor: no se revela).
  2. Si ya está inactivo → `ConflictException` `ERR_ACA_06`.
  3. Elimina las franjas futuras `LIBRE`/`BLOQUEADA` del horario con `eliminarFuturasNoReservadas` (hoy y hora actual del `Clock`).
  4. **Recién después** marca `activa = false` y guarda. El orden importa: si el paso 3 falla, el horario sigue activo y la operación se puede repetir (decisión 4).
  5. Log `INFO` (`module="horarios"`, `instructorCodigo`, `plantillaCodigo`, `franjasEliminadas`).
- Tests unitarios (repositorios mockeados, `Clock` fijo): desactiva y elimina las franjas con los parámetros correctos (hoy y hora actual); no marca inactivo si el borrado lanza; inexistente y de otro instructor → `ERR_ACA_05`; ya inactivo → `ERR_ACA_06` sin tocar franjas; el log informa la cantidad eliminada; no llama a ninguna operación sobre franjas reservadas (el borrado ya las excluye, ver UT 05).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/service/PlantillaHorarioService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/service/PlantillaHorarioServiceTest.java`

## Fuera de alcance (no tocar)

- Reactivar un horario (follow-up).
- Cancelar o avisar a los estudiantes con franjas reservadas: esas franjas se conservan y su cancelación es de CU15.
- El controller (UT 11) y el borrado en sí (consulta de la UT 05).

## Resultado esperado

Desactivar un horario lo deja inactivo, sin franjas futuras libres ni bloqueadas, y respeta las reservadas.

## Validación

1. `./mvnw test` en `academico` pasa con los casos nuevos de `PlantillaHorarioServiceTest` en verde.
2. `./mvnw checkstyle:check` sin errores.
