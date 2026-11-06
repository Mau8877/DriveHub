# UT-CU48-FE-01 — Página para fijar la contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU48-FE-01` |
| **Nombre** | Página para fijar la contraseña |
| **Caso de uso** | CU48-FE |
| **Microservicio** | `identidad` (micro-frontend Angular) |
| **Depende de** | — |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (estructura de feature, Reactive Forms, mensajes en español), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (`dh-auth-layout`, clases `dh-*`, nada de controles nativos) y las features públicas existentes de `identidad` (`registro`, restablecer contraseña) como modelo.

## Solicitud de negocio

La persona que se registró por WhatsApp necesita un lugar seguro, fuera del chat, para elegir su contraseña.

## Objetivo

Existe `/account/set-password?token=...`, que envía la contraseña y muestra el resultado.

## Alcance

- Feature `features/registro-whatsapp/` en `projects/frontend/identidad/src/app/`:
  - `api/registro-whatsapp.service.ts`: `completar(token, password)` con `HttpClient` hacia `/api/v1/registros-whatsapp/completar` (por el interceptor del remoto; sin token de sesión).
  - `screens/establecer-password.ts`: lee `token` de la URL, formulario reactivo con contraseña y confirmación (mínimo 9 caracteres, ver/ocultar con el patrón de la pantalla de registro), mensajes de error en español inline, y los estados éxito / enlace inválido.
  - `schemas/`: reglas de la contraseña **iguales** a las de registro; `types/` con los DTO.
  - `tests/`: especificación de la pantalla y del servicio.
- Ruta pública `account/set-password` en las rutas públicas expuestas por el remoto (`routes-publicas.ts`) y, si hace falta, sin envolver en `<dh-shell>` igual que `/account/*`.
- Un enlace sin `token` en la URL muestra directamente el estado de enlace inválido, sin llamar a la API.
- El `token` no se guarda en `localStorage` ni se muestra en pantalla.

Archivos esperados (crear/tocar): `features/registro-whatsapp/**`, `routes-publicas.ts` y sus tests.

## Fuera de alcance (no tocar)

- El shell, los demás remotos y `@drivehub/ui`; el registro normal.

## Resultado esperado

Abrir el enlace del chat, escribir la contraseña dos veces y ver «Tu cuenta está lista» con la indicación de volver a WhatsApp.

## Validación

1. `pnpm --dir projects/frontend/identidad test`, `lint` y `build`.
2. Con el stack levantado: abrir un enlace válido, contraseña de 8 caracteres → error de longitud; de 9 o más → éxito; abrir el mismo enlace otra vez → estado de enlace inválido.
