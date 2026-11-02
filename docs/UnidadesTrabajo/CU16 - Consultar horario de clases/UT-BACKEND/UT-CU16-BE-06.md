# UT-CU16-BE-06 — `AgendaService`: ver la agenda

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-BE-06` |
| **Nombre** | `AgendaService`: ver la agenda |
| **Caso de uso** | CU16-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU16-BE-02`, `UT-CU16-BE-05` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#flujo-de-una-request) (el service es la única capa que loguea y decide el rol), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#autorización-en-cada-servicio) (`@PreAuthorize` en el service), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md). Tomar como modelo `ReservaService.listar` y el mapeo a `ReservaResponse`. Decisiones 3 a 6 del [README](./README.md).

## Solicitud de negocio

El instructor quiere abrir su agenda y ver qué clases tiene un día, con quién y en qué estado.

## Objetivo

`AgendaService.agenda` devuelve la agenda de un día del instructor, con el nombre de cada estudiante, el estado visible y si la clase se puede cancelar.

## Alcance

- DTO (`record`) en `features/reservas/dto/`: `ClaseAgendaResponse(UUID codigo, LocalDate fecha, String horaInicio, String horaFin, UUID estudianteCodigo, String estudianteNombre, EstadoReserva estado, boolean terminada, boolean cancelable)` con `static de(Reserva, String estudianteNombre, boolean terminada, boolean cancelable)`. El `estado` se muestra sin transitorios (`CANCELACION_PENDIENTE` → `CANCELADA`, `REPROGRAMACION_PENDIENTE` → `CONFIRMADA`), la misma regla de `ReservaResponse`. Sin ids internos.
- `AgendaService` (nueva clase, `features/reservas/service/`, `@Service`, `@RequiredArgsConstructor`) con:
  - `@PreAuthorize("hasRole('INSTRUCTOR')") Page<ClaseAgendaResponse> agenda(UUID instructorCodigo, UUID escuelaCodigo, String bearer, String fecha, int page, int pageSize)`:
    1. `escuelaCodigo` nulo → `ForbiddenException` `ERR_ACA_16`. `fecha` nula → hoy según el `Clock`; si no es `yyyy-MM-dd` → `ValidationException` `ERR_SYS_01`. `page` menor que 1 y `pageSize` fuera de 1 a 100 se acotan (como `listar`).
    2. Pide `agendaDeInstructor` (UT 02).
    3. Si hay clases, pide los nombres con `IdentidadClient.nombresDeEstudiantes` (una sola vez, con el Bearer del instructor); si falla, el `ServicioNoDisponibleException` `ERR_ACA_17` sale tal cual. Sin clases no se llama a Identidad.
    4. Mapea cada reserva: nombre del mapa o `Estudiante` si falta (constante `NOMBRE_SIN_DATOS`); `terminada` con la misma regla que `ReservaService.terminada` (extraerla a un lugar común si hace falta, sin duplicarla); `cancelable` = `CONFIRMADA` y `!plazo.haEmpezado`.
  - No loguea lecturas (regla de logging: un `GET` no se registra).
- Tests de servicio (repositorio, `IdentidadClient` y `Clock` simulados): lista ordenada con nombres; estado visible de las transitorias; `cancelable` verdadero solo para `CONFIRMADA` que no empezó y falso para empezada, terminada, cancelada y completada; `terminada` en el límite de la hora de fin; estudiante sin nombre → `Estudiante`; Identidad caída → `ERR_ACA_17`; sin clases → no se llama a Identidad; fecha omitida → hoy; fecha inválida, `page` o `pageSize` inválidos → `ERR_SYS_01`; la anotación `@PreAuthorize("hasRole('INSTRUCTOR')")` está en `agenda` (comprobada por reflexión, como en `ReservaServiceTest`).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/dto/ClaseAgendaResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/AgendaService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/AgendaServiceTest.java`

## Fuera de alcance (no tocar)

- Cancelar (UT 07), el controller y la ruta (UT 08).
- `ReservaService` y la vista del estudiante: no cambian (solo se puede extraer `terminada` si hace falta, sin cambiar su comportamiento).
- Cualquier cálculo de plazo de 12 horas: el instructor no lo tiene.

## Resultado esperado

El servicio entrega la agenda de un día del instructor lista para mostrar.

## Validación

1. `./mvnw test` en `academico` pasa, incluidos los tests de `ReservaService` sin cambios.
2. `./mvnw checkstyle:check` sin errores.
