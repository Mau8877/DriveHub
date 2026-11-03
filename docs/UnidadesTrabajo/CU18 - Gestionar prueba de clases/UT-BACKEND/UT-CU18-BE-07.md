# UT-CU18-BE-07 — La agenda de CU16 con `evaluable` y `conPrueba`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-07` |
| **Nombre** | La agenda de CU16 con `evaluable` y `conPrueba` |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU18-BE-03`, `UT-CU18-BE-05` |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md#regla-de-dependencia) (un módulo habla con otro por el service del dueño, nunca por su repository). Tomar como modelo UT-CU17-BE-08 (`cerrable` y `claseCodigo`, y `ClaseService.codigosPorReserva`). Decisión 10 del [README](./README.md).

## Solicitud de negocio

La app no debe adivinar cuándo ofrecer «Evaluar» o «Ver prueba»: el servidor se lo dice en cada clase de la agenda.

## Objetivo

Cada clase de la agenda trae `evaluable` y `conPrueba`, calculados por el servidor.

## Alcance

- `ClaseAgendaResponse` (se edita): dos campos al final, `boolean evaluable` y `boolean conPrueba`, documentados en el Javadoc. El factory existente de `ClaseAgendaResponse` sigue funcionando y los pone en `false`; cambio compatible hacia atrás.
- `PruebaClaseService` (se edita): `Map<Long, Set<TipoPrueba>> tiposEvaluadosPorReserva(Map<Long, ...>)`, o la firma equivalente que, dadas las reservas `COMPLETADA` de la página junto con el `_id` de su clase, devuelva qué tipos ya tienen prueba. Una sola consulta (`findByClaseIdInAndDeletedAtIsNull`), sin una por clase. Sin `@PreAuthorize` (la agenda ya exige el rol).
- `AgendaService` (se edita): al armar la página, para las reservas `COMPLETADA` que tienen clase, pide los tipos ya evaluados y calcula:
  - `conPrueba` = tiene al menos una prueba;
  - `evaluable` = la reserva está `COMPLETADA`, tiene clase y le falta algún tipo (menos de tres pruebas).
  - Las demás reservas (confirmadas, canceladas, `NO_ASISTIO`, con cierre a medias) traen `false` en los dos. No se consulta por ellas.
  - `cancelable`, `cerrable` y `evaluable` nunca son verdaderos a la vez.
- Tests: una `COMPLETADA` sin pruebas → `evaluable = true`, `conPrueba = false`; con una prueba → los dos verdaderos; con las tres → `evaluable = false`, `conPrueba = true`; `NO_ASISTIO`, `CONFIRMADA` y `CANCELADA` → los dos `false`; una `COMPLETADA` sin clase (dato viejo) → los dos `false`; una sola consulta de pruebas por página aunque haya varias clases (verificado con el repositorio simulado); la agenda de CU16 y CU17 sigue en verde.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/dto/ClaseAgendaResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/AgendaService.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/PruebaClaseService.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/AgendaServiceTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/PruebaClaseServiceTest.java`

## Fuera de alcance (no tocar)

- Mostrar el puntaje o el resultado en la agenda: se ven en «Ver prueba».
- Cambiar `cancelable`, `cerrable`, `claseCodigo` o el estado «Pendiente de cierre».
- La app móvil.

## Resultado esperado

`GET /api/v1/reservas/agenda` trae, en cada clase, si se puede evaluar y si ya tiene alguna prueba.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. Con el servicio real (UT 09): una clase cerrada con `PRESENTE` sale `evaluable: true`; tras evaluar los tres tipos, `evaluable: false` y `conPrueba: true`.
