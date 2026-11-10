# UT-CU53-FE-01 — Campana con contador y pantalla de avisos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU53-FE-01` |
| **Nombre** | Campana de la cabecera con los avisos sin leer y pantalla «Avisos» |
| **Caso de uso** | CU53-FE |
| **Microservicio** | `shell` |
| **Depende de** | UT-CU53-BE-01 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (clases `dh-*`, nada de controles nativos).

## Solicitud de negocio

El Administrador tiene que ver de un vistazo si pasó algo en su escuela y leerlo sin salir del panel.

## Objetivo

La campana de la cabecera muestra los avisos sin leer y lleva a `/avisos`, donde se listan, se filtran, se abren y se marcan.

## Alcance

- `core/notificaciones/avisos.service.ts` (nuevo): `noLeidas` (signal), `refrescarConteo()`, `listar(page, soloNoLeidas)`, `leer(aviso)`, `leerTodas()`, por GraphQL.
- `features/avisos/screens/avisos.ts` (nuevo): cabecera con «Marcar todos como leídos», filtro «Todos / No leídos», lista en acordeón (abrir = marcar leído), «Ver más avisos», estados de carga, vacío y error con «Reintentar»; un `ERR_NOT_04` quita el aviso.
- `layout/shell-layout.ts`: `[avisos]` = contador para Administrador de escuela, Instructor y Estudiante (`false` para el Super admin); `dh-avisos` navega a `/avisos`; refresca el contador al navegar.
- `app.routes.ts`: ruta `avisos` con `rolGuard`. `layout/nav-items.ts`: migas de `/avisos`.
- Tests: `core/notificaciones/tests/avisos.service.spec.ts`, `features/avisos/tests/avisos.spec.ts` y los casos de la campana en `layout/shell-layout.spec.ts`.

Archivos esperados (crear/tocar): los de arriba.

## Fuera de alcance (no tocar)

- `packages/ui` (`<dh-header>` ya tiene la campana).
- Navegar desde un aviso a su pantalla.
- Preferencias de notificación en la web.

## Resultado esperado

Con avisos sin leer, la campana muestra el número; al pulsarla se abre `/avisos`, y abrir o marcar todos baja el número.

## Validación

1. `pnpm --dir projects/frontend/shell test` y `lint`: los specs nuevos en verde.
2. Manual: entrar como Administrador de escuela después de una compra; la campana muestra 1; abrir el aviso la deja en 0.
3. Manual: entrar como Super admin; no hay campana.
