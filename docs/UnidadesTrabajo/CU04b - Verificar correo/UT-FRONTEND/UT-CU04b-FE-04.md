# UT-CU04b-FE-04 — Montar `/cuenta` público en el shell

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-FE-04` |
| **Nombre** | Montar `/cuenta` público en el shell |
| **Caso de uso** | CU04b-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU04b-FE-03 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md)

## Solicitud de negocio

El enlace del correo apunta a `/cuenta/verificar-correo`, que debe abrirse sin sesión y fuera del layout.

## Objetivo

El shell monta las rutas públicas del remoto `identidad` bajo `/cuenta`.

## Alcance

- `app.routes.ts`: nueva ruta `cuenta` (como `registro`, fuera de `ShellLayout` y sin `sesionGuard`) con `loadChildren: () => loadRemoteModule('identidad', './routes-cuenta').then((m) => m.routes)`. No usa `loginGuard`: se puede abrir con o sin sesión.
- Test de rutas: `/cuenta/…` no pide sesión y no pasa por el layout; el resto de las rutas conserva su guard.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/app.routes.ts`
- `projects/frontend/shell/src/app/app.routes.spec.ts`

## Fuera de alcance (no tocar)

- Páginas dentro del shell: viven en el remoto (UT 03).
- Cambios en `federation.manifest.json` (la URL del remoto no cambia).

## Resultado esperado

Abrir `/cuenta/verificar-correo?token=…` muestra la página sin pedir sesión.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Manual (con el backend de CU04b): abrir el enlace del correo en una ventana sin sesión y ver "Tu correo quedó verificado".
