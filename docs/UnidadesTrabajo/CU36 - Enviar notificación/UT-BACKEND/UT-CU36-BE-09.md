# UT-CU36-BE-09 — Dispositivos del navegador (`WEB`)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-09` |
| **Nombre** | Dispositivos del navegador (`WEB`) |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | UT-CU36-BE-03 |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#dynamodb).

## Solicitud de negocio

Las personas que usan el panel web también tienen que recibir los avisos en su navegador.

## Objetivo

El navegador de una persona se registra como un dispositivo más y recibe el push.

## Alcance

- `Plataforma.WEB` en `features/dispositivos/enums/plataforma.enum.ts`. El registro, el envío (`sendEachForMulticast` ya entrega a tokens web) y la desactivación de tokens inválidos no cambian.
- Tests del service y del controller con `WEB`.
- Actualizar `DISENO_BASE_DE_DATOS.md` (`plataforma`: `ANDROID`, `IOS`, `WEB`).

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/dispositivos/enums/plataforma.enum.ts
- projects/backend/notificaciones/src/features/dispositivos/tests/dispositivos.service.spec.ts
- projects/backend/notificaciones/src/features/dispositivos/tests/dispositivos.controller.spec.ts
- docs/DriveHub/DISENO_BASE_DE_DATOS.md

## Fuera de alcance (no tocar)

- El registro desde el navegador (UT-CU36-FE-01 y 02).
- Un mensaje distinto por plataforma.

## Resultado esperado

`POST /api/v1/dispositivos-push` acepta `plataforma: WEB`.

## Validación

1. `pnpm test`, `lint` y `build` en verde.
