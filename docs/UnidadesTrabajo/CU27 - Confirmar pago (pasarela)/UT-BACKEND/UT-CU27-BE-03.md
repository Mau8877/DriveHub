# UT-CU27-BE-03 — Consumir y devolver horas (las compras más antiguas primero)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU27-BE-03` |
| **Nombre** | Consumir y devolver horas (las compras más antiguas primero) |
| **Caso de uso** | CU27-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU27-BE-02 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Académico necesita gastar y devolver horas sin saber de compras ni de dinero, y Pagos necesita saber de cuál compra salió cada hora para decidir después qué se puede reembolsar.

## Objetivo

`BilleteraService.consumir` y `devolver` descuentan y restituyen horas de forma idempotente, tomando siempre de las compras más antiguas.

## Alcance

Es el contrato mínimo que Académico usa ([CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md)); se define aquí porque sin consumo no se pueden probar de punta a punta CU28, CU29 y CU31. Los casos de uso de Académico que lo llaman son de otro desarrollador.

- `ConsumoResultado(horasDescontadas, horasDisponibles)` y `DevolucionResultado(horasDevueltas, horasDisponibles)` (`record`).
- `BilleteraService.consumir(escuelaCodigo, estudianteCodigo, horas, instructorCodigo, claseCodigo, idempotencyKey)` con `@PreAuthorize("hasAnyRole('INSTRUCTOR','ESTUDIANTE','ADMIN_ESCUELA')")`:
  1. `horas` entero de 1 a 500, la clave de 1 a 64 caracteres y `instructorCodigo` y `claseCodigo` obligatorios; si no, `ValidationException` `ERR_SYS_01`.
  2. Si ya hay movimientos con esa clave para esa billetera, devuelve el mismo resultado sin descontar de nuevo (los suma).
  3. Lee la billetera con su versión; si `horasDisponibles < horas`, `ConflictException` `ERR_PAG_09` y no descuenta nada.
  4. Toma los pagos `CONFIRMADO` del estudiante con `horas_restantes > 0` ordenados por `confirmado_en, id`, **con bloqueo de escritura** (`@Lock(PESSIMISTIC_WRITE)`), y reparte las horas: de cada compra toma lo que pueda hasta completar. Por cada compra tocada guarda un movimiento `CONSUMO` (con el pago, `instructorCodigo`, `claseCodigo` y la misma clave) y baja su `horas_restantes`.
  5. Baja `horasDisponibles` y guarda. Un conflicto de versión se reintenta hasta 3 veces.
- `BilleteraService.devolver(escuelaCodigo, estudianteCodigo, claseCodigo, idempotencyKey)` con el mismo `@PreAuthorize`: busca los movimientos `CONSUMO` de esa clase de esa billetera que no tengan ya una `DEVOLUCION` de esa clase; si no hay ninguno, `NotFoundException` `ERR_PAG_16`; por cada uno guarda una `DEVOLUCION` con el mismo pago, el mismo `instructor_codigo` y el mismo `clase_codigo` (así CU29 resta lo devuelto de las horas gastadas con cada instructor) y devuelve las horas a esa compra (`horas_restantes`) y al saldo. Con la misma clave devuelve el mismo resultado.
- Se agregan al catálogo y a `docs/errors/` `ERR_PAG_09` (horas insuficientes, 409) y `ERR_PAG_16` (no hay consumo para devolver, 404).
- Logs: `INFO` en cada consumo y devolución (`module = billetera`, `claseCodigo`, `horas`), sin datos del estudiante.
- Tests de service con repositorios mockeados: consume de una sola compra; consume cruzando dos compras (genera dos movimientos y baja ambas `horas_restantes`); saldo insuficiente → `ERR_PAG_09` sin cambios; misma clave → mismo resultado sin descontar; `horas = 0` o `501` → `ERR_SYS_01`; devolver restituye a las mismas compras; devolver dos veces la misma clase → la segunda no devuelve de más; clase sin consumo → `ERR_PAG_16`. Tests de integración con Testcontainers (`BilleteraConsumoIntegracionTest`): dos consumos simultáneos que juntos superan el saldo dejan uno aceptado y uno rechazado y el saldo nunca queda negativo; el orden de las compras se respeta; `sum(horas_restantes)` de las compras es igual a `horas_disponibles` tras cada operación.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/service/BilleteraService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/ConsumoResultado.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/DevolucionResultado.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/PagoRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/BilleteraServiceConsumoTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/BilleteraConsumoIntegracionTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- Los controllers de consumo y devolución (UT 06).
- Decidir cuándo una clase gasta horas: es de Académico.
- Apartar horas por un reembolso (CU31).

## Resultado esperado

Se pueden gastar y devolver horas con reintentos seguros, siempre de las compras más antiguas, sin que el saldo quede negativo ni se pierda el origen de cada hora.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con dos compras de 5 horas acreditadas, consumir 7 deja la primera en 0 y la segunda en 3; devolver esa clase las restituye.
