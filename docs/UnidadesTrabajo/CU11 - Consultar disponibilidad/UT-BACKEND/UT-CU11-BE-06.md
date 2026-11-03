# UT-CU11-BE-06 — Ruta en el gateway y mapa de servicios

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU11-BE-06` |
| **Nombre** | Ruta en el gateway y mapa de servicios |
| **Caso de uso** | CU11-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU11-BE-05 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) y [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md#recursos-http-de-cada-servicio): un recurso nuevo se registra en el gateway y en el mapa **en el mismo cambio**.

## Solicitud de negocio

La web solo habla con el gateway: sin la ruta, la pantalla de disponibilidad recibiría un 404.

## Objetivo

El gateway enruta `/api/v1/disponibilidad-instructores/**` a Escuela y el mapa de servicios refleja el recurso y la nueva llamada entre servicios.

## Alcance

- `gateway/src/main/resources/application.properties`: sumar `/api/v1/disponibilidad-instructores/**` al predicado `Path=` de la ruta `escuela`.
- `MICROSERVICIOS.md`: el recurso `disponibilidad-instructores` en la fila de `escuela` de "Recursos HTTP de cada servicio", y una fila nueva en la tabla de llamadas REST entre servicios: **Escuela → Académico**, consultar el resumen de franjas de los instructores de la escuela para un día (reenvía el Bearer del Administrador), CU11.
- Un caso en `GatewayRoutingTest` solo si la ruta tuviera algo particular: es un path simple, así que no hace falta un test por recurso.

Archivos esperados (crear/tocar): `application.properties` del gateway y `MICROSERVICIOS.md`.

## Fuera de alcance (no tocar)

- Rutas de otros servicios y la ruta de `franjas`: ya enruta `/api/v1/franjas/**` a Académico, que cubre `/franjas/resumen`.
- Filtros de seguridad, CORS y rate limit del gateway.
- Código de Escuela o de Académico.

## Resultado esperado

La web llega a la disponibilidad por el gateway y el mapa de servicios dice lo mismo que el gateway.

## Validación

1. `./mvnw test` del gateway en verde y `./mvnw checkstyle:check` sin errores.
2. `GET http://localhost:8080/api/v1/disponibilidad-instructores` con el token de `adminexample@gmail.com` responde `200`; sin token, `401`.
3. `disponibilidad-instructores` aparece en `application.properties` y en `MICROSERVICIOS.md`.
