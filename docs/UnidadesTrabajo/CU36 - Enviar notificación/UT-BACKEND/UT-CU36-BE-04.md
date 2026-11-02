# UT-CU36-BE-04 — Preferencias (lectura), política de envío e idempotencia

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-BE-04` |
| **Nombre** | Preferencias (lectura), política de envío e idempotencia |
| **Caso de uso** | CU36-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md) y [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#dynamodb).

## Solicitud de negocio

Hay que respetar lo que la persona eligió y no avisarle dos veces de lo mismo.

## Objetivo

El servicio sabe leer las preferencias, decidir qué canales corresponden a un aviso y marcar un pedido como procesado.

## Alcance

- `features/preferencias/repositories/preferencias.repository.ts`: **solo** `obtener(usuarioCodigo)` (devuelve `PreferenciasNotificacion | null`). CU37 agrega la escritura.
- `features/notificaciones/services/politica-envio.ts` (función pura): `decidirCanales(preferencias | null, tipo, ahora)` devuelve `{ inApp, email, push }` más el correo a usar. Reglas: sin preferencias, todo activo; tipo desactivado, nada; push apagado dentro de la franja `silencioDesde`–`silencioHasta` (`HH:mm`, hora `America/La_Paz`, puede cruzar la medianoche; sin ambos extremos no hay silencio).
- `common/idempotencia/eventos-procesados.repository.ts`: `registrar(eventoCodigo)` con escritura condicional `attribute_not_exists` (devuelve `false` si ya estaba) con `ttl` de 7 días, y `liberar(eventoCodigo)`. `idempotencia.module.ts` que lo exporta.
- Tests: la política (por defecto, tipo apagado, canal apagado, silencio normal y que cruza medianoche, límites de la franja, correo del pedido vs. de las preferencias) y el repositorio de eventos con el cliente mockeado (nuevo, duplicado, error distinto se propaga).

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/preferencias/repositories/preferencias.repository.ts
- projects/backend/notificaciones/src/features/preferencias/preferencias.module.ts
- projects/backend/notificaciones/src/features/notificaciones/services/politica-envio.ts (+ `.spec.ts`)
- projects/backend/notificaciones/src/common/idempotencia/eventos-procesados.repository.ts (+ `.spec.ts`)
- projects/backend/notificaciones/src/common/idempotencia/idempotencia.module.ts

## Fuera de alcance (no tocar)

- Crear o editar preferencias, su controller y su validación (CU37).
- El model de preferencias (ya existe).

## Resultado esperado

`decidirCanales` responde siempre lo mismo para las mismas entradas y el segundo `registrar` del mismo evento devuelve `false`.

## Validación

1. `pnpm test` y `lint` en verde.
