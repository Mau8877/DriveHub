# UT-CU25-FE-02 — Pantalla de planes de horas (esquema y ruta) en `pagos`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25-FE-02` |
| **Nombre** | Pantalla de planes de horas (esquema y ruta) en `pagos` |
| **Caso de uso** | CU25-FE |
| **Microservicio** | `pagos` (web) |
| **Depende de** | UT-CU25-FE-01 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (rutas del remoto, `features/{feature}/` con las mismas carpetas que en React), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md). Como modelo directo, `features/instructores/` del remoto `escuela`: su `schemas/instructores.esquema.ts`, su pantalla, su spec y la ruta de `escuela/src/app/app.routes.ts`. Leer también `projects/frontend/pagos/package.json` y `federation.config.mjs`.

## Solicitud de negocio

El Administrador gestiona los planes de horas de su escuela con la misma pantalla que ya conoce de estudiantes e instructores.

## Objetivo

Existe la pantalla "Planes de horas" completa en `/pagos/planes`: listado con búsqueda y filtro, crear, editar, desactivar y reactivar.

## Alcance

- `projects/frontend/pagos/package.json`: dependencia `"@drivehub/gestion": "workspace:*"` (el remoto la tenía solo en `skip` de la federación). Se actualiza `pnpm-lock.yaml`.
- `projects/frontend/pagos/src/app/features/planes/`:
  - `schemas/planes.esquema.ts`: `EsquemaGestion` con `recurso: 'planes-pago'`, título "Planes de horas", descripción "Planes de horas de tu escuela." y `nombreSingular: 'plan'`.
    - **Columnas:** `nombre` (principal, con el icono `receipt`), `horas` (título "Horas", `ancho: '6rem'`), `costoBs` (`tipo: 'moneda'`, `moneda: 'BOB'`, "Costo (Bs)"), `costoUsd` (`tipo: 'moneda'`, `moneda: 'USD'`, "Costo (USD)") y `activo` (insignia: `true` éxito "Activo", `false` neutro "Desactivado", `ancho: '8rem'`; igual que el booleano `activa` del esquema de preguntas del remoto `examenes`).
    - **Búsqueda** por nombre. **Filtro** `filter[activo]` con "Todos", "Activos" (`true`) y "Desactivados" (`false`).
    - **Crear** (`POST`), una sola sección: `nombre` (obligatorio, máximo 80, mensaje "El nombre es obligatorio"), `horas` (`tipo: 'numero'`, obligatorio, `min: 1`, `max: 500`, mensajes "Las horas son obligatorias" y "Las horas deben ser un número entero de 1 a 500"; `mitad`) y `costoBs` (`tipo: 'decimal'`, `decimales: 2`, obligatorio, `min: 0.01`, `max: 99999999.99`, título "Costo en bolivianos (Bs)", `mitad`, con `ayuda: 'El equivalente en dólares se calcula solo.'`).
    - **Editar** (`PATCH`): los mismos tres campos con las mismas reglas. Se declaran **una vez** y se reusan en crear y editar (misma constante).
    - **Acciones por fila:** "Desactivar" (`POST {id}/desactivar`, icono `power`, tono peligro, visible solo si `activo` es `true`, con confirmación "¿Desactivar el plan?" y un texto que dice que deja de ofrecerse pero conserva su historia) y "Reactivar" (`POST {id}/activar`, visible solo si `activo` es `false`, sin confirmación). Sin eliminar.
    - **`erroresDeCampo`:** `ERR_PAG_01` → `nombre`, con "Ya existe un plan con ese nombre en tu escuela". Sin detalle de solo lectura.
    - **Mensajes:** vacío "Todavía no hay planes en tu escuela.", sin resultados "Sin resultados.", error al guardar "No pudimos guardar el plan. Inténtalo de nuevo más tarde."
  - `screens/planes-gestion.ts`: componente delgado con `CUSTOM_ELEMENTS_SCHEMA`, `import '@drivehub/gestion'` y `<dh-gestion [esquema]="esquema">`, igual que `instructores-gestion.ts`.
  - `tests/planes.esquema.spec.ts`: recurso y título; columnas y tipos (moneda en BOB y USD, insignia de `activo`); filtro de estado con sus tres opciones; acciones y su visibilidad por estado; `erroresDeCampo`; reglas con `validar` de `@drivehub/gestion` (nombre vacío y de 81 caracteres; horas 0, 501, con decimales y vacías; costo 0, negativo, con tres decimales, con coma y con punto, y el máximo); que crear y editar usan los mismos campos; que no hay campo de dólares en el formulario.
  - `tests/planes-gestion.spec.ts`: el componente monta `<dh-gestion>` con el esquema como propiedad.
- `projects/frontend/pagos/src/app/app.routes.ts`: una hija más de la ruta raíz, `path: 'planes'` con `loadComponent` a `PlanesGestion` (una sola ruta: el drawer maneja crear y editar), junto a `Inicio`; no se toca `federation.config.mjs`.
- `app.routes.spec.ts` del remoto (créalo si no existe): `planes` carga `PlanesGestion` de forma perezosa, es una sola ruta, y `''` sigue cargando `Inicio`.
- `.claude/rules/MICROSERVICIOS.md`: en la fila del remoto `pagos`, la columna de casos de uso queda en "CU25".

Archivos esperados (crear/tocar):

- `projects/frontend/pagos/package.json` y `pnpm-lock.yaml`
- `projects/frontend/pagos/src/app/features/planes/schemas/planes.esquema.ts`
- `projects/frontend/pagos/src/app/features/planes/screens/planes-gestion.ts`
- `projects/frontend/pagos/src/app/features/planes/tests/planes.esquema.spec.ts` y `planes-gestion.spec.ts`
- `projects/frontend/pagos/src/app/app.routes.ts` y su spec
- `.claude/rules/MICROSERVICIOS.md`

## Fuera de alcance (no tocar)

- El menú del shell y el contenedor `frontend` (UT 03).
- Cambios en `@drivehub/gestion` o `@drivehub/ui` (UT 01): si falta algo, se vuelve a esa UT.
- `Inicio` del remoto (`/pagos`).
- Detalle de solo lectura, cuotas y la contratación de un plan.

## Resultado esperado

Con la ruta y el contenedor de la UT 03, el Administrador lista, busca, filtra, crea, edita, desactiva y reactiva planes de horas.

## Validación

1. `pnpm --dir projects/frontend/pagos test`, `lint` y `build` sin errores.
2. Con el stack levantado (UT 03 y backend hechos): crear un plan de 10 horas por 450 Bs lo muestra con `Bs 450,00` y `US$ 64,66`; repetir el nombre con otras mayúsculas marca el error bajo el campo; desactivarlo pide confirmación y lo deja "Desactivado"; reactivarlo no pregunta; editar el costo cambia ambas columnas.
