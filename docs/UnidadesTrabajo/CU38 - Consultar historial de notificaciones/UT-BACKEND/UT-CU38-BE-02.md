# UT-CU38-BE-02 — Consultas y actualización de la bandeja en el repository

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU38-BE-02` |
| **Nombre** | Consultas y actualización de la bandeja en el repository |
| **Caso de uso** | CU38-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — |

## Antes de implementar

Leer [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#dynamodb) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Mirar `common/idempotencia/eventos-procesados.repository.spec.ts` como modelo de test de un repository con el cliente de DynamoDB falso.

## Solicitud de negocio

El servicio necesita leer la bandeja de una persona y marcar sus avisos como leídos.

## Objetivo

`NotificacionesRepository` (hoy solo `guardar`) sabe listar la bandeja, contar y listar las no leídas, buscar un aviso por su código público y marcarlo leído.

## Alcance

Sin lógica de negocio, solo acceso a datos. En `repositories/notificaciones.repository.ts`:

- `listarBandeja(usuarioCodigo, filtros: { leida?: boolean; tipo?: TipoNotificacion }): Promise<Notificacion[]>`: `Query` por `usuarioCodigo` con `ScanIndexForward: false` y `FilterExpression` `canal = :inApp` (más `leida` y `tipo` si vienen; nombres con `ExpressionAttributeNames`). Sigue `LastEvaluatedKey` hasta agotar.
- `buscarEnBandeja(usuarioCodigo, codigoNotificacion): Promise<Notificacion | undefined>`: la misma consulta con el filtro `codigoNotificacion = :codigo` (la clave es `sk`, no el código público).
- `contarNoLeidas(usuarioCodigo): Promise<number>`: `Query` sobre el índice `NoLeidas` con `Select: 'COUNT'`, siguiendo la paginación.
- `listarNoLeidas(usuarioCodigo): Promise<Notificacion[]>`: `Query` sobre `NoLeidas` (las filas de ese índice son solo `IN_APP` sin leer).
- `marcarLeida(usuarioCodigo, sk): Promise<void>`: `UpdateCommand` con `SET leida = :true REMOVE noLeidaTs`. Idempotente: sobre un aviso ya leído no falla.
- Tests del repository con el cliente falso: arma bien las expresiones (canal `IN_APP` siempre, filtros opcionales, orden descendente), sigue `LastEvaluatedKey`, el conteo usa el índice, y la actualización quita `noLeidaTs`.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/features/notificaciones/repositories/notificaciones.repository.ts
- projects/backend/notificaciones/src/features/notificaciones/tests/notificaciones.repository.spec.ts

## Fuera de alcance (no tocar)

- Paginar con `page`/`pageSize`, armar `meta` o decidir si un aviso existe (UT-CU38-BE-03).
- `definicion-tablas.ts`: el índice `NoLeidas` ya existe, no se cambia.
- El método `guardar` y todo lo de CU36.

## Resultado esperado

El service dispone de las cinco operaciones de lectura y actualización sobre la tabla, sin que el repository decida nada de negocio.

## Validación

1. `pnpm --dir projects/backend/notificaciones test`, `lint` y `build` en verde.
2. Contra DynamoDB local, con avisos de los tres canales de una persona, `listarBandeja` devuelve solo los `IN_APP`, del más reciente al más antiguo.
