# UT-CU49-BE-04 — Comprar horas con confirmación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU49-BE-04` |
| **Nombre** | Comprar horas con confirmación |
| **Caso de uso** | CU49-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | UT-CU49-BE-01, UT-CU49-BE-02 |

## Antes de implementar

[CU26](../../../CasosDeUso/CU26%20-%20Realizar%20pago.md), [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md) y `PagoController`/`IniciarPagoRequest` de Pagos (contrato real).

## Solicitud de negocio

El estudiante compra un plan de horas por chat y paga con tarjeta en el enlace de la pasarela.

## Objetivo

Existen `preparar_compra` y `confirmar_compra`.

## Alcance

- `preparar_compra(plan: str)` (código de plan): verifica el plan con `GET pagos /api/v1/planes-pago/disponibles` y guarda una confirmación `COMPRA`; devuelve el resumen (plan, horas, monto en USD y su equivalente en Bs) y el `codigoConfirmacion`. No cambia ningún dato.
- `confirmar_compra(codigo_confirmacion)`: consume la confirmación, verifica el tope diario, llama `POST pagos /api/v1/pagos` reenviando el Bearer y devuelve el enlace de pago de la pasarela y el aviso de que las horas se acreditan al pagar. Repetirla devuelve el mismo enlace sin crear otro pago.
- El enlace de pago nunca se escribe en los logs.
- Código y lógica en `app/features/compras_chat/services/`; la herramienta es delgada.
- Tests: prepara y confirma, plan inexistente o de otra escuela, confirmación inválida, repetición, pasarela caída (503 de Pagos → mensaje legible), tope diario.

Archivos esperados (crear/tocar): `app/features/compras_chat/**`, `app/mcp/herramientas.py`, tests.

## Fuera de alcance (no tocar)

- Cobrar dentro del chat, reembolsos, comprobantes y cambios en Pagos.

## Resultado esperado

Con una confirmación válida el estudiante recibe el enlace de pago; no se cobra nada desde el chat.

## Validación

1. `python -m pytest` y `ruff` en `projects/backend/asistente-ia`.
2. Con el stack: `confirmar_compra` crea un pago pendiente y devuelve un enlace; llamarla otra vez no crea un segundo pago.
