# UT-CU49-BE-03 — Reservar clases con confirmación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU49-BE-03` |
| **Nombre** | Reservar clases con confirmación |
| **Caso de uso** | CU49-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | UT-CU49-BE-01, UT-CU49-BE-02 |

## Antes de implementar

[CU14](../../../CasosDeUso/CU14%20-%20Reservar%20horario.md), [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y `ReservaController`/`ReservarFranjasRequest` de Académico (contrato real).

## Solicitud de negocio

El estudiante reserva una o más clases por chat y solo se descuentan sus horas si confirma.

## Objetivo

Existen `preparar_reserva` y `confirmar_reserva`.

## Alcance

- `preparar_reserva(franjas: list[str])` (códigos de franja): comprueba que sean franjas libres con `GET academico /franjas/disponibles`, que las horas (`GET pagos .../mia`) alcancen, y guarda una confirmación `RESERVA`; devuelve el resumen (cada clase con instructor, fecha y hora, horas que se gastarán y las que quedarían) y el `codigoConfirmacion`. No cambia ningún dato. Si las horas no alcanzan o alguna franja no está libre, responde con el motivo y no crea confirmación.
- `confirmar_reserva(codigo_confirmacion)`: consume la confirmación, verifica el tope diario, llama `POST academico /api/v1/reservas` con las franjas, reenviando el Bearer, y devuelve las clases reservadas y las rechazadas con su motivo (como CU14); guarda el resultado para que repetirla devuelva lo mismo sin reservar otra vez.
- Código y lógica en `app/features/reservas_chat/services/` (la herramienta es delgada); logs INFO sin tokens ni datos personales.
- Actualizar CU46 (nota) no hace falta; sí la tabla de comunicación de `MICROSERVICIOS.md` (`Asistente IA → Académico` y `→ Pagos` ahora incluyen escrituras de CU49).
- Tests: prepara y confirma, horas insuficientes, franja ocupada, confirmación vencida/usada/ajena, rechazo parcial, repetir la confirmación, servicio caído, rol distinto de estudiante.

Archivos esperados (crear/tocar): `app/features/reservas_chat/**`, `app/mcp/herramientas.py`, `MICROSERVICIOS.md`, tests.

## Fuera de alcance (no tocar)

- Cancelar y reprogramar; cambios en Académico o Pagos.

## Resultado esperado

Con una confirmación válida las clases quedan reservadas y las horas descontadas, y no antes.

## Validación

1. `python -m pytest` y `ruff` en `projects/backend/asistente-ia`.
2. Con el stack: `preparar_reserva` no modifica la billetera; `confirmar_reserva` sí, una sola vez aunque se llame dos veces.
