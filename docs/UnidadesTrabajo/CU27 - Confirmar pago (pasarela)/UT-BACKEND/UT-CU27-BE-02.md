# UT-CU27-BE-02 — Acreditar horas y consultar el saldo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU27-BE-02` |
| **Nombre** | Acreditar horas y consultar el saldo |
| **Caso de uso** | CU27-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU27-BE-01 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Cuando un pago se confirma, el estudiante tiene que recibir exactamente las horas que compró, una sola vez, aunque el aviso llegue repetido.

## Objetivo

`BilleteraService.acreditar` suma las horas del pago a la billetera (creándola si no existe) y `saldo` informa las horas disponibles.

## Alcance

- `BilleteraRepository` (`findByEscuelaCodigoAndEstudianteCodigo`) y `MovimientoHorasRepository` (`existsByIdempotencyKeyAndPagoId`, `findByBilleteraHorasIdOrderByCreatedAtDesc`), con el filtro de escuela explícito en cada consulta.
- `BilleteraService` (clase concreta, `@RequiredArgsConstructor`, `@Transactional`):
  - `boolean acreditar(Pago pago)`: **no lleva `@PreAuthorize`** (la invoca el flujo del sistema, sin sesión). Si ya existe el movimiento `ACREDITACION` con clave `acreditacion:{codigo del pago}` devuelve `false` sin tocar nada (aviso repetido). Si no: busca o crea la billetera de `(escuelaCodigo, estudianteCodigo)` del pago (si dos hilos la crean a la vez y uno choca con el índice único, relee), suma `pago.horas` a `horasDisponibles`, guarda el movimiento `ACREDITACION` y deja `pago.horasRestantes = pago.horas`. Devuelve `true`. Un conflicto de versión se reintenta hasta 3 veces.
  - `int saldo(UUID escuelaCodigo, UUID estudianteCodigo)`: las horas disponibles, o 0 si todavía no tiene billetera. `@Transactional(readOnly = true)`.
  - Registra `INFO` al acreditar (`module = billetera`, `pagoCodigo`, `escuelaCodigo`, `horas`).
- Tests de service con repositorios mockeados (`BilleteraServiceAcreditarTest`): primera compra crea la billetera con el saldo del plan; segunda compra suma; el mismo pago dos veces no suma otra vez; el movimiento lleva el pago, el tipo y la clave; `horasRestantes` queda igual a las horas; conflicto de versión se reintenta y a la tercera falla se propaga. Un test de integración (Testcontainers, `BilleteraIntegracionTest`): dos acreditaciones concurrentes de pagos distintos del mismo estudiante dejan la suma correcta y una sola billetera.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/repository/BilleteraRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/repository/MovimientoHorasRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/service/BilleteraService.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/BilleteraServiceAcreditarTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/BilleteraIntegracionTest.java

## Fuera de alcance (no tocar)

- Consumir y devolver horas (UT 03).
- Confirmar el pago y publicar eventos (UT 05).
- Controllers (UT 06).

## Resultado esperado

Acreditar un pago suma sus horas exactamente una vez y el saldo lo refleja; una acreditación repetida no cambia nada.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Acreditar dos veces el mismo pago de prueba deja `horas_disponibles` en las horas del plan y un solo movimiento.
