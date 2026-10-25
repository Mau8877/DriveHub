# UT-CU26-BE-02 — Pasarela de pago: sesión de cobro y excepción 503

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-02` |
| **Nombre** | Pasarela de pago: sesión de cobro y excepción 503 |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El estudiante paga con tarjeta en la página de la pasarela (Stripe): el sistema tiene que poder crearle esa sesión de cobro y avisarle con claridad si la pasarela no responde.

## Objetivo

Existe `PasarelaPago` con su implementación de Stripe para crear la sesión de cobro, su configuración, y la excepción de servicio no disponible (503).

## Alcance

- Dependencia `com.stripe:stripe-java` en el `pom.xml` (versión fijada en una property, como `testcontainers.version`).
- `PagosProperties` suma `Pasarela`: `stripeSecretKey`, `stripeWebhookSecret` (lo usa CU27), `vigenciaSesionMinutos` (por defecto 30, **mínimo 30** que exige Stripe y máximo 1440; si no, el servicio no arranca) y `urlRetorno` (la URL pública de `/api/v1/pagos/retorno`, UT 07). Variables `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `PAGOS_URL_RETORNO`; se agregan a `.env.example` y al `environment` del servicio `pagos` en `docker-compose.yml`. Las claves pueden estar vacías (tests, desarrollo sin cuenta): el servicio arranca igual y falla al intentar cobrar. **Nunca** se escriben claves en el repo.
- `features/pagos/pasarela/PasarelaPago` (interfaz, hay una implementación real pero la usan los tests con un doble): `SesionCobro crearSesionCobro(SolicitudCobro solicitud)`. `SolicitudCobro(UUID pagoCodigo, String idempotencyKey, String nombreProducto, long montoCentavosUsd, Instant expiraEn, String urlExito, String urlCancelacion)` y `SesionCobro(String referenciaExterna, String urlPago, Instant expiraEn)` como `record`.
- `features/pagos/pasarela/StripePasarela`: crea una Checkout Session en modo `payment`, moneda `usd`, un solo ítem (`nombreProducto`, `montoCentavosUsd`), `client_reference_id` y `metadata.pagoCodigo` = código del pago, `expires_at`, las dos URLs, y la `Idempotency-Key` de Stripe = la del pago (reintentar no crea dos sesiones). Cualquier `StripeException`, timeout o clave vacía se traduce a `ServicioNoDisponibleException` (`ERR_PAG_06`); se loguea `WARN` con `module = pagos` y el tipo de error, sin claves ni datos de tarjeta.
- `common/exception/ServicioNoDisponibleException` (`503`) y su manejo en `GlobalExceptionHandler`; `PagosErrorCodes.PASARELA_NO_DISPONIBLE = ERR_PAG_06`. Como Pagos no tenía ninguna excepción 503, se agrega a las tablas de [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) (y se corrige su nota de "el listado de 6 es el completo") y de [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), antes de usarla.
- Documentar `ERR_PAG_06` en `docs/errors/pagos/ERRORES_PAGOS.md` y en el índice de `docs/errors/README.md`, con el formato de [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).
- Tests: `StripePasarelaTest` contra un `HttpServer` del JDK que hace de API de Stripe (con `Stripe.overrideApiBase`): se envían el monto en centavos, `usd`, `client_reference_id`, `expires_at` y la clave de idempotencia; una respuesta 500 o un corte de conexión dan `ServicioNoDisponibleException`; sin clave configurada también. `PagosPropertiesTest`: la vigencia fuera de rango no arranca.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/PasarelaPago.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/StripePasarela.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/SolicitudCobro.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/SesionCobro.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/ServicioNoDisponibleException.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/GlobalExceptionHandler.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/pom.xml
- projects/backend/pagos/src/main/resources/application.properties
- .env.example
- docker-compose.yml
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/StripePasarelaTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/config/PagosPropertiesTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md
- .claude/rules/backend/EXCEPCIONES_BACKEND.md
- .claude/rules/backend/RESPONSES_BACKEND.md

## Fuera de alcance (no tocar)

- Reembolsar (CU31) y verificar avisos firmados (CU27): la interfaz solo trae la creación de la sesión.
- Llamar a Stripe de verdad desde un test.
- El servicio que decide el monto y guarda el pago (UT 05).

## Resultado esperado

Pagos puede pedirle a Stripe una sesión de cobro con la clave correcta, y una falla de la pasarela llega al cliente como 503 `ERR_PAG_06`.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con una clave de prueba real en `.env`, un test manual (no automático) crea una sesión y la dirección que devuelve abre la página de pago de Stripe.
3. Con `STRIPE_SECRET_KEY` vacía, el servicio arranca y crear una sesión responde 503.
