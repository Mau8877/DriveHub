# UT-CU39-BE-01 — Guardar la prueba de conducción

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU39-BE-01` |
| **Nombre** | Guardar la prueba de conducción |
| **Caso de uso** | CU39-BE |
| **Microservicio** | `asistente-ia` |
| **Depende de** | UT-CU40-BE-01 (seguridad JWT) |

## Antes de implementar

[FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Lo que la app comprobó en la prueba de conducción debe quedar en el historial de la persona.

## Objetivo

`POST /api/v1/sesiones-ia` valida y guarda una prueba terminada con un mensaje por paso.

## Alcance

- Schemas `PasoPruebaSchema`, `CrearSesionPruebaRequest` y `SesionResponse` (JSON en camelCase).
- `SesionRepository.guardar_con_mensajes()` (`transact_write_items` sobre `SesionesIA` y `MensajesIA`, sin nulos y con `Decimal`).
- `SesionService.guardar_prueba()`: valida la numeración de pasos, arma la sesión `PRUEBA_CONDUCCION` / `FINALIZADA` y un mensaje `ASSISTANT` por paso, guarda y loguea.
- Router `POST /api/v1/sesiones-ia` (rol `ESTUDIANTE`), registrado en `main.py`.
- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`: cómo se usan `SesionesIA` y `MensajesIA` en CU39.
- Tests: `tests/test_sesiones.py` (service, transacción y contrato HTTP).

Archivos esperados (crear/tocar): los de `app/features/sesiones/{schemas,repositories,services,routers}`, `app/main.py`, el test y el diseño de BD.

## Fuera de alcance (no tocar)

- Listar o ver sesiones, chat, `EN_CURSO`, fotogramas o video, y la app móvil.

## Resultado esperado

Un Estudiante envía el resultado de una prueba y queda guardado como sesión con sus pasos; cualquier otro rol recibe 403.

## Validación

1. `python -m pytest` (64 casos) y `python -m ruff check . && python -m ruff format --check .` en `projects/backend/asistente-ia`.
