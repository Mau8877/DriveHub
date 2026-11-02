# UT-CU17-BE-08 — La agenda de CU16 con `cerrable` y `claseCodigo`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-BE-08` |
| **Nombre** | La agenda de CU16 con `cerrable` y `claseCodigo` |
| **Caso de uso** | CU17-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU17-BE-02`, `UT-CU17-BE-06` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) (un módulo pide datos de otro por su service; esta es la mitad de la dependencia en los dos sentidos de la decisión 4 del [README](./README.md)). Tomar como modelo `AgendaService.agenda` y `ClaseAgendaResponse` (UT-CU16-BE-06). Decisión 9 del README.

## Solicitud de negocio

La app del instructor no repite reglas: necesita que la agenda le diga cuándo ofrecer «Cerrar clase» y cuándo «Editar», y con qué código abrir la clase ya cerrada.

## Objetivo

Cada clase de la agenda dice si se puede cerrar y, si ya está cerrada, cuál es su código de clase.

## Alcance

- `ClaseService` (se edita, sin tocar `cerrar`, `obtener` ni `actualizar`): `Map<Long, UUID> codigosPorReserva(Collection<Long> reservaIds)`, con una sola consulta `findByReservaIdInAndDeletedAtIsNull` (UT 02); lista vacía → mapa vacío sin consultar. Sin `@PreAuthorize`: lo llama la agenda, que ya lo tiene.
- `ClaseAgendaResponse` (CU16): dos campos nuevos al final del `record`:
  - `boolean cerrable`: la reserva está `CONFIRMADA` y **ya empezó** (`PlazoDeClase.haEmpezado`). Es el complemento exacto de `cancelable`: nunca los dos verdaderos.
  - `UUID claseCodigo`: el `codigoClase`, solo si la reserva está `COMPLETADA` o `NO_ASISTIO` **y** tiene clase; en cualquier otro caso `null`.
  - `static de(...)` recibe los dos valores nuevos.
- `AgendaService.agenda` (CU16): después de leer la página, pide **una vez** `codigosPorReserva` con los `id` de las reservas de la página (si la página no tiene clases no llama), y completa `cerrable` y `claseCodigo` de cada una. `cancelar` arma su respuesta con `cerrable = false` y `claseCodigo = null` (la clase quedó cancelada). La lógica de qué clases aparecen, el orden y los nombres **no cambia**.
- Tests: se **actualizan** los de `AgendaServiceTest` y `AgendaControllerTest` (los records ganaron dos campos; el contrato del sobre y los demás casos no cambian). Casos nuevos: `cerrable` verdadero solo para `CONFIRMADA` que ya empezó, falso para la que no empezó, `CANCELADA`, `COMPLETADA` y `NO_ASISTIO`; `cancelable` y `cerrable` nunca verdaderos a la vez; `claseCodigo` presente para `COMPLETADA` y `NO_ASISTIO` con clase, `null` para una `COMPLETADA` sin clase (dato viejo), para una `CONFIRMADA` con clase pendiente de terminar (S1) y para el resto; `codigosPorReserva` se pide una sola vez por página y no se llama sin clases; `ClaseService.codigosPorReserva` con y sin resultados, y con clases borradas fuera; el JSON del controller trae `cerrable` y `claseCodigo`.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/ClaseService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/dto/ClaseAgendaResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/AgendaService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/ClaseServiceTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/AgendaServiceTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/controller/AgendaControllerTest.java`

## Fuera de alcance (no tocar)

- Cerrar, ver y corregir (UT 06 y 07) y el `ClaseController` (UT 09).
- Qué clases aparecen en la agenda, su orden y la cancelación del instructor (CU16): no cambian.
- La app móvil (`UT-MOBILE`).

## Resultado esperado

La agenda trae `cerrable` y `claseCodigo`, y la app puede decidir qué botón ofrecer sin repetir reglas.

## Validación

1. `./mvnw test` en `academico` pasa, incluidos los tests de la agenda de CU16 ya actualizados.
2. `./mvnw checkstyle:check` sin errores.
