# UT-CU44-BE-04 — Gateway: test de que no enruta `/internal`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-04` |
| **Nombre** | Gateway: test de que no enruta `/internal` |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `gateway` |
| **Depende de** | — |

## Antes de implementar

La decisión 4 del [README](./README.md#decisiones-de-diseño). [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) (sección Tests: `GatewayRoutingTest`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Las rutas internas de Pagos jamás deben poder alcanzarse desde el navegador o la app. Hoy no lo están porque nadie las registró; debe quedar un test que impida que alguien las registre por error.

## Objetivo

`GatewayRoutingTest` comprueba que una petición a `/internal/v1/...` no se reenvía a ningún servicio.

## Alcance

- Un caso nuevo en `GatewayRoutingTest`: `POST /internal/v1/billeteras-horas/consumos` contra el gateway levantado con el servicio falso del test → responde `404` (el mismo sobre de ruta desconocida que ya verifican los otros casos) y **el servicio falso no recibe ninguna petición**.
- Un segundo caso con `/api/v1/internal/...` y `/api/v1/billeteras-horas/../../internal/...` si el gateway los trata distinto: ambos tampoco llegan al servicio.
- No se cambia ninguna ruta ni `application.properties`.

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayRoutingTest.java`

## Fuera de alcance (no tocar)

- `application.properties` del gateway y cualquier ruta: el gateway **no** cambia.
- Los endpoints internos de Pagos (UT 03).
- Reglas de CORS o rate limit.

## Resultado esperado

Si alguien registra por error `/internal` en el gateway, este test se pone en rojo.

## Validación

1. `./mvnw test` en `gateway` pasa.
2. `./mvnw checkstyle:check` sin errores.
3. Prueba de que el test sirve: agregar temporalmente `/internal/**` a un predicado `Path=` y comprobar que el test falla; revertirlo.
