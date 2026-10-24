# UT-CU06-FE-02 — Menú del avatar con "Editar perfil"

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-FE-02` |
| **Nombre** | Menú del avatar con "Editar perfil" |
| **Caso de uso** | CU06-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU06-FE-01 |

> **Actualización:** el menú del avatar ya no es el componente `AvatarMenu` del shell: pasó a la
> cabecera compartida `<dh-header>` de `@drivehub/ui` (menú de la cuenta con "Editar perfil" y
> "Cerrar sesión"), que usa el shell y cualquier micro-frontend. Ver
> [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md).

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md)

## Solicitud de negocio

La persona llega a su perfil desde cualquier pantalla, tocando su avatar en la barra superior.

## Objetivo

La cabecera del shell muestra el avatar de la persona; al tocarlo se abre un menú con "Editar perfil", que lleva a `/perfil`.

## Alcance

- `layout/avatar-menu.ts` (componente `AvatarMenu`, selector `app-avatar-menu`, standalone, signals):
  - Botón redondo con la foto (`<img [src]="usuario.fotoUrl" alt="">`) o, sin foto, las iniciales de nombres y apellidos sobre `bg-primary text-primary-foreground`. Si la imagen no carga (`(error)`, ej. URL vencida), cae a las iniciales.
  - Al pulsarlo se abre un menú (`role="menu"`, el botón con `aria-haspopup="menu"` y `aria-expanded`) con el nombre completo y un ítem `role="menuitem"` "Editar perfil" (`routerLink="/perfil"`).
  - Se cierra al elegir el ítem, con `Escape`, al hacer clic fuera y al perder el foco; el foco vuelve al botón.
  - Navegable con teclado (Enter/Espacio abre, Tab recorre).
  - Solo clases de tokens (`bg-popover`, `border-border`, `hover:bg-accent`…).
- `layout/shell-layout.ts`: `<app-avatar-menu />` al final de la cabecera, después de "Cerrar sesión" (que no se mueve; ver follow-ups del README). Sin usuario no se muestra.
- Tests (`avatar-menu.spec.ts` y `shell-layout.spec.ts`): iniciales sin foto; foto con foto; si la imagen falla, vuelve a iniciales; el menú está cerrado al inicio, abre con clic y teclado, cierra con `Escape` y clic fuera; "Editar perfil" navega a `/perfil`; el layout muestra el menú con usuario y no sin él.

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/layout/avatar-menu.ts`
- `projects/frontend/shell/src/app/layout/avatar-menu.spec.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.spec.ts`

## Fuera de alcance (no tocar)

- La ruta `/perfil` (UT 03) y el contenido de la pantalla.
- Mover "Cerrar sesión" al menú.
- Estilos con valores arbitrarios.

## Resultado esperado

Desde cualquier pantalla se ve el avatar y, tocándolo, se llega a "Editar perfil".

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` pasan.
2. Manual: iniciar sesión, abrir el menú del avatar con mouse y con teclado, ver el ítem "Editar perfil" y que lleva a `/perfil` (404 del shell hasta la UT 03).
