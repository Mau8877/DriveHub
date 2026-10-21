# UT-CU04b-FE-02 — Botón "Verificar correo" en la barra superior

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-FE-02` |
| **Nombre** | Botón "Verificar correo" en la barra superior |
| **Caso de uso** | CU04b-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU04b-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona debe ver, desde cualquier pantalla, que su correo falta por verificar y poder pedir el correo.

## Objetivo

La cabecera del shell muestra "Verificar correo" mientras el correo no está verificado.

## Alcance

- `layout/shell-layout.ts`: junto a "Avisos" y "Cerrar sesión", un `<button type="button">` "Verificar correo" visible solo si `sesion.usuario()?.correoVerificado === false`. Mismas clases de tokens que los otros botones.
- Al pulsarlo llama a `SesionService.verificarCorreo()`; mientras corre queda deshabilitado; al terminar muestra, bajo la cabecera, el aviso "Te enviamos un correo para verificar tu dirección. Revisa tu bandeja." (signal `aviso`); si falla, "No pudimos enviar el correo. Intenta más tarde.".
- Tests del componente: el botón no aparece con el correo verificado ni sin usuario; aparece sin verificar; un clic llama a `verificarCorreo` una vez y se deshabilita; muestra el aviso de éxito y el de error.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/layout/shell-layout.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.spec.ts`

## Fuera de alcance (no tocar)

- `SesionService` (UT 01).
- Mostrar la dirección de correo o un menú de usuario.
- Estilos con valores arbitrarios.

## Resultado esperado

Desde cualquier pantalla de la web se puede pedir el correo de verificación mientras falte.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Manual (con el backend de CU04b): iniciar sesión con una cuenta nueva, pulsar el botón y ver el correo en http://localhost:8025.
