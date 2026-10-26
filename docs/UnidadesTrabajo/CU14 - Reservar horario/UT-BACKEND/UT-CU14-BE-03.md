# UT-CU14-BE-03 — Repositorio de franjas: disponibles, apartar y liberar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-BE-03` |
| **Nombre** | Repositorio de franjas: disponibles, apartar y liberar |
| **Caso de uso** | CU14-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb-académico-contenido) (soft delete explícito), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md#tests-de-integración-con-bd-real-testcontainers). Leer `FranjaRepositoryCustom` y `FranjaRepositoryImpl` de CU12 (`cambiarEstado` es el modelo del apartado atómico).

## Solicitud de negocio

El estudiante necesita ver las franjas libres de su escuela con filtros, y reservar una sin que dos estudiantes se queden con la misma.

## Objetivo

`FranjaRepository` sabe listar las franjas disponibles de una escuela con filtros, listar los instructores que tienen alguna, apartar una franja de forma atómica y liberarla.

## Alcance

En `features/horarios/repository/` (extender `FranjaRepositoryCustom` y `FranjaRepositoryImpl`), todas con `deletedAt` nulo:

- `disponibles(escuelaCodigo, hoy, ahora, fechaDesde, fechaHasta, instructorCodigo, horaDesde, horaHasta, Pageable)`: franjas `LIBRE` de esa escuela que **no empezaron** (fecha posterior a `hoy`, o `hoy` con `horaInicio` posterior a `ahora`), con los filtros opcionales (cada uno `null` = sin filtro): rango de fechas, instructor, y `horaInicio` entre `horaDesde` (inclusive) y `horaHasta` (exclusive). Sin `fechaDesde`, desde `hoy`. Orden `fecha`, `horaInicio`, y `instructorCodigo` para desempatar.
- `instructoresConDisponibles(escuelaCodigo, hoy, ahora, fechaDesde, fechaHasta)`: los `instructorCodigo` distintos que tienen al menos una franja disponible con esos criterios.
- `apartar(codigoFranja, escuelaCodigo, reservaId, hoy, ahora)`: **un solo** `findAndModify` atómico que pasa la franja de `LIBRE` a `RESERVADA` y guarda `reservaId`, solo si es de esa escuela y todavía no empezó; devuelve la franja ya actualizada o vacío. Quien llega segundo recibe vacío.
- `liberar(codigoFranja, reservaId)`: `findAndModify` que vuelve la franja a `LIBRE` y limpia `reservaId`, solo si sigue `RESERVADA` con ese `reservaId` (no libera la reserva de otro). Devuelve si cambió algo.
- `buscarPorCodigoYEscuela(codigoFranja, escuelaCodigo)`: para distinguir «no existe» de «no está libre».
- Tests de integración con MongoDB real (patrón `assumeTrue`): `disponibles` respeta escuela, estado, que no empiece, cada filtro y su combinación; `horaHasta` es exclusiva; `apartar` cambia solo una franja `LIBRE` de esa escuela y **dos llamadas simultáneas (hilos) a la misma franja dejan exactamente una ganadora**; `apartar` rechaza una franja de otra escuela, bloqueada, reservada o ya empezada; `liberar` no libera si el `reservaId` no coincide.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/horarios/repository/FranjaRepositoryCustom.java`, `FranjaRepositoryImpl.java`, `FranjaRepository.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/horarios/repository/FranjaRepositoryTest.java`

## Fuera de alcance (no tocar)

- Las consultas y operaciones del instructor de CU12 (`cambiarEstado`, `eliminarFuturasNoReservadas`, etc.): no se cambian.
- Reglas de negocio, errores catalogados y nombres de instructores (UT 05 y 06).
- Cualquier cambio en el modelo `Franja` o sus índices.

## Resultado esperado

Los servicios del CU14 pueden listar las franjas ofrecidas y apartar una de forma que dos estudiantes nunca compartan la misma.

## Validación

1. `./mvnw test` en `academico` pasa (las pruebas con MongoDB real quedan `SKIPPED` si Docker no está disponible).
2. `./mvnw checkstyle:check` sin errores.
