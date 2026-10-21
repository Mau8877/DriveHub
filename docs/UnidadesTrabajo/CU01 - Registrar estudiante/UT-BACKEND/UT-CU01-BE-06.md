# UT-CU01-BE-06 — Ruta `escuelas` en el gateway

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-06` |
| **Nombre** | Ruta `escuelas` en el gateway |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU01-BE-04 |

## Antes de implementar

[GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md) (rutas por recurso y tests) y [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md) (tabla de recursos).

## Solicitud de negocio

La web consulta el nombre de la escuela por el gateway; un recurso que no está en el gateway responde 404 aunque el servicio lo implemente.

## Objetivo

`/api/v1/escuelas/**` llega al servicio Escuela a través del gateway.

## Alcance

- Agregar `/api/v1/escuelas/**` al predicado `Path=` de la ruta de `escuela` en `gateway/src/main/resources/application.properties`.
- Agregar `escuelas` a la fila de `escuela` en la tabla "Recursos HTTP de cada servicio" de `.claude/rules/MICROSERVICIOS.md`, en el mismo cambio.
- Un caso en `GatewayRoutingTest` que verifique que `/api/v1/escuelas/escuelaexample` se enruta al servicio falso de Escuela con la misma ruta.

Archivos esperados (crear/tocar):

- `projects/backend/gateway/src/main/resources/application.properties`
- `projects/backend/gateway/src/test/java/.../GatewayRoutingTest.java`
- `.claude/rules/MICROSERVICIOS.md`

## Fuera de alcance (no tocar)

- Las rutas de `usuarios` (ya existe) y del resto de recursos.
- CORS, rate limit y validación de JWT del gateway.
- Lógica de negocio en el gateway.

## Resultado esperado

`GET http://localhost:8080/api/v1/escuelas/escuelaexample` llega a Escuela y devuelve su respuesta; la tabla de `MICROSERVICIOS.md` refleja el recurso.

## Validación

1. `./mvnw test` en `gateway` pasa, con el caso nuevo de `GatewayRoutingTest`.
2. Con el stack levantado (UT 04 y 05 aplicadas): `curl http://localhost:8080/api/v1/escuelas/escuelaexample` devuelve `200`.
3. `./mvnw checkstyle:check` sin errores.
4. La fila de `escuela` en `MICROSERVICIOS.md` incluye `escuelas`.
