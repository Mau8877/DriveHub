# UT-CU04-FE-02 — Página "Restablecer contraseña" en el remoto `identidad`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-FE-02` |
| **Nombre** | Página "Restablecer contraseña" en el remoto `identidad` |
| **Caso de uso** | CU04-FE |
| **Microservicio** | `identidad` (web) |
| **Depende de** | UT-CU04-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Se apoya en `routes-cuenta` y `CuentaService` que crea [CU04b](../../CU04b%20-%20Verificar%20correo/UT-FRONTEND/README.md) (UT FE-03 y FE-04).

## Solicitud de negocio

El enlace del correo debe llevar a una página donde elegir la contraseña nueva, también desde un teléfono.

## Objetivo

Existe la página pública `/account/reset-password?token=…`.

## Alcance

- `cuenta.service.ts`: `restablecer(token, password)` → `POST /auth/restablecer`.
- `features/cuenta/pages/restablecer.ts`: lee `token` del query string (sin token, muestra directamente el estado de enlace inválido). Reactive Form con contraseña nueva (mínimo 9 caracteres, **la misma regla que `registro.reglas.ts`**; se reutiliza ese módulo del remoto si ya exporta la regla) y confirmación; si no coinciden, error bajo el campo sin enviar. Éxito: "Tu contraseña se actualizó. Ya puedes iniciar sesión." con enlace a `/login`. `ERR_IDE_11`: "Este enlace no es válido o ya venció." con enlace a `/account/forgot-password`. `ERR_SYS_01`: muestra el mensaje del campo.
- Ruta `restablecer` en `routes-cuenta.ts`.
- Tests: sin `token` muestra el error sin llamar al backend; contraseña corta y confirmación distinta no envían; envío correcto llama al servicio y muestra el éxito; `ERR_IDE_11` muestra el enlace a pedir otro; ni la contraseña ni el token se guardan en `localStorage` ni se escriben en la consola.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/cuenta/cuenta.service.ts`
- `projects/frontend/identidad/src/app/features/cuenta/pages/restablecer.ts`
- `projects/frontend/identidad/src/app/features/cuenta/pages/restablecer.spec.ts`
- `projects/frontend/identidad/src/app/routes-cuenta.ts`

## Fuera de alcance (no tocar)

- Iniciar sesión automáticamente al terminar.
- Medidor de fortaleza de la contraseña o reglas distintas a las del registro.

## Resultado esperado

La página `/account/reset-password` cambia la contraseña con un enlace válido y explica los demás casos.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. Manual (con el backend de CU04): abrir el enlace del correo en Mailpit, cambiar la contraseña e iniciar sesión con la nueva.
