# UT-CU06-FE-09 — Página pública `change-email`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-09` |
| **Nombre** | Página pública `change-email` |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Tomar como modelo la página existente `features/cuenta/screens/verificar-correo.ts`.

## Solicitud de negocio

Al abrir el enlace del correo nuevo, en cualquier navegador, la persona ve que el cambio se completó o por qué no.

## Objetivo

La ruta pública `/account/change-email?token=…` confirma el enlace e informa el resultado.

## Alcance

- `features/cuenta/screens/cambiar-correo.ts` (componente `CambiarCorreoConfirmacion`): lee `token` de la query; sin token, muestra "El enlace no es válido" sin llamar a la API; con token llama a `CuentaService.confirmarCambioCorreo(token)` (método nuevo: `POST /auth/cambiar-correo/confirmar` con `{ token }`, público).
  - Éxito: "Tu correo se cambió correctamente" y un enlace "Ir a la aplicación" (`/`).
  - `ERR_IDE_11`: "El enlace no es válido, venció o ya se usó. Pide uno nuevo desde tu perfil." con enlace a `/perfil` (el guard lo lleva al login si hace falta).
  - Cualquier otro error: mensaje genérico con reintento.
  - Funciona con o sin sesión y no depende del shell.
- `routes-cuenta.ts`: `{ path: 'change-email', loadComponent: … }` junto a `verify-email`.
- `features/cuenta/api/cuenta.service.ts`: `confirmarCambioCorreo(token)`.
- Tests (`tests/cambiar-correo.spec.ts` y `tests/cuenta.service.spec.ts`): confirma con el token de la URL; muestra éxito; `ERR_IDE_11` muestra el mensaje de enlace inválido; sin token no llama a la API; la ruta está en `routes-cuenta`.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/cuenta/screens/cambiar-correo.ts`
- `projects/frontend/identidad/src/app/features/cuenta/api/cuenta.service.ts`
- `projects/frontend/identidad/src/app/routes-cuenta.ts`
- `projects/frontend/identidad/src/app/features/cuenta/tests/cambiar-correo.spec.ts`
- `projects/frontend/identidad/src/app/features/cuenta/tests/cuenta.service.spec.ts`

## Fuera de alcance (no tocar)

- Montar `/account` en el shell: ya está montado por CU04b.
- La sección que pide el cambio (UT 08).
- Iniciar sesión automáticamente tras confirmar.

## Resultado esperado

El enlace del correo nuevo abre una página pública que confirma el cambio o explica que el enlace no sirve.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. Manual (con el backend de CU06): abrir el enlace del correo de Mailpit en otro navegador; repetirlo y ver el mensaje de enlace usado.
