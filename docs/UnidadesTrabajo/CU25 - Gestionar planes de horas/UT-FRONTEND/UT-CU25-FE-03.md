# UT-CU25-FE-03 — Menú del shell y `pagos` en el contenedor `frontend`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-FE-03` |
| **Nombre** | Menú del shell y `pagos` en el contenedor `frontend` |
| **Caso de uso** | CU25-FE |
| **Microservicio** | `shell` y `docker-compose.yml` |
| **Depende de** | UT-CU25-FE-02 |

## Antes de implementar

[ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (el menú lo dibuja `<dh-sidebar>`; el shell solo decide los ítems). Leer `projects/frontend/shell/src/app/layout/nav-items.ts` y sus pruebas, `features/inicio/screens/inicio.ts` y el servicio `frontend` de `docker-compose.yml` (su comentario explica cómo se suma un remoto).

## Solicitud de negocio

El Administrador de escuela encuentra "Planes de horas" dentro de "Gestionar pagos", y la pantalla carga de verdad cuando se levanta el proyecto con Docker.

## Objetivo

El menú ofrece "Planes de horas" (solo al Administrador de escuela) y el servidor de desarrollo del remoto `pagos` corre en el contenedor.

## Alcance

- `nav-items.ts`: la opción de `Gestionar pagos` pasa de `{ ruta: '/pagos', etiqueta: 'Plan de pagos y cuotas' }` a `{ ruta: '/pagos/planes', etiqueta: 'Planes de horas', icono: 'receipt', roles: ['ADMIN_ESCUELA'] }`.
- Las migas ya resuelven la ruta por prefijo: `/pagos/planes` muestra "Inicio › Gestionar pagos › Planes de horas". La tarjeta de `Inicio` sale sola (una por pantalla del menú, con "Gestionar pagos" como descripción).
- `docker-compose.yml`, servicio `frontend`: se suma `--filter pagos` al `pnpm install` y a `pnpm ... run dev` (el volumen `frontend_nm_pagos` y el puerto 4203 ya están), y el comentario del servicio pasa a listar los remotos que corren hoy. El manifiesto `shell/public/federation.manifest.json` ya trae `pagos` en el puerto 4203. Con seis servidores de desarrollo el contenedor superaba su límite de memoria y el kernel lo mataba (`OOMKilled`): `mem_limit` sube de `2432m` a `3072m`, con un comentario de por qué.
- Pruebas: en `nav-items.spec.ts`, la opción del grupo "Gestionar pagos" es "Planes de horas" con la ruta `/pagos/planes`, la ve solo `ADMIN_ESCUELA`, y `migasDeRuta('/pagos/planes', items)` da `Inicio › Gestionar pagos › Planes de horas`; en `shell-layout.spec.ts` y `inicio.spec.ts`, las opciones y tarjetas esperadas reflejan el cambio (la tarjeta enlaza a `/pagos/planes`).

Archivos esperados (crear/tocar):

- `projects/frontend/shell/src/app/layout/nav-items.ts` y `nav-items.spec.ts`
- `projects/frontend/shell/src/app/layout/shell-layout.spec.ts` y `projects/frontend/shell/src/app/features/inicio/tests/inicio.spec.ts`
- `docker-compose.yml`

## Fuera de alcance (no tocar)

- Otras opciones del menú.
- El remoto `pagos` (UT 02) y cualquier otra app o la configuración de federación.
- Permisos de la ruta en el servidor: los decide el backend.
- Iconos nuevos en `@drivehub/ui` (`receipt` ya existe).

## Resultado esperado

El Administrador de escuela entra a los planes de horas desde el menú y la pantalla se ve dentro del shell al levantar el stack.

## Validación

1. `pnpm --dir projects/frontend/shell test`, `lint` y `build` sin errores.
2. Con `docker compose up` y `adminexample@gmail.com`: "Gestionar pagos › Planes de horas" abre `/pagos/planes` con las migas correctas; como Super admin no aparece.
3. `docker compose logs frontend` muestra el servidor de `pagos` escuchando en el puerto 4203, y `docker inspect drivehub-frontend` no reporta `OOMKilled`.
