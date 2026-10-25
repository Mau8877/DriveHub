# UT-CU28-BE-02 — Servicio de consulta: listado, detalle y reembolso previsto

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU28-BE-02` |
| **Nombre** | Servicio de consulta: listado, detalle y reembolso previsto |
| **Caso de uso** | CU28-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU28-BE-01 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El estudiante ve cada compra con sus horas restantes y, si ninguna se usó, cuánto recibiría si pide el reembolso.

## Objetivo

`ConsultaPagosService` devuelve el listado y el detalle de los pagos del estudiante, con el reembolso previsto.

## Alcance

- `ReembolsoCalculadora` (`@Component`, en `features/pagos/service/`, la reutiliza CU31): `ReembolsoPrevisto calcular(BigDecimal montoPagado)` donde `montoDevuelto = montoPagado × (100 − retención) ÷ 100` con escala 2 y `RoundingMode.HALF_UP`, y `montoRetenido = montoPagado − montoDevuelto` (así la suma da siempre exactamente lo pagado). `ReembolsoPrevisto(montoDevuelto, montoRetenido, porcentajeRetenido)`.
- `PagosProperties`: `retencionReembolsoPorcentaje` (variable `REEMBOLSO_RETENCION_PORCENTAJE`, por defecto `5`, entre 0 y 50; fuera de rango el servicio no arranca).
- DTOs (`record`): `PagoResumenResponse(codigo, planNombre, horas, montoUsd, montoBs, estado, creadoEn)` con `estado` = `EstadoPagoVisible`; `PagoDetalleResponse(...lo anterior, horasRestantes, confirmadoEn, reembolsable, reembolso)` donde `reembolso` es el `ReembolsoPrevisto` o `null` si no es reembolsable. El id interno y `escuelaCodigo` nunca salen.
- `ConsultaPagosService` (clase concreta, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`, `@PreAuthorize("hasRole('ESTUDIANTE')")`):
  - `listar(escuelaCodigo, estudianteCodigo, page, pageSize, estado)`: `page` 1, `pageSize` 20 y máximo 100; orden `createdAt DESC, id DESC`; solo concepto `HORAS`; devuelve la página de `PagoResumenResponse` y el `PageMeta`.
  - `detalle(escuelaCodigo, estudianteCodigo, codigo)`: `NotFoundException` `ERR_PAG_07` si el pago no existe o es de otra persona. `reembolsable` es verdadero solo si el pago está `CONFIRMADO`, `horasRestantes == horas` y no hay ninguna reversión `SOLICITADA` ni `EJECUTADA` del pago.
  - El estado visible se calcula en un solo lugar (un método estático de `EstadoPagoVisible` o del mapper) y lo usan listado y detalle.
- Sin logs de lectura (solo se loguean cambios).
- Tests de service con repositorios mockeados: listado con valores por defecto y tope; filtro por estado; detalle de pago ajeno → `ERR_PAG_07`; `reembolsable` en cada combinación (confirmado intacto, con horas usadas, pendiente, fallido, con reversión solicitada, con reversión ejecutada); `ReembolsoCalculadoraTest`: 100,00 → 95,00 y 5,00; 7,13 → 6,77 y 0,36 (el redondeo no pierde centavos: suman 7,13); retención 0 devuelve todo; propiedad fuera de rango no arranca.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/ConsultaPagosService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/ReembolsoCalculadora.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/dto/PagoResumenResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/dto/PagoDetalleResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/dto/ReembolsoPrevisto.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ConsultaPagosServiceTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/ReembolsoCalculadoraTest.java

## Fuera de alcance (no tocar)

- El controller (UT 03).
- Pedir y ejecutar el reembolso (CU31).
- Los comprobantes del pago: los suma CU30 al detalle cuando exista el comprobante.

## Resultado esperado

El service entrega la lista y el detalle de los pagos de un estudiante, con el estado visible, las horas restantes y el reembolso previsto cuando corresponde.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Un pago de 100,00 USD confirmado e intacto muestra `reembolsable = true` con 95,00 y 5,00.
