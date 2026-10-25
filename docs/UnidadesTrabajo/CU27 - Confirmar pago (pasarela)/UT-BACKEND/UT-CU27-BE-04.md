# UT-CU27-BE-04 — Verificar el aviso firmado de la pasarela

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU27-BE-04` |
| **Nombre** | Verificar el aviso firmado de la pasarela |
| **Caso de uso** | CU27-BE |
| **Microservicio** | `pagos` |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Solo un aviso que de verdad viene de la pasarela puede confirmar un pago: cualquier otro se rechaza sin tocar nada.

## Objetivo

`PasarelaPago.verificarAviso` valida la firma y traduce el evento de Stripe a un aviso propio.

## Alcance

- Se suma a `PasarelaPago` (UT 02 de CU26) el método `AvisoPasarela verificarAviso(String cuerpo, String firma)`. `AvisoPasarela(String eventoId, TipoAviso tipo, String referenciaExterna, UUID pagoCodigo, Long montoCentavos, String moneda, String cobroReferencia)` como `record`; `TipoAviso` = `COBRO_COMPLETADO`, `COBRO_VENCIDO`, `OTRO`.
- `StripePasarela.verificarAviso`: verifica la firma con `stripeWebhookSecret` y una tolerancia de `app.pagos.tolerancia-firma-segundos` (por defecto 300; la librería de Stripe la comprueba); firma ausente, mal formada, vencida, o con secreto vacío → `ValidationException` `ERR_PAG_08` (400). Mapea: `checkout.session.completed` con `payment_status = paid` → `COBRO_COMPLETADO`; `checkout.session.expired` → `COBRO_VENCIDO`; cualquier otro evento → `OTRO`. Toma `client_reference_id` como `pagoCodigo`, el id de la sesión como `referenciaExterna`, `amount_total` y `currency` como monto y moneda, y el id del cobro (`payment_intent`) en `cobroReferencia` (lo usa CU31 para reembolsar).
- Se agrega `ERR_PAG_08` (aviso con firma inválida, 400) al catálogo y a `docs/errors/`. Se registra `WARN` (`module = pagos`) sin el cuerpo ni la firma.
- Tests (`StripePasarelaAvisoTest`) con cuerpos firmados que el propio test calcula con HMAC-SHA256 y el secreto de prueba: aviso válido de cada tipo; firma alterada, cuerpo alterado, fecha fuera de tolerancia, sin cabecera y secreto vacío → `ERR_PAG_08`; un evento desconocido → `OTRO`; una sesión sin `payment_status = paid` no es `COBRO_COMPLETADO`.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/PasarelaPago.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/StripePasarela.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/AvisoPasarela.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/pasarela/TipoAviso.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/PagosProperties.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/common/exception/PagosErrorCodes.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/StripePasarelaAvisoTest.java
- docs/errors/pagos/ERRORES_PAGOS.md
- docs/errors/README.md

## Fuera de alcance (no tocar)

- Qué se hace con cada aviso (UT 05).
- El controller y la seguridad del endpoint (UT 06).
- Los avisos de reembolso (CU31).

## Resultado esperado

Un aviso con firma válida se convierte en un `AvisoPasarela` tipado; cualquier otro se rechaza con 400 `ERR_PAG_08`.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. El test de firma pasa con el secreto de prueba y falla al cambiar un solo carácter del cuerpo.
