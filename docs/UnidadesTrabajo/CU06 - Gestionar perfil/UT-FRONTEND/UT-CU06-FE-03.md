# UT-CU06-FE-03 — Ruta `/perfil` en el shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-03` |
| **Nombre** | Ruta `/perfil` en el shell |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU06-FE-04 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

"Editar perfil" tiene que abrir la pantalla del remoto `identidad` dentro del layout, para cualquier rol.

## Objetivo

El shell monta `/perfil` con las rutas que expone `identidad` como `./routes-perfil`.

## Alcance

- `app.routes.ts`: dentro de los hijos del `ShellLayout` (hereda `sesionGuard`), antes del comodín:

  ```ts
  { path: 'perfil', loadChildren: () => loadRemoteModule('identidad', './routes-perfil').then((m) => m.routes) },
  ```

  Sin `rolGuard`: la ven los cuatro roles.
- `app.routes.spec.ts`: `/perfil` existe dentro del layout, exige sesión y no tiene `rolGuard`.
- No se agrega ítem al menú lateral (`nav-items.ts`): se llega desde el avatar.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/app.routes.ts`
- `projects/frontend/shell/src/app/app.routes.spec.ts`

## Fuera de alcance (no tocar)

- Lo que expone el remoto (UT 04) y el menú del avatar (UT 02).
- `federation.manifest.json` (la URL de `identidad` ya está).

## Resultado esperado

`/perfil` carga la pantalla del remoto `identidad` con la cabecera y el menú del shell.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Manual (con el remoto `identidad` corriendo): ir a `/perfil` con sesión muestra la pantalla; sin sesión lleva a `/login`.
