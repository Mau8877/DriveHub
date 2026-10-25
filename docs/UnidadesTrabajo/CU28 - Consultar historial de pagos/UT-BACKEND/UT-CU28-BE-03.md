# UT-CU28-BE-03 — Controller del historial y errores documentados

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU28-BE-03` |
| **Nombre** | Controller del historial y errores documentados |
| **Caso de uso** | CU28-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU28-BE-02 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La app necesita pedir el historial y el detalle de las compras del estudiante.

## Objetivo

`GET /api/v1/pagos` y `GET /api/v1/pagos/{codigo}`.

## Alcance

- `PagoController` suma `GET /api/v1/pagos` (`page`, `pageSize`, `filter[estado]`; un valor desconocido de `filter[estado]` es `400 ERR_SYS_01`) y `GET /api/v1/pagos/{codigo}`, con `@CurrentUserId` y `@CurrentEscuelaId`. Las rutas literales (`/retorno`) y `/{codigo}/estado` de CU26 conviven con estas.
- Sin lógica ni logs en el controller. `ERR_PAG_07` (CU26) ya está en el catálogo y en `docs/errors/`: esta UT no agrega códigos.
- Tests `@WebMvcTest`: el listado respeta el sobre (`data` array y `meta`); el filtro de estado se traduce; detalle `200` con todos los campos y `reembolso = null` cuando no es reembolsable; `404 ERR_PAG_07`; `401` sin sesión y `403` para otro rol; el cuerpo nunca trae el id interno ni `escuelaCodigo`.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/controller/PagoController.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoControllerTest.java

## Fuera de alcance (no tocar)

- Cambiar el gateway: `pagos` ya está enrutado.
- El reembolso (CU31) y el comprobante (CU30).

## Resultado esperado

La app puede listar y abrir las compras del estudiante con su estado, sus horas restantes y el reembolso previsto.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el estudiante de ejemplo, tras una compra de prueba confirmada, `GET /api/v1/pagos` la lista y su detalle muestra `reembolsable = true`.
