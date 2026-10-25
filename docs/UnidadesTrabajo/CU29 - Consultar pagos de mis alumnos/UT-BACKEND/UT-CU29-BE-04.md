# UT-CU29-BE-04 — Consulta de horas de los alumnos de un instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-BE-04` |
| **Nombre** | Consulta de horas de los alumnos de un instructor |
| **Caso de uso** | CU29-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU29-BE-03 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El instructor ve, por cada alumno, cuánto compró, cuánto gastó con él y cuánto le queda, y el detalle clase a clase.

## Objetivo

`HorasAlumnosService` arma el listado y el detalle de los alumnos de un instructor.

## Alcance

- `MovimientoHorasRepository` suma consultas agregadas con filtro de escuela: horas compradas por estudiante (suma de `pago.horas` de los pagos `CONFIRMADO` de cada uno; los reembolsados no cuentan), horas gastadas con un instructor por estudiante (suma de `CONSUMO` menos suma de `DEVOLUCION` de ese `instructor_codigo`), y los consumos netos por clase de un estudiante con un instructor (`clase_codigo`, horas netas > 0, fecha del consumo). Todas por lotes (una consulta por conjunto de estudiantes, no una por alumno).
- `BilleteraRepository` suma la lectura de `horas_disponibles` por conjunto de estudiantes.
- DTOs (`record`): `HoraAlumnoResponse(estudianteCodigo, nombres, apellidos, horasCompradas, horasGastadasConmigo, horasRestantes)`; `HorasAlumnoDetalleResponse(resumen: HoraAlumnoResponse, consumos: List<ConsumoClaseResponse(claseCodigo, fecha, horas)>, pagos: List<PagoAlumnoResponse(codigo, planNombre, horas, estado, creadoEn)>)`. Los `pagos` son los de concepto `HORAS` del alumno (más recientes primero, hasta 20) con el estado visible de CU28; el comprobante lo suma CU30.
- `HorasAlumnosService` (clase concreta, `@RequiredArgsConstructor`, `@PreAuthorize("hasRole('INSTRUCTOR')")`, `@Transactional(readOnly = true)`):
  - `listar(escuelaCodigo, instructorCodigo, search, page, pageSize)`: obtiene los alumnos asignados (`EscuelaClient`), sus nombres (`IdentidadClient`), filtra por `search` (contiene, sin distinguir mayúsculas, sobre `nombres apellidos`), ordena por apellidos y nombres, **pagina en memoria** (`page` 1, `pageSize` 20, máximo 100) y calcula los tres totales solo de los alumnos de la página. Un alumno sin billetera ni compras aparece con ceros.
  - `detalle(escuelaCodigo, instructorCodigo, estudianteCodigo)`: si `esMiAlumno` es falso, `NotFoundException` `ERR_PAG_12` (no se distingue "no existe" de "no es tuyo"); si no, arma el detalle.
- `ERR_PAG_12` (alumno no asignado o inexistente, 404) al catálogo y a `docs/errors/`.
- Las consultas leen **solo** lo del instructor del token: nunca las horas gastadas con otros instructores.
- Tests de service con repositorios y clientes mockeados: listado ordenado, búsqueda, paginación en memoria, alumno sin movimientos con ceros, las horas gastadas con otro instructor no se suman, una devolución resta, un pago reembolsado no cuenta como comprado; detalle de alumno ajeno → `ERR_PAG_12`; clientes caídos → `ERR_PAG_15`. Repositorio con Testcontainers (`HorasAlumnosRepositoryTest`): las agregaciones con datos de dos instructores y dos escuelas.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/repository/MovimientoHorasRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/repository/BilleteraRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/service/HorasAlumnosService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/HoraAlumnoResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/HorasAlumnoDetalleResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/ConsumoClaseResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/PagoAlumnoResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/HorasAlumnosServiceTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/HorasAlumnosRepositoryTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- El controller (UT 05).
- El comprobante en el detalle (CU30).
- Guardar nombres o la lista de alumnos en Pagos.

## Resultado esperado

El service devuelve a cada alumno asignado con sus horas compradas, gastadas con este instructor y restantes, y el detalle clase a clase, sin mezclar datos de otros instructores.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con consumos de dos instructores sobre el mismo alumno, cada uno ve solo sus horas gastadas.
