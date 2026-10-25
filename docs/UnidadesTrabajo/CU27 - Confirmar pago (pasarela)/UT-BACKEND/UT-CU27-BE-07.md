# UT-CU27-BE-07 — Gateway: rutas del webhook y de la billetera

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU27-BE-07` |
| **Nombre** | Gateway: rutas del webhook y de la billetera |
| **Caso de uso** | CU27-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU27-BE-06 |

## Antes de implementar

Leer [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El aviso de la pasarela y la billetera tienen que alcanzar a Pagos por el único punto de entrada de la API.

## Objetivo

El gateway enruta `/api/v1/billeteras-horas/**` y `/api/v1/webhooks-stripe/**` a `pagos`, y el mapa de servicios lo refleja.

## Alcance

- `application.properties` del gateway: se suman `/api/v1/billeteras-horas/**` y `/api/v1/webhooks-stripe/**` al predicado `Path=` de la ruta `pagos`.
- El gateway **no** valida el JWT hoy: el webhook pasa sin cambios. El rate limit por IP (100 por minuto) alcanza para los avisos de Stripe; si un día se queda corto, se evalúa excluir esa ruta (se anota en [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), no se hace ahora).
- [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md): la fila de recursos de `pagos` suma `billeteras-horas` y `webhooks-stripe`.
- `GatewayRoutingTest`: las dos rutas nuevas llegan al servicio falso con su ruta y su cuerpo intactos (el cuerpo del webhook **no** se modifica: la firma depende de él).

Archivos esperados (crear/tocar):

- projects/backend/gateway/src/main/resources/application.properties
- projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayRoutingTest.java
- .claude/rules/MICROSERVICIOS.md
- .claude/rules/backend/GATEWAY_BACKEND.md

## Fuera de alcance (no tocar)

- Las rutas de CU29 (`horas-alumnos`).
- Cambiar el rate limit o validar el JWT en el gateway.

## Resultado esperado

Por el gateway se alcanzan el webhook y la billetera, y el cuerpo del webhook llega byte a byte igual.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway`.
2. Con el stack levantado y la CLI de Stripe apuntando al gateway, un aviso de prueba llega a `pagos` con firma válida.
