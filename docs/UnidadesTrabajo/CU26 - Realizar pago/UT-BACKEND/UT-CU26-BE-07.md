# UT-CU26-BE-07 — Controller de pagos, página de retorno y seguridad

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-07` |
| **Nombre** | Controller de pagos, página de retorno y seguridad |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU26-BE-05 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La app necesita iniciar la compra, consultar su estado y volver de la página de pago a una pantalla clara.

## Objetivo

`POST /api/v1/pagos`, `GET /api/v1/pagos/{codigo}/estado` y la página pública `GET /api/v1/pagos/retorno`.

## Alcance

- `config/CurrentUserId` (anotación) y su soporte en `CurrentUserArgumentResolvers`: entregan el `sub` del token (el código de la persona); un `sub` que no sea un UUID es una sesión inválida (401). `common/util/EscuelaDelToken.exigir(...)` es el helper compartido que responde 403 `ERR_PAG_04` si el token no trae la escuela (`PlanPagoController` pasa a usarlo).
- `PagoController`: `POST /api/v1/pagos` lee el header **`Idempotency-Key`** (obligatorio; su ausencia es `400 ERR_SYS_01`), arma la petición con `@CurrentUserId` y `@CurrentEscuelaId`, responde `201` con `PagoIniciadoResponse` si el pago se creó y `200` con el mismo cuerpo si es un reintento; `GET /api/v1/pagos/{codigo}/estado`. Delgado, sin lógica ni logs ([ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md)).
- `RetornoPagoController`: `GET /api/v1/pagos/retorno` devuelve una **página HTML mínima y fija** en español ("Recibimos tu pago. Vuelve a DriveHub para ver el resultado.") con un enlace que abre la app. El enlace sale de la configuración (`PAGOS_ENLACE_APP`, hoy `drivehub://app/payments/return`), no está escrito en el código, y se escapa para HTML. Es la `success_url` y la `cancel_url` de la sesión de cobro (`PAGOS_URL_RETORNO`); no refleja ningún parámetro de la URL.
- `SecurityConfig`: ese único path queda público (`permitAll`), todo lo demás de `/api/v1/pagos/**` exige sesión. Es una excepción documentada: se suma a la lista de rutas fuera del sobre de [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md) y de [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md).
- `PagosProperties.urlRetorno` alimenta la `urlExito` y la `urlCancelacion` de `SolicitudCobro` (se ajusta `PagoService.iniciar` si en la UT 05 quedaron como parámetros).
- Tests `@WebMvcTest`: sobre `ApiResponse` (`status`, `data`, sin `error`), `201` y `200` en reintento, header ausente → `400 ERR_SYS_01`, `401` sin sesión, `403` para otro rol, `404 ERR_PAG_07` en el estado; la página de retorno responde `200 text/html` sin sesión y sin reflejar parámetros (`?x=<script>`).

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/controller/PagoController.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/controller/RetornoPagoController.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/config/SecurityConfig.java
- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/service/PagoService.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoControllerTest.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/RetornoPagoControllerTest.java
- .claude/rules/backend/RESPONSES_BACKEND.md
- .claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md

## Fuera de alcance (no tocar)

- Confirmar el pago y el aviso de la pasarela (CU27).
- El listado e historial de pagos (CU28).
- Servir la página de retorno desde otro servicio o desde la app.

## Resultado esperado

La app puede iniciar una compra, preguntar por su estado y volver de la página de pago a una pantalla de bienvenida.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el stack levantado y un token de estudiante, `POST /api/v1/pagos` con `Idempotency-Key` devuelve 201 con `urlPago`; la misma petición devuelve 200 y el mismo pago; `GET /api/v1/pagos/retorno` abre sin token.
