# UT-CU10-FE-03 — Pantalla y ruta de asignación de autos en `escuela`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU10-FE-03` |
| **Nombre** | Pantalla y ruta de asignación de autos en `escuela` |
| **Caso de uso** | CU10-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU10-FE-01, UT-CU10-FE-02 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) y [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md) (carpetas por feature, tests en `tests/`). Como referencia, `features/autos/` y `features/instructores/`.

## Solicitud de negocio

El Administrador decide desde su panel qué instructor usa cada auto, sin llamar a la API a mano.

## Objetivo

La pantalla `/escuela/asignacion-autos` lista los autos con su instructor y permite asignar, cambiar, liberar y ver el historial.

## Alcance

- `features/asignacion-autos/schemas/asignacion-autos.esquema.ts` (`EsquemaGestion`): recurso `asignaciones-vehiculo`, `idCampo: 'vehiculoCodigo'`, sin `crear` ni `editar`.
  - Columnas: placa (principal), marca y modelo, estado del auto (insignia), instructor (`instructor.nombreCompleto`, "Sin asignar" si no hay) y desde (`fechaInicio`).
  - Búsqueda por placa, marca, modelo o instructor; filtro `filter[asignacion]` con "Todos", "Asignados" y "Sin asignar".
  - Acciones con `formulario` (UT-01): `asignar` (`{id}/asignar`, POST) y `cambiar-instructor`, con un campo `seleccion` de instructor (`opcionesDe`: recurso `instructores`, `params: { 'filter[estado]': 'ACTIVO' }`, valor `codigo`, etiqueta `nombreCompleto`) y un campo `fecha` (hoy por defecto, `hastaHoy`); y `liberar` con `confirmar` y `tono: 'peligro'`. Cada una con su `visibleSi` según la decisión 6 del README.
  - `detalle` con `lista` (UT-02): `cargar: '{id}/historial?pageSize=100'`, etiqueta "Ver historial", columnas instructor, desde y hasta ("Vigente").
  - `erroresDeCampo`: `ERR_ESC_13`, `ERR_ESC_16` y `ERR_ESC_18` bajo el campo instructor.
  - Mensajes de vacío y de sin resultados en español.
- `features/asignacion-autos/screens/asignacion-autos-gestion.ts`: anfitrión delgado de `<dh-gestion>`, con `CUSTOM_ELEMENTS_SCHEMA` e `import '@drivehub/gestion'`.
- `app.routes.ts`: ruta hija `asignacion-autos` con `loadComponent`.
- Tests: `asignacion-autos.esquema.spec.ts` en `tests/` de la feature (recurso, filtros, acciones y su `visibleSi`, errores de campo y reglas de los campos, con `validar` de `@drivehub/gestion`), `asignacion-autos-gestion.spec.ts` y el caso nuevo de `app.routes.spec.ts`.

Archivos esperados (crear/tocar): los tres de `features/asignacion-autos/`, `app.routes.ts` y `app.routes.spec.ts`.

## Fuera de alcance (no tocar)

- `federation.config.mjs` de las apps Angular y del shell.
- El menú del shell (UT-04) y la app móvil.
- La pantalla de autos de CU07 (no se le agrega el instructor).

## Resultado esperado

Con el stack levantado, el Administrador entra a Escuela → Asignación de autos y completa el ciclo asignar, cambiar de instructor, ver el historial y liberar.

## Validación

1. `pnpm --dir projects/frontend/escuela test`, `lint` y `build` en verde.
2. En el navegador, como `adminexample@gmail.com`: asignar un auto disponible a un instructor activo (la acción "Asignar" desaparece de esa fila), cambiarlo a otro, ver el historial con las dos asignaciones y liberarlo.
3. Un auto en mantenimiento no ofrece "Asignar".
