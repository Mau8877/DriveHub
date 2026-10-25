# UT-CU26-BE-08 — Gateway: quitar los recursos de cuotas e inscripciones

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-BE-08` |
| **Nombre** | Gateway: quitar los recursos de cuotas e inscripciones |
| **Caso de uso** | CU26-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU26-BE-01 |

## Antes de implementar

Leer [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

`inscripciones-plan` y `cuotas` desaparecieron con la compra directa de horas; el gateway no debe seguir enrutándolas a un servicio que ya no las tiene.

## Objetivo

El gateway deja de enrutar `/api/v1/inscripciones-plan/**` y `/api/v1/cuotas/**`, y el mapa de servicios lo refleja.

## Alcance

- `application.properties` del gateway: se quitan `/api/v1/inscripciones-plan/**` y `/api/v1/cuotas/**` del predicado `Path=` de la ruta `pagos`.
- [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md): la fila de recursos de `pagos` queda `planes-pago`, `pagos`, `comprobantes` (CU27 y CU29 suman los suyos).
- `GatewayRoutingTest`: `/api/v1/cuotas` y `/api/v1/inscripciones-plan` responden 404; `/api/v1/pagos` y `/api/v1/planes-pago` siguen enrutando a `pagos`.

Archivos esperados (crear/tocar):

- projects/backend/gateway/src/main/resources/application.properties
- projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayRoutingTest.java
- .claude/rules/MICROSERVICIOS.md

## Fuera de alcance (no tocar)

- Rutas nuevas de CU27 y CU29.
- Cambios en el rate limit o en CORS.

## Resultado esperado

Pedir `/api/v1/cuotas` o `/api/v1/inscripciones-plan` al gateway devuelve 404 y el resto de `pagos` sigue funcionando.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway`.
2. Con el stack levantado, `GET /api/v1/planes-pago` sigue respondiendo y `GET /api/v1/cuotas` da 404.
