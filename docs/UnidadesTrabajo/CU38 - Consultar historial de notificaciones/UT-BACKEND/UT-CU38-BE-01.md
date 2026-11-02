# UT-CU38-BE-01 — Código de error `ERR_NOT_04`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU38-BE-01` |
| **Nombre** | Código de error `ERR_NOT_04` |
| **Caso de uso** | CU38-BE |
| **Microservicio** | `notificaciones` |
| **Depende de** | — |

## Antes de implementar

Leer [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md) y [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md).

## Solicitud de negocio

Un aviso que no existe o es de otra persona debe responder con un error catalogado, igual para los dos casos.

## Objetivo

El catálogo del servicio tiene la constante `AVISO_NO_ENCONTRADO` (`ERR_NOT_04`).

## Alcance

- `common/exceptions/error-codes.ts`: `AVISO_NO_ENCONTRADO: 'ERR_NOT_04'`, después de `FRANJA_SILENCIO_INVALIDA`.
- La documentación del código ya quedó escrita con el CU: sección en [`docs/errors/notificaciones/ERRORES_NOTIFICACIONES.md`](../../../errors/notificaciones/ERRORES_NOTIFICACIONES.md) y su link en el [índice](../../../errors/README.md). Esta UT solo verifica que coincidan con la constante.

Archivos esperados (crear/tocar):

- projects/backend/notificaciones/src/common/exceptions/error-codes.ts

## Fuera de alcance (no tocar)

- Lanzar el error (UT-CU38-BE-03).
- Cualquier otro código nuevo.

## Resultado esperado

`ErrorCodes.AVISO_NO_ENCONTRADO` existe y su número coincide con el catálogo de `docs/errors/`.

## Validación

1. `pnpm --dir projects/backend/notificaciones build` en verde.
2. `ERR_NOT_04` aparece una sola vez en el código y en `docs/errors/`, con su link en el índice.
