# UT-CU04-FE-01 — Página "Recuperar contraseña" en el remoto `identidad`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-FE-01` |
| **Nombre** | Página "Recuperar contraseña" en el remoto `identidad` |
| **Caso de uso** | CU04-FE |
| **Microservicio** | `identidad` (web) |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Se apoya en `routes-cuenta` y `CuentaService` que crea [CU04b](../../CU04b%20-%20Verificar%20correo/UT-FRONTEND/README.md) (UT FE-03 y FE-04).

## Solicitud de negocio

La persona que olvidó su contraseña necesita un lugar para pedir el enlace, con una advertencia clara sobre el correo verificado.

## Objetivo

Existe la página pública `/account/forgot-password`.

## Alcance

- `features/cuenta/cuenta.service.ts`: `solicitarRecuperacion(email)` → `POST /auth/recuperar`.
- `features/cuenta/pages/recuperar.ts`: Reactive Form tipado con un campo de correo (obligatorio y formato de correo, mensajes en español bajo el campo). Al enviar, deshabilita el botón y muestra siempre el mismo mensaje: "Si el correo está registrado y verificado, te enviamos un enlace. Revisa tu bandeja." Un texto fijo bajo el formulario aclara: "La recuperación solo funciona si verificaste tu correo desde la aplicación." y un enlace "Volver a iniciar sesión" a `/login`. Un error de red muestra "No pudimos procesar la solicitud. Intenta más tarde."
- Ruta `recuperar` en `routes-cuenta.ts`.
- Tests: el formulario no envía con el correo vacío o inválido; con uno válido llama al servicio y muestra el mensaje fijo; el mensaje es el mismo aunque el backend responda distinto (200 siempre); el error de red muestra el otro mensaje.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/cuenta/cuenta.service.ts`
- `projects/frontend/identidad/src/app/features/cuenta/cuenta.service.spec.ts`
- `projects/frontend/identidad/src/app/features/cuenta/pages/recuperar.ts`
- `projects/frontend/identidad/src/app/features/cuenta/pages/recuperar.spec.ts`
- `projects/frontend/identidad/src/app/routes-cuenta.ts`

## Fuera de alcance (no tocar)

- La página de nueva contraseña (UT 02) y el enlace desde el login (UT 03).
- Mostrar si la cuenta existe o está verificada.

## Resultado esperado

La página `/account/forgot-password` pide el correo y siempre responde lo mismo.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
