# UT-CU53-BE-02 — Pagos: avisar a la escuela de compras y reembolsos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU53-BE-02` |
| **Nombre** | Avisos de compra confirmada y reembolso ejecutado en la bandeja de la escuela |
| **Caso de uso** | CU53-BE |
| **Microservicio** | `pagos` |
| **Depende de** | UT-CU53-BE-01 |

## Antes de implementar

[ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca datos de pago en los logs) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

El Administrador quiere enterarse de las compras de horas y de los reembolsos de su escuela sin revisar los reportes.

## Objetivo

Al confirmarse una compra o ejecutarse un reembolso, Pagos deja además un aviso en la bandeja de la escuela del pago.

## Alcance

- `features/pagos/client/NotificacionesClient.java`: `avisarEscuela(...)`, que manda `escuelaCodigo` en lugar de `usuarioCodigo` (un `enviar` privado común a los dos).
- `features/pagos/service/AvisosPagoListener.java`: en `PagoConfirmado`, «Nueva compra de horas» (`{nombre} compró {horas} hora(s) del plan {plan}.`); en `ReembolsoEjecutado`, «Reembolso realizado» (`Se devolvió a {nombre} la compra del plan {plan}.`). Evento `pago.confirmado.escuela` / `pago.reembolsado.escuela` para no chocar con el aviso del estudiante. Sin nombre copiado, «Un estudiante».
- Tests: `AvisosPagoListenerTest` y `NotificacionesClientTest`.

Archivos esperados (crear/tocar): los dos de producción y sus dos tests.

## Fuera de alcance (no tocar)

- Los avisos al estudiante: siguen igual.
- Pago vencido, compensación y reembolso fallido: no avisan a la escuela.
- Montos o datos de la tarjeta en el aviso.

## Resultado esperado

Cada compra confirmada y cada reembolso ejecutado aparecen una sola vez en la bandeja de la escuela, después del commit y sin poder frenar la operación.

## Validación

1. `./mvnw test -Dtest=AvisosPagoListenerTest,NotificacionesClientTest` en `projects/backend/pagos`: verde.
2. `./mvnw checkstyle:check` sin errores.
3. Manual: comprar un plan con la tarjeta de prueba `4242 4242 4242 4242`; el Administrador ve «Nueva compra de horas» en la campana.
