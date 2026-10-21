# UT-CU06-FE-08 — Sección de cambiar el correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-08` |
| **Nombre** | Sección de cambiar el correo |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU06-FE-05 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona pide cambiar su correo, entiende que falta abrir el enlace del correo nuevo y ve el cambio al volver.

## Objetivo

El componente `CambiarCorreo` (flujo F4, parte del perfil) dentro de la pantalla de perfil.

## Alcance

- `features/perfil/components/cambiar-correo.ts` (`app-cambiar-correo`, recibe `perfil = input.required<Perfil>()` y emite `perfilCambiado`): muestra el correo actual y un formulario con contraseña actual y correo nuevo (`type="email"`, ≤ 120 caracteres, formato válido; reglas del registro).
  - Envía a `PerfilService.solicitarCambioCorreo`; deshabilitado mientras envía.
  - Éxito: mensaje "Te enviamos un correo a {correoNuevo}. Tu correo no cambia hasta que abras el enlace (vale 24 horas)." y el formulario queda bloqueado en ese estado, con un botón "Pedir otro enlace".
  - Errores: `ERR_IDE_14` bajo la contraseña; `ERR_IDE_01` ("Ese correo ya está en uso") y `ERR_IDE_18` ("Ese ya es tu correo") bajo el correo nuevo; un fallo del servidor de correo (500) como "No pudimos enviar el correo. Intenta más tarde.".
- **Ver el correo nuevo al volver (CA18):** mientras haya una solicitud pendiente, al volver a la pestaña (`visibilitychange` → visible) la pantalla vuelve a pedir `PerfilService.obtener()`; si el correo cambió, actualiza el perfil, quita el estado pendiente y avisa "Tu correo ahora es {correo}". Se limpia el listener al destruir el componente. Como la cuenta pasa a verificada, el shell renueva su sesión al volver (ya lo hace por CU04b) y el botón "Verificar correo" desaparece.
- Se agrega a la pantalla `Perfil` (UT 05), plegado por defecto bajo "Cambiar correo".
- Tests (`tests/cambiar-correo.spec.ts`): no envía con campos vacíos o un correo inválido; envía el cuerpo correcto; éxito muestra el mensaje con la dirección nueva y bloquea el formulario; cada código de error aparece bajo su campo; al volver a la pestaña con una solicitud pendiente consulta el perfil y, si el correo cambió, lo muestra y avisa; sin solicitud pendiente no consulta.

Archivos esperados (crear/tocar):

- `projects/frontend/identidad/src/app/features/perfil/components/cambiar-correo.ts`
- `projects/frontend/identidad/src/app/features/perfil/screens/perfil.ts`
- `projects/frontend/identidad/src/app/features/perfil/tests/cambiar-correo.spec.ts`

## Fuera de alcance (no tocar)

- La página que abre el enlace (UT 09).
- Cambiar el correo directamente sin confirmar.
- Verificar el correo actual (CU04b): el botón de la cabecera no cambia.

## Resultado esperado

La persona pide el cambio, ve qué falta hacer y, al confirmar el enlace y volver, ve su correo nuevo.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build` pasan.
2. Manual (con el backend de CU06): pedir el cambio, abrir el correo en http://localhost:8025, abrir el enlace en otra pestaña y volver al perfil: el correo nuevo aparece sin recargar.
