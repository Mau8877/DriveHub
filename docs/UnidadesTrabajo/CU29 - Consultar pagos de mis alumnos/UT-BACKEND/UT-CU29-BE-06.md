# UT-CU29-BE-06 — Gateway: ruta de horas de los alumnos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-BE-06` |
| **Nombre** | Gateway: ruta de horas de los alumnos |
| **Caso de uso** | CU29-BE |
| **Microservicio** | `gateway` |
| **Depende de** | UT-CU29-BE-05 |

## Antes de implementar

Leer [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md), [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La pantalla del instructor llega a Pagos por el único punto de entrada de la API.

## Objetivo

El gateway enruta `/api/v1/horas-alumnos/**` a `pagos` y el mapa de servicios lo refleja.

## Alcance

- `application.properties` del gateway: se suma `/api/v1/horas-alumnos/**` al predicado `Path=` de la ruta `pagos`.
- [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md): la fila de recursos de `pagos` suma `horas-alumnos`; la tabla de llamadas REST entre servicios suma **Pagos → Escuela** (alumnos asignados del instructor, reenvía el Bearer) y amplía la fila de **Pagos → Identidad** (que CU26 ya registró) con la consulta de nombres de estudiantes.
- `GatewayRoutingTest`: la ruta nueva llega al servicio falso.

Archivos esperados (crear/tocar):

- projects/backend/gateway/src/main/resources/application.properties
- projects/backend/gateway/src/test/java/com/drivehub/gateway/GatewayRoutingTest.java
- .claude/rules/MICROSERVICIOS.md

## Fuera de alcance (no tocar)

- Otras rutas.

## Resultado esperado

Por el gateway se alcanza el listado de horas de los alumnos.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `gateway`.
2. Con el stack levantado, `GET /api/v1/horas-alumnos` responde por el gateway.
