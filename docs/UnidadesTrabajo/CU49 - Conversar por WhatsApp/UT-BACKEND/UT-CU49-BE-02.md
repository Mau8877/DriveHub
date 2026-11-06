# UT-CU49-BE-02 — Confirmaciones y tope diario

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU49-BE-02` |
| **Nombre** | Confirmaciones y tope diario |
| **Caso de uso** | CU49-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | — |

## Antes de implementar

[FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (DynamoDB: claves con codigos públicos, TTL, tablas declaradas en código) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

Reservar y comprar cuestan horas y plata: ninguna acción debe ejecutarse sin que la persona la haya confirmado, ni más veces de las razonables.

## Objetivo

Existe un almacén de confirmaciones de un solo uso y un tope diario de escrituras por persona.

## Alcance

- Tabla `ConfirmacionesIA` en `app/common/dynamodb/tablas.py`: partición `usuarioCodigo`, orden `'<fecha ISO>#<codigoConfirmacion>'`, atributos `codigoConfirmacion` (UUID v7), `accion` (`RESERVA` | `COMPRA`), `datos` (lo que se confirmará), `estado` (`PENDIENTE` | `EJECUTADA`), `resultado` (opcional), `ttl` (epoch, 10 minutos para pendientes). Documentarla en `docs/DriveHub/DISENO_BASE_DE_DATOS.md`.
- Feature `app/features/confirmaciones/` con `models/`, `repositories/` y `services/ConfirmacionesService`: `crear(usuario, accion, datos)`, `consumir(usuario, codigo, accion)` (valida que sea de esa persona, de esa acción, pendiente y vigente; marca ejecutada de forma condicional para que dos llamadas simultáneas no ejecuten dos veces), `registrar_resultado`, `ejecutadas_hoy(usuario)`.
- Tope: `TOPE_ESCRITURAS_DIARIAS` (por defecto 10); superarlo lanza `BusinessException` (`ERR_IA_NN`, 429 no existe como excepción: usar `ConflictException` con mensaje claro, o la que defina el catálogo del servicio).
- Códigos `ERR_IA_NN` nuevos para: confirmación inválida o vencida y tope superado; constantes en `app/common/error_codes.py` y sus secciones en `docs/errors/asistente-ia/ERRORES_ASISTENTE_IA.md` con links en `docs/errors/README.md`.
- Tests: crear/consumir, de otra persona, de otra acción, vencida, usada dos veces, concurrencia simulada, y tope.

Archivos esperados (crear/tocar): `tablas.py`, `app/features/confirmaciones/**`, `core/config.py`, `error_codes.py`, `docs/errors/**`, `DISENO_BASE_DE_DATOS.md` y los tests.

## Fuera de alcance (no tocar)

- Las herramientas que usan las confirmaciones (UT 03 y 04).

## Resultado esperado

Una confirmación se crea, se consume exactamente una vez y solo por su dueña.

## Validación

1. `python -m pytest` y `ruff` en `projects/backend/asistente-ia`.
2. Con dynamodb-local, la tabla se crea al arrancar con TTL activo.
