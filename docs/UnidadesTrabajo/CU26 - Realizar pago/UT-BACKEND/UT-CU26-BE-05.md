# UT-CU26-BE-05 — Iniciar la compra: pago pendiente y sesión de cobro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-05` |
| **Nombre** | Iniciar la compra: pago pendiente y sesión de cobro |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU26-BE-01, UT-CU26-BE-02, UT-CU26-BE-03, UT-CU26-BE-04 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Al confirmar el resumen, el estudiante queda enviado a pagar con el monto ya fijado, sin riesgo de cobrarle dos veces si reintenta.

## Objetivo

`PagoService.iniciar` crea el pago pendiente y la sesión de cobro de forma idempotente, y `estado` informa su resultado.

## Alcance

- DTOs (`record`): `IniciarPagoRequest(UUID planCodigo)` con `@NotNull`; `PagoIniciadoResponse(codigo, estado, urlPago, expiraEn, planNombre, horas, montoUsd, montoBs)`; `EstadoPagoResponse(codigo, estado)`.
- `PagoRepository` (`findByIdempotencyKey`, `findByCodigoAndEstudianteCodigoAndEscuelaCodigo`) y `TransaccionPasarelaRepository` (`findFirstByPagoIdOrderByIdDesc`).
- `PagoService` (clase concreta, `@RequiredArgsConstructor`):
  - `iniciar(escuelaCodigo, estudianteCodigo, planCodigo, idempotencyKey)` con `@PreAuthorize("hasRole('ESTUDIANTE')")`. Devuelve el resultado y si se creó o se repitió.
    1. La clave es obligatoria, de 1 a 64 caracteres; si no, `ValidationException` `ERR_SYS_01`.
    2. Si ya hay un pago con esa clave: si es del mismo estudiante y del mismo plan, devuelve el mismo (con la `url_pago` de su sesión); si la sesión todavía no se creó, `ConflictException` `ERR_PAG_17`; si es de otro estudiante u otro plan, `ConflictException` `ERR_PAG_18`.
    3. El plan se busca por código y escuela y debe estar activo; si no, `NotFoundException` `ERR_PAG_05` (no se distingue "no existe" de "es de otra escuela").
    4. Pide el perfil a Identidad (`IdentidadClient.perfilPropio`, con `BearerActual`: trae el nombre del estudiante y el de su escuela), calcula `monto` con `ConversionMoneda.aDolares(costoBs)` y toma `tipo_cambio` de la configuración.
    5. Guarda el pago `PENDIENTE` (concepto `HORAS`, método `TARJETA`, moneda `USD`, todas las copias, `expira_en = ahora + vigenciaSesionMinutos + 1 minuto de holgura`, porque la pasarela exige 30 minutos completos y entre que se calcula la hora y ella recibe el pedido pasan unos segundos) en su **propia transacción** (`TransactionTemplate`, no se llama a la pasarela con una transacción abierta). Si dos peticiones con la misma clave chocan en el índice único, la que pierde relee el pago y sigue por el paso 2.
    6. Llama a `PasarelaPago.crearSesionCobro` con la clave del pago como clave de idempotencia. Si falla, marca el pago `FALLIDO` (otra transacción) y relanza `ERR_PAG_06`.
    7. Guarda la `TransaccionPasarela` (`proveedor = STRIPE`, `referencia_externa` = id de sesión, `url_pago`, `estado = CREADA`) y devuelve el resultado.
  - `estado(estudianteCodigo, codigo)`: `EstadoPagoResponse`; `NotFoundException` `ERR_PAG_07` si el pago no existe o es de otra persona.
  - Registra `INFO` al crear (`module = pagos`, `pagoCodigo`, `escuelaCodigo`) y `WARN` al fallar la pasarela, sin nombres, montos ni la dirección de pago.
- Se agregan al catálogo (`PagosErrorCodes`) y a `docs/errors/` los códigos `ERR_PAG_05`, `ERR_PAG_07`, `ERR_PAG_17` y `ERR_PAG_18`.
- Tests de service con repositorios, clientes y pasarela mockeados (`PagoServiceTest`): compra feliz (pago con todas las copias, vence en 30 min, sesión guardada); el monto en USD usa el tipo de cambio configurado; plan inexistente, desactivado o de otra escuela → `ERR_PAG_05`; mismo `Idempotency-Key` → mismo pago sin segunda sesión; clave de otro estudiante u otro plan → `ERR_PAG_18`; carrera con índice único → relee; sesión aún sin crear → `ERR_PAG_17`; falla de la pasarela → pago `FALLIDO` y `ERR_PAG_06`; `estado` de un pago ajeno → `ERR_PAG_07`; el log no contiene el monto. Un test `@SpringBootTest` + Testcontainers (`PagoServiceIntegracionTest`) con pasarela y clientes falsos: dos compras simultáneas con la misma clave dejan un solo pago.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/dto/IniciarPagoRequest.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/dto/PagoIniciadoResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/dto/EstadoPagoResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/PagoRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/repository/TransaccionPasarelaRepository.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/PagoService.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoServiceTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoServiceIntegracionTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- El controller y la página de retorno (UT 07), y el vencimiento de pagos pendientes (UT 06).
- Confirmar el pago, acreditar horas y los avisos de la pasarela (CU27).
- La billetera, el comprobante y el reembolso.

## Resultado esperado

Una compra válida deja un pago pendiente con todo fijado y la dirección de pago; repetirla devuelve lo mismo; una falla de la pasarela deja el pago fallido y responde 503.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con una clave de prueba de Stripe y el estudiante de ejemplo, `iniciar` dos veces con la misma clave devuelve el mismo `urlPago` y en el panel de Stripe hay una sola sesión.
