# UT-CU28-BE-01 — Consultas de pagos del estudiante

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU28-BE-01` |
| **Nombre** | Consultas de pagos del estudiante |
| **Caso de uso** | CU28-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El estudiante tiene que ver sus compras, de la más reciente a la más antigua, filtradas por estado, sin ver nunca las de otra persona.

## Objetivo

El repositorio de pagos lista y busca pagos de un estudiante, con el estado que ve el estudiante.

## Alcance

- `features/pagos/enums/EstadoPagoVisible` = `PENDIENTE`, `CONFIRMADO`, `FALLIDO`, `REEMBOLSO_EN_PROCESO`, `REEMBOLSADO`. Se **deriva**, no se guarda: `PENDIENTE`, `CONFIRMADO` y `FALLIDO` son el estado del pago, salvo que un pago `CONFIRMADO` tenga una reversión `SOLICITADA`, que se ve `REEMBOLSO_EN_PROCESO`; `REVERTIDO` se ve `REEMBOLSADO`.
- `PagoSpecification` (en `features/pagos/repository/`): `deEstudiante(escuelaCodigo, estudianteCodigo)` (siempre las dos), `conEstadoVisible(EstadoPagoVisible)` (el filtro `REEMBOLSO_EN_PROCESO` usa una subconsulta `exists` sobre `reversion_pago` con estado `SOLICITADA`; el de `CONFIRMADO` excluye esos casos) y `deConcepto(HORAS)`.
- `PagoRepository` extiende `JpaSpecificationExecutor<Pago>` si no lo hacía; `findByCodigoAndEstudianteCodigoAndEscuelaCodigo` (CU26) se reutiliza para el detalle.
- `ReversionPagoRepository` (`existsByPagoIdAndEstado`, `findFirstByPagoIdOrderByIdDesc`) con las consultas que necesita el detalle (UT 02).
- Tests de repositorio con Testcontainers (`PagoConsultaRepositoryTest`): solo los pagos del estudiante pedido y de su escuela (otro estudiante y otra escuela no aparecen); orden `created_at DESC, id DESC`; cada filtro de estado devuelve lo que corresponde, incluido `REEMBOLSO_EN_PROCESO` con una reversión `SOLICITADA` y `REEMBOLSADO` con un pago `REVERTIDO`; paginación y total; un pago con `deleted_at` no aparece.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/enums/EstadoPagoVisible.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/PagoSpecification.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/PagoRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/ReversionPagoRepository.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoConsultaRepositoryTest.java

## Fuera de alcance (no tocar)

- El servicio, los DTO y el controller (UT 02 y 03).
- Crear reversiones o comprobantes (CU30 y CU31).
- Guardar el estado visible: se calcula.

## Resultado esperado

Se puede consultar por página los pagos de un estudiante, filtrando por el estado que él ve, sin que se mezclen datos de otros.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con datos de dos estudiantes, la consulta de uno nunca trae filas del otro.
