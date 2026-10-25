# UT-CU31-BE-06 — Controller del reembolso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU31-BE-06` |
| **Nombre** | Controller del reembolso |
| **Caso de uso** | CU31-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU31-BE-03 |

## Antes de implementar

Leer [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

La app necesita un botón que inicie el reembolso de una compra.

## Objetivo

`POST /api/v1/pagos/{codigo}/reembolso`.

## Alcance

- `PagoController` suma `POST /api/v1/pagos/{codigo}/reembolso` con cuerpo `SolicitarReembolsoRequest(motivo: MotivoReembolso, motivoDetalle: String?)` (`@Valid`; `motivo` obligatorio; un valor fuera de la lista es `400 ERR_SYS_01`; el detalle de `OTRO` lo valida el service), `@CurrentUserId` y `@CurrentEscuelaId`: llama a `ReversionService.solicitarReembolso` y responde `202 Accepted` con el `PagoDetalleResponse` ya actualizado (estado `REEMBOLSO_EN_PROCESO`); la devolución termina cuando la pasarela avisa. Delgado, sin lógica ni logs.
- Tests `@WebMvcTest`: `202` con el sobre y el estado nuevo, y el motivo y su detalle llegan al service; sin cuerpo o sin `motivo`, o con un motivo desconocido → `400 ERR_SYS_01` sin llamar al service; `404 ERR_PAG_07`; `409 ERR_PAG_10` y `409 ERR_PAG_11`; `503 ERR_PAG_06`; `401` sin sesión y `403` para otro rol.

Archivos esperados (crear/tocar):

- projects/backend/pagos/src/main/java/com/drivehub/pagos/features/pagos/controller/PagoController.java
- projects/backend/pagos/src/test/java/com/drivehub/pagos/features/pagos/PagoControllerReembolsoTest.java

## Fuera de alcance (no tocar)

- Cambiar el gateway: `pagos` ya está enrutado.
- Reembolsos iniciados por el Administrador.

## Resultado esperado

La app puede iniciar el reembolso de una compra y recibir su detalle actualizado.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `pagos`.
2. Con el estudiante de ejemplo y una compra de prueba intacta, `POST /api/v1/pagos/{codigo}/reembolso` responde 202.
