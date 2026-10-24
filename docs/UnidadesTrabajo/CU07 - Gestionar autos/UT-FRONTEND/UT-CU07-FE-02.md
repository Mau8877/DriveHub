# UT-CU07-FE-02 — Esquema, pantalla y ruta de autos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-FE-02` |
| **Nombre** | Esquema, pantalla y ruta de autos |
| **Caso de uso** | CU07-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU07-FE-01 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (carpetas por feature, tests en `tests/`).

## Solicitud de negocio

El Administrador gestiona la flota desde su panel, sin llamar a la API a mano.

## Objetivo

La pantalla `/escuela/autos` lista, registra, edita y cambia el estado de los autos.

## Alcance

- `features/autos/schemas/autos.esquema.ts`: recurso `vehiculos`; columnas, búsqueda, filtro `filter[estado]`; formulario de registro (placa, marca, modelo, año, color, transmisión, km, vencimientos) y de edición (placa y transmisión de solo lectura, `bloqueo` si está de baja); acciones `enviar-a-mantenimiento`, `habilitar` y `dar-de-baja` (esta con confirmación) con su `visibleSi`; `ERR_ESC_07` bajo el campo placa.
- `features/autos/screens/autos-gestion.ts`: anfitrión delgado de `<dh-gestion>`.
- `app.routes.ts`: ruta hija `autos` con `loadComponent`. `inicio.ts`: enlace "Autos".
- Tests: `autos.esquema.spec.ts`, `autos-gestion.spec.ts` y el de `app.spec.ts`.

Archivos esperados (crear/tocar): los tres de `features/autos/`, `app.routes.ts`, `inicio.ts`, `app.spec.ts`.

## Fuera de alcance (no tocar)

- `federation.config.mjs` de las apps Angular y el shell.
- Mobile.

## Resultado esperado

Con el stack levantado, el Administrador entra a Escuela → Autos y completa el ciclo registrar, editar, mantenimiento, habilitar y baja.

## Validación

1. `pnpm --dir projects/frontend/escuela test`, `lint` y `build` en verde.
2. En el navegador, como `adminexample@gmail.com`: registrar un auto, ver el error bajo la placa al repetirla, enviarlo a mantenimiento, habilitarlo y darlo de baja (el botón Editar desaparece).
