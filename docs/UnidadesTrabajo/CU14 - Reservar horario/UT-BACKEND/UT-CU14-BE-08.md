# UT-CU14-BE-08 — Mis clases e historial

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-08` |
| **Nombre** | Mis clases e historial |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU14-BE-01`, `UT-CU14-BE-02`, `UT-CU14-BE-04` |

## Antes de implementar

[ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md) (paginación y `filter[...]`), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). La definición de «próximas» e «historial» está en la decisión 11 del [README](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El estudiante quiere ver sus clases por venir, de la más próxima a la más lejana, y el historial de las que ya pasaron o se cancelaron, con su estado.

## Objetivo

Existe `GET /api/v1/reservas`, que lista las clases del estudiante como próximas o como historial.

## Alcance

- `ReservaService.listar(estudianteCodigo, bearer, vista, page, pageSize)` con `@PreAuthorize("hasRole('ESTUDIANTE')")`:
  - `vista`: `proximas` (por defecto) o `historial`; otro valor → `ValidationException` `ERR_SYS_01`. `pageSize` 20 por defecto y 100 como máximo.
  - Consulta `ReservaRepository.proximas` o `historial` con la fecha y hora actuales del `Clock` en `app.horarios.zona-horaria`; nunca devuelve reservas de otro estudiante.
  - `terminada` = la fecha y `horaFin` de la reserva ya pasaron. Una `CONFIRMADA` con `terminada = true` solo aparece en el historial.
  - Une los nombres de los instructores de la página con `IdentidadClient`; un nombre faltante se muestra «Instructor»; Identidad caída → `ServicioNoDisponibleException` `ERR_ACA_17` (el listado no se muestra a medias).
- `ReservaController`: `GET` con `page`, `pageSize` y `filter[vista]`, que responde `200` con `data` (lista de `ReservaResponse`) y `meta`.
- Tests: service (próximas solo confirmadas que no terminaron y en orden ascendente; historial con completadas, no asistió, canceladas y confirmadas terminadas, descendente; una `PENDIENTE_PAGO` no aparece en ninguna; una reserva de otro estudiante no aparece; la que termina justo ahora pasa de una vista a la otra; vista inválida → `ERR_SYS_01`; Identidad caída → `ERR_ACA_17`) y controller con `@WebMvcTest` (sobre y `meta`; otro rol → `403`).

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ReservaService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/controller/ReservaController.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/` (service y controller)

## Fuera de alcance (no tocar)

- Retomar reservas interrumpidas al consultar (UT 09): esta UT solo lista.
- Cancelar o reprogramar (CU15), cerrar la clase (CU17), el calendario del instructor (CU16).
- Mostrar al estudiante datos del instructor distintos de su nombre.

## Resultado esperado

El estudiante ve sus clases próximas y su historial, ordenados y paginados, con el nombre del instructor.

## Validación

1. `./mvnw test` en `academico` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Con el stack arriba y una reserva confirmada: `GET /api/v1/reservas` con el token del estudiante → la reserva en `data`; `GET /api/v1/reservas?filter%5Bvista%5D=historial` → lista vacía hasta que la clase pase.
