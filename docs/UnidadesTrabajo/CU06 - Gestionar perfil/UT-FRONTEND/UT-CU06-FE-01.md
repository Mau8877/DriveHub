# UT-CU06-FE-01 — Sesión con `fotoUrl` y evento `perfil-actualizado`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-01` |
| **Nombre** | Sesión con `fotoUrl` y evento `perfil-actualizado` |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `shell` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (sesión compartida y comunicación entre remotos)

## Solicitud de negocio

La barra superior debe mostrar la foto de la persona y reflejar al instante los cambios que hace desde su perfil.

## Objetivo

`SesionService` conoce la `fotoUrl` y se actualiza cuando el remoto avisa que el perfil cambió.

## Alcance

- `core/sesion/sesion.types.ts`: `UsuarioSesion` suma `fotoUrl: string | null` (el backend la omite si no hay foto: tratar `undefined` como `null`).
- `core/sesion/sesion.service.ts`: método `actualizarPerfil(cambios: Partial<Pick<UsuarioSesion, 'nombres' | 'apellidos' | 'fotoUrl'>>)` que mezcla en `_usuario`. En el constructor, `escucharEvento('perfil-actualizado', …)` de `@drivehub/mf-contract` llama a `actualizarPerfil` con el detalle y se desuscribe con `DestroyRef`. El nombre del evento va como constante exportada (`EVENTO_PERFIL_ACTUALIZADO`) en un archivo del shell (`core/sesion/eventos.ts`).
- Tests (`sesion.service.spec.ts`): `fotoUrl` de login y renovación queda en `usuario()`; `actualizarPerfil` cambia solo los campos pasados; un evento `perfil-actualizado` actualiza el usuario; sin sesión, el evento no hace nada.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/core/sesion/sesion.types.ts`
- `projects/frontend/shell/src/app/core/sesion/sesion.service.ts`
- `projects/frontend/shell/src/app/core/sesion/eventos.ts`
- `projects/frontend/shell/src/app/core/sesion/sesion.service.spec.ts`

## Fuera de alcance (no tocar)

- El avatar y el menú (UT 02).
- Llamar al backend desde el shell para pedir el perfil: la foto llega en la sesión.
- Guardar la foto o el perfil en `localStorage`.

## Resultado esperado

La sesión del shell trae `fotoUrl` y se corrige sola cuando el remoto emite `perfil-actualizado`.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
