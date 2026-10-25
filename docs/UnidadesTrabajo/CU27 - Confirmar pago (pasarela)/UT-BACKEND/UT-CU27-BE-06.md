# UT-CU27-BE-06 — Endpoint del aviso de la pasarela y API de la billetera

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU27-BE-06` |
| **Nombre** | Endpoint del aviso de la pasarela y API de la billetera |
| **Caso de uso** | CU27-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU27-BE-03, UT-CU27-BE-05 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La pasarela necesita un punto de entrada público para avisar los cobros, y la app y Académico necesitan consultar y mover el saldo de horas.

## Objetivo

`POST /api/v1/webhooks-stripe` y los endpoints de `billeteras-horas`.

## Alcance

- `WebhookStripeController`: `POST /api/v1/webhooks-stripe` recibe el **cuerpo crudo** (`@RequestBody String`, no un DTO: la firma se calcula sobre los bytes exactos) y la cabecera `Stripe-Signature`, llama a `PasarelaPago.verificarAviso` y después a `ConfirmarPagoService.procesar`, y responde `200` con `ApiResponse.success(null, "Aviso recibido")`. Firma inválida: `400 ERR_PAG_08`. Delgado, sin lógica ni logs.
- `SecurityConfig`: **ese único path** queda público (`permitAll`, sin JWT; la firma es su autenticación). Se documenta la excepción en [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md#endpoints-públicos).
- `BilleteraController` (`/api/v1/billeteras-horas`):
  - `GET /mia` (`ESTUDIANTE`): `SaldoResponse(horasDisponibles)` del estudiante del token.
  - `GET /{estudianteCodigo}/saldo` (`INSTRUCTOR`, `ADMIN_ESCUELA`): `SaldoResponse` de ese estudiante en la escuela del token.
  - `POST /consumos` (header `Idempotency-Key` obligatorio; cuerpo `ConsumirHorasRequest(estudianteCodigo, horas, instructorCodigo, claseCodigo)`, validado con `@Valid`): `201` con `ConsumoResultado`. Un `ESTUDIANTE` solo puede consumir sobre su propia billetera (si `estudianteCodigo` no es el suyo, `403`).
  - `POST /devoluciones` (header `Idempotency-Key`; `DevolverHorasRequest(estudianteCodigo, claseCodigo)`): `200` con `DevolucionResultado`, con la misma regla de `ESTUDIANTE`.
  - La escuela siempre sale del token (`@CurrentEscuelaId`); nunca del cuerpo. El rol se decide en el service.
- Para probar en local, `docs/INFRAESTRUCTURA.md` suma una sección "Probar pagos con Stripe": la CLI de Stripe reenviando los avisos al gateway (`stripe listen --forward-to localhost:8080/api/v1/webhooks-stripe`) y cómo copiar el secreto que imprime a `STRIPE_WEBHOOK_SECRET`.
- Tests `@WebMvcTest`: el webhook acepta el cuerpo crudo sin sesión, pasa la firma tal cual al service y responde 200; firma inválida → 400 con sobre `failed` y `ERR_PAG_08`; `/mia` y `/saldo` con el sobre; consumo y devolución con sus códigos (`201`, `200`, `ERR_SYS_01` por header ausente, `409 ERR_PAG_09`, `404 ERR_PAG_16`); la matriz de roles (`401`, `403`, el estudiante ajeno).
- Se actualiza [CONTRATO_BILLETERA_HORAS.md](../../../DriveHub/CONTRATO_BILLETERA_HORAS.md) con las rutas y los campos reales.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/controller/WebhookStripeController.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/controller/BilleteraController.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/SaldoResponse.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/ConsumirHorasRequest.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/billetera/dto/DevolverHorasRequest.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/SecurityConfig.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/WebhookStripeControllerTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/billetera/BilleteraControllerTest.java
- docs/INFRAESTRUCTURA.md
- docs/DriveHub/CONTRATO_BILLETERA_HORAS.md
- .claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md

## Fuera de alcance (no tocar)

- Rutas del gateway (UT 07).
- Llamar a estos endpoints desde Académico: son los casos de uso de Académico.
- El historial y detalle de pagos (CU28).

## Resultado esperado

La pasarela puede avisar cobros a un endpoint público protegido por su firma, y quien corresponda puede consultar, gastar y devolver horas.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con la CLI de Stripe en local (`stripe trigger checkout.session.completed` o un pago de prueba real), el pago pasa a `CONFIRMADO` y `GET /api/v1/billeteras-horas/mia` muestra las horas.
