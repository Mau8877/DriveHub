# UT-CU02-FE-01 — Contrato `renovarSesion` en `@drivehub/mf-contract`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-FE-01` |
| **Nombre** | Contrato `renovarSesion` en `@drivehub/mf-contract` |
| **Caso de uso** | CU02-FE |
| **Microservicio** | `packages/mf-contract` (paquete compartido) |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

Los remotos nunca inician ni renuevan sesión por su cuenta; cuando una petición recibe un rechazo por sesión vencida, tienen que pedirle al shell que la renueve.

## Objetivo

`ContextoShell` expone `renovarSesion()` y todos los remotos (Angular y React) lo reciben.

## Alcance

- `index.d.ts`: agregar a `ContextoShell` el método `renovarSesion(): Promise<string | null>` con su documentación: renueva la sesión (una sola renovación a la vez, aunque la pidan varios remotos), devuelve el access token nuevo o `null` si no se pudo, y en ese caso el shell ya llevó a la persona al inicio de sesión. `MountProps` lo hereda sin cambios.
- `index.js`: sin cambios de código (el contexto se guarda tal cual en `globalThis`); actualizar el comentario del archivo si hace falta.
- Actualizar los tests o ayudantes que arman un `ContextoShell` a mano para que incluyan el método nuevo.
- **Orden:** al ser un miembro requerido, el build del shell queda roto hasta aplicar UT-CU02-FE-04, que lo registra; ejecutar la 01 y la 04 seguidas.

Archivos esperados (crear/tocar):

- `packages/mf-contract/index.d.ts`
- `packages/mf-contract/index.js` (solo comentario)
- specs que construyan un `ContextoShell`

## Fuera de alcance (no tocar)

- Implementar la renovación (UT 03 y 04).
- Un método de cierre de sesión (CU03).
- Cambiar `obtenerToken` o `navegar`.

## Resultado esperado

El contrato tipa `renovarSesion()` para el shell, los remotos Angular y los remotos React.

## Validación

1. Tras aplicar la UT 04, `pnpm --dir projects/frontend/shell build` y el `build` de un remoto Angular y de uno React pasan.
2. `pnpm lint` de las apps que consumen el contrato sigue pasando.
