# UT-CU32-FE-02 — Esquema, pantalla y ruta de manuales

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU32-FE-02` |
| **Nombre** | Esquema, pantalla y ruta de manuales |
| **Caso de uso** | CU32-FE |
| **Microservicio** | `contenido` (web, React) |
| **Depende de** | UT-CU32-FE-01, UT-CU32-BE-05 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (estructura de un remoto React, tests de feature en `tests/`).

## Solicitud de negocio

El Administrador gestiona los manuales desde su panel, sin tocar la API.

## Objetivo

La pantalla `/contenido` lista, crea, edita, publica y archiva manuales.

## Alcance

- `package.json`: dependencia `@drivehub/gestion` (`workspace:*`) y `pnpm-lock.yaml`.
- `src/features/manuales/schemas/manuales.esquema.ts`: recurso `manuales`; columnas (con el PDF como enlace), búsqueda y filtros de estado y categoría; formularios de creación (PDF obligatorio) y edición (PDF opcional, `bloqueo` si está archivado); acciones `publicar`, `publicar-de-nuevo` y `archivar` (esta con confirmación) con su `visibleSi`; `ERR_CON_03` bajo el PDF.
- `src/features/manuales/screens/ManualesGestion.tsx` (anfitrión delgado) e `index.ts`; `src/dh-gestion.d.ts`; `src/routes/index.tsx` apunta a la pantalla.
- `docker-compose.yml`: el contenedor `frontend` instala y corre también `contenido`.
- Tests: esquema (reglas, acciones, enlaces), pantalla, `mount.test.tsx` actualizado y `matchMedia` simulado en `setupTests.ts`.

Archivos esperados (crear/tocar): los de `features/manuales/`, `routes/index.tsx`, `dh-gestion.d.ts`, `setupTests.ts`, `mount.test.tsx`, `package.json`, `pnpm-lock.yaml`, `docker-compose.yml`.

## Fuera de alcance (no tocar)

- `@module-federation/vite` (`federation.config`) y el shell.
- Otras pantallas del remoto.

## Resultado esperado

Con el stack levantado, el Administrador entra a Contenido, sube un PDF, crea un manual, lo publica, lo edita con PDF nuevo y lo archiva.

## Validación

1. `pnpm --dir projects/frontend/contenido test`, `lint` y `build` en verde.
2. En el navegador, como `adminexample@gmail.com`: ver los seis manuales de ejemplo, abrir un PDF desde "Ver PDF", crear un manual con un PDF propio, publicarlo, reemplazar su PDF (la versión pasa a 2) y archivarlo (el botón Editar desaparece).
