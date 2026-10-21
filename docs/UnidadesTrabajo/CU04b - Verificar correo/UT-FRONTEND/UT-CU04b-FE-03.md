# UT-CU04b-FE-03 — Página pública `verificar-correo` en el remoto `identidad`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-FE-03` |
| **Nombre** | Página pública `verificar-correo` en el remoto `identidad` |
| **Caso de uso** | CU04b-FE |
| **Microservicio** | `identidad` (web) |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Ver también cómo `routes-publicas` expone el registro.

## Solicitud de negocio

El enlace del correo debe llevar a una página que verifique el correo aunque la persona no tenga sesión en ese navegador.

## Objetivo

El remoto `identidad` expone `./routes-cuenta` con la página que confirma el enlace.

## Alcance

- `federation.config.mjs` de `identidad`: nuevo `exposes` `./routes-cuenta` → `./src/app/routes-cuenta.ts` (solo se agrega la entrada; `shared` no cambia).
- `routes-cuenta.ts`: rutas públicas con `provideHttpClient(withInterceptors([apiInterceptor]))`, con la ruta hija `verificar-correo` (CU04 agrega `recuperar` y `restablecer` aquí).
- `features/cuenta/api/cuenta.service.ts` (`confirmarCorreo(token)` → `POST /auth/verificar-correo/confirmar`) y `features/cuenta/screens/verificar-correo.ts`: lee `token` del query string al iniciar, confirma, y muestra tres estados: verificando, "Tu correo quedó verificado" con un enlace a `/login`, y error (si `ERR_IDE_11` o falta el token: "Este enlace no es válido o ya venció. Inicia sesión y vuelve a pedir la verificación desde la barra superior.").
- Tests: el servicio llama al endpoint; la página muestra cada estado; sin `token` en la URL muestra el error sin llamar al backend.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/federation.config.mjs`
- `projects/frontend/identidad/src/app/routes-cuenta.ts`
- `projects/frontend/identidad/src/app/features/cuenta/api/cuenta.service.ts`
- `projects/frontend/identidad/src/app/features/cuenta/screens/verificar-correo.ts`
- `projects/frontend/identidad/src/app/features/cuenta/tests/cuenta.service.spec.ts`
- `projects/frontend/identidad/src/app/features/cuenta/tests/verificar-correo.spec.ts`

## Fuera de alcance (no tocar)

- Montar la ruta en el shell (UT 04).
- Páginas de recuperar y restablecer contraseña (CU04).
- Cambios de versiones o `shared` en `federation.config.mjs`.

## Resultado esperado

Existe la página que confirma el enlace de verificación, lista para que el shell la monte.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
