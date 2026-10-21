# UT-CU06-FE-07 — Sección de cambiar la contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-07` |
| **Nombre** | Sección de cambiar la contraseña |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-FE-05 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona cambia su contraseña desde su perfil, sin tener que cerrar sesión.

## Objetivo

El componente `CambiarContrasena` (flujo F3) dentro de la pantalla de perfil.

## Alcance

- `features/perfil/components/cambiar-contrasena.ts` (`app-cambiar-contrasena`): formulario con contraseña actual, nueva y confirmación (`type="password"`, `autocomplete="current-password"` / `"new-password"`).
  - Reglas de la nueva **reutilizadas del registro** (9 a 64 caracteres); la confirmación debe coincidir (error en el campo, sin enviar: A9 del CU); la nueva no puede ser igual a la actual (se avisa en el cliente, y el backend lo reafirma con `ERR_IDE_15`).
  - Envía `{ passwordActual, passwordNueva, dispositivo: 'WEB' }` a `PerfilService.cambiarContrasena`; deshabilitado mientras envía.
  - Éxito: aviso "Contraseña actualizada. Cerramos tus otras sesiones." y los campos se vacían. La sesión actual **no** se cierra ni se renueva.
  - `ERR_IDE_14` ("La contraseña actual no es correcta") bajo el primer campo; `ERR_IDE_15` y `ERR_SYS_01` con los mensajes del mapa; no se vacía lo escrito en errores.
- Se agrega a la pantalla `Perfil` (UT 05), plegado por defecto bajo un título "Cambiar contraseña".
- Tests (`tests/cambiar-contrasena.spec.ts`): no envía con campos vacíos, contraseña corta, confirmación distinta o nueva igual a la actual; envía el cuerpo con `dispositivo: 'WEB'`; éxito vacía el formulario y avisa; `ERR_IDE_14` aparece bajo la contraseña actual; nunca se muestran ni se registran las contraseñas.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/perfil/components/cambiar-contrasena.ts`
- `projects/frontend/identidad/src/app/features/perfil/screens/perfil.ts`
- `projects/frontend/identidad/src/app/features/perfil/tests/cambiar-contrasena.spec.ts`

## Fuera de alcance (no tocar)

- Recuperar la contraseña sin la actual (CU04).
- Cerrar la sesión actual o forzar un nuevo login.
- Medidor de fortaleza de contraseña.

## Resultado esperado

La persona cambia su contraseña con la actual y sigue con su sesión abierta.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. Manual (con el backend de CU06): cambiar la contraseña con la actual incorrecta (error) y con la correcta (aviso); la sesión de la web sigue, y una app móvil abierta con la misma cuenta pierde la suya.
