# UT-CU03-FE-02 — Botón "Cerrar sesión" en el layout

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-FE-02` |
| **Nombre** | Botón "Cerrar sesión" en el layout |
| **Caso de uso** | CU03-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU03-FE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona necesita un lugar visible desde cualquier pantalla para salir.

## Objetivo

El layout del shell tiene un botón "Cerrar sesión" que ejecuta `SesionService.cerrarSesion()`.

## Alcance

- `layout/shell-layout.ts`: inyectar `SesionService` y agregar en la cabecera, junto a "Avisos", un `<button type="button">` "Cerrar sesión" con las mismas clases de tokens que el botón de avisos. Mientras corre el cierre, queda deshabilitado (signal `cerrando`) para evitar doble clic.
- Test del componente (`shell-layout.spec.ts`, el existente o uno nuevo si no hay): al hacer clic llama a `cerrarSesion` una vez; el botón se deshabilita mientras dura.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/layout/shell-layout.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.spec.ts`

## Fuera de alcance (no tocar)

- Nombre del usuario, menú desplegable o confirmación previa.
- `SesionService` (UT 01).
- Estilos con valores arbitrarios o colores propios.

## Resultado esperado

Desde cualquier pantalla de la web se puede cerrar sesión con un clic.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Manual (con el backend de CU03): iniciar sesión, pulsar "Cerrar sesión" → llega a `/login`; recargar `/` → vuelve a `/login` sin recuperar la sesión.
