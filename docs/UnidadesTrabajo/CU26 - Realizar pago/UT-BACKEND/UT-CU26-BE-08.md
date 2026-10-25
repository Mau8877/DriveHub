# UT-CU26-BE-08 — Gateway: quitar cuotas e inscripciones y dejar pública la página de retorno

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-08` |
| **Nombre** | Gateway: quitar cuotas e inscripciones y dejar pública la página de retorno |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU26-BE-01 |

## Antes de implementar

Leer [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

`inscripciones-plan` y `cuotas` desaparecieron con la compra directa de horas; el gateway no debe seguir enrutándolas a un servicio que ya no las tiene. Y el gateway valida el token de toda la API: la página a la que vuelve el estudiante de la pasarela de pago no lleva token, así que tiene que quedar pública también acá.

## Objetivo

El gateway deja de enrutar `/api/v1/inscripciones-plan/**` y `/api/v1/cuotas/**`, deja pasar sin token `GET /api/v1/pagos/retorno`, y el mapa de servicios lo refleja.

## Alcance

- `application.properties` del gateway: se quitan `/api/v1/inscripciones-plan/**` y `/api/v1/cuotas/**` del predicado `Path=` de la ruta `pagos`.
- `security/SecurityConfig`: `GET /api/v1/pagos/retorno` pasa a `permitAll` (es lo único público de Pagos); el resto de `/api/v1/pagos/**` sigue exigiendo token válido. Se documenta en [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md).
- [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md): la fila de recursos de `pagos` queda `planes-pago`, `pagos`, `comprobantes` (CU27 y CU29 suman los suyos).
- `GatewayRoutingTest` (con un servicio falso de Pagos): los recursos de Pagos llegan con su ruta intacta y `/api/v1/cuotas` e `/api/v1/inscripciones-plan` responden 404. `GatewayAuthTest`: la página de retorno llega sin token, cualquier otro método o ruta de `/api/v1/pagos` y `/api/v1/planes-pago/disponibles` responden 401 sin token y llegan con uno válido.

Archivos esperados (crear/tocar):

- projects/backend/gateway/src/main/resources/application.properties
- projects/backend/gateway/src/main/java/com/drivehub/gateway/security/SecurityConfig.java
- projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayRoutingTest.java
- projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayAuthTest.java
- .claude/rules/MICROSERVICIOS.md

## Fuera de alcance (no tocar)

- Rutas nuevas de CU27 y CU29.
- Cambios en el rate limit o en CORS.

## Resultado esperado

Pedir `/api/v1/cuotas` o `/api/v1/inscripciones-plan` al gateway devuelve 404, la página de retorno abre sin token y el resto de `pagos` sigue exigiéndolo.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway`.
2. Con el stack levantado, `GET /api/v1/pagos/retorno` abre sin token, `GET /api/v1/planes-pago/disponibles` sin token da 401 y `GET /api/v1/cuotas` da 404.
