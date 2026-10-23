# Unidades de trabajo — CU08-FE

**Caso de uso:** Gestionar estudiantes en la web (Administrador de escuela): listado con búsqueda por nombre y filtro de estado, detalle de solo lectura, alta de un estudiante y edición de su categoría y su ubicación, con su opción en el menú.
**Micro-frontend(s):** `@drivehub/gestion` (UT 01), `escuela` (UT 02 y 03) y `shell` (UT 04) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU08 - Gestionar estudiantes`](../../../CasosDeUso/CU08%20-%20Gestionar%20estudiantes.md)
**Recurso HTTP / contrato:** pantalla `/escuela/estudiantes` (listado, detalle, alta y edición viven dentro de `<dh-gestion>`); consume `GET`, `POST /api/v1/estudiantes`, `GET`, `PATCH /api/v1/estudiantes/{codigo}` y `GET /api/v1/categorias-licencia` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

**Requiere CU08a-FE** ([UT-FRONTEND de CU08a](../../CU08a%20-%20Perfil%20de%20la%20persona/UT-FRONTEND/README.md)), en particular el mapa (`<dh-mapa-ubicacion>`, UT 01) y los campos `seleccion` y `ubicacion` de `<dh-gestion>` (UT 02). Leer en [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (la pantalla es un esquema para `<dh-gestion>`), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. El backend ([CU08-BE](../UT-BACKEND/README.md)) hace falta solo para la validación manual de la UT 04.

## Decisiones de diseño

1. **Es un esquema de `<dh-gestion>`, no una pantalla a mano** (GESTION_FRONTEND.md): una feature `estudiantes` en el remoto `escuela` con `schemas/` (el esquema), `screens/` (el anfitrión delgado) y `tests/`. Sin `api/` ni `types/` propios: el componente llama a la API. La pantalla vive en el remoto `escuela` (donde está el trabajo de gestión escolar), aunque el recurso lo atienda Identidad: el cliente solo conoce el gateway.
2. **`<dh-gestion>` necesita una capacidad nueva: el detalle de solo lectura.** Hoy sabe listar, crear, editar y ejecutar acciones, pero no mostrar un registro sin formulario. Reutilizar `editar` con todo en `soloLectura` dejaría un botón "Editar" y un "Guardar" que no sirven. Se agrega `detalle` al esquema (UT 01); sirve a cualquier pantalla de solo lectura futura.
3. **El formulario de edición muestra todo pero solo deja cambiar dos cosas.** Los datos que el Administrador no puede cambiar (nombre, CI, correo) aparecen como `soloLectura` y no se envían; la categoría y la ubicación son editables y son lo único que viaja en el `PATCH`.
4. **La lista de categorías sale de la API** (`opcionesDe` sobre `categorias-licencia`: valor `sigla`, etiqueta `descripcion`), no de una lista fija en el esquema. Así una categoría nueva en el catálogo aparece sin tocar la web.
5. **Reglas de validación del alta** iguales a las del backend, la web de registro y mobile: edad mínima de 18, contraseña de 9 a 64, CI de 5 a 15 caracteres `[A-Za-z0-9-]`, longitudes. Los códigos de `erroresDeCampo` son los de `docs/errors/`.
6. **Una sola ruta** `/escuela/estudiantes`, bajo el prefijo del Administrador (`/escuela`): el detalle, el alta y la edición se abren en el panel lateral sobre el listado. Es una ruta hija de las que el remoto ya expone como `./routes`.
7. **Columnas:** nombre completo (principal, icono `user`), edad, categoría (como código), fecha de incorporación y estado (insignia). `Nro` lo pone el componente. Filtro de estado `Todos` / `Activos` / `Inactivos`. Buscador "Buscar por nombre".
8. **Sin columna ni filtro de saldo.** Pagos no existe todavía; cuando exista se amplían el esquema y el CU.
9. **El menú:** el Administrador ve un ítem propio "Estudiantes" que lleva a `/escuela/estudiantes`, además del "Escuela" de siempre. Ocultar el ítem no protege la ruta: el backend decide por rol (la protección con `rolGuard` de las rutas del Administrador es un follow-up de CU05).

## Rutas y pantallas

| Ruta | Pantalla | Quién |
|------|----------|-------|
| `/escuela/estudiantes` | `<dh-gestion>`: listado con búsqueda, filtro de estado y paginación; "Ver detalles" (solo lectura), "Nuevo estudiante" y "Editar" (categoría y ubicación) | `ADMIN_ESCUELA` |

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU08-FE-01](./UT-CU08-FE-01.md) | Detalle de solo lectura en `<dh-gestion>` | `gestion` | — |
| [UT-CU08-FE-02](./UT-CU08-FE-02.md) | Esquema y pantalla "Gestionar estudiantes" | `escuela` | 01, CU08a-FE-02 |
| [UT-CU08-FE-03](./UT-CU08-FE-03.md) | Ruta de estudiantes en el remoto `escuela` | `escuela` | 02 |
| [UT-CU08-FE-04](./UT-CU08-FE-04.md) | Ítem "Estudiantes" en el menú del Administrador | `shell` | 03 |

**Cadena:** `01 → 02 → 03 → 04`. Lineal. La 04 puede escribirse en cualquier momento, pero solo se prueba de punta a punta al final.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Listado con nombre, edad, categoría, incorporación y estado, paginado | 02, 03 |
| Flujo principal pasos 3 y 4, CA02, CA03 | Búsqueda por nombre y filtro de estado | 02 |
| A1, CA04 | Listado vacío con mensaje | 02 |
| F1, A2, CA06 | Detalle de solo lectura | 01, 02 |
| F2, A3, A4, A5, CA07 a CA09 | Alta con validación y errores bajo el campo | 02 |
| F3, CA10 | Edición de categoría y ubicación solamente | 02 |
| CA12 | Opción de menú solo para el Administrador | 04 |

CA05, CA11, CA13 y CA14 son de backend.

## Follow-ups (no se implementan en estas UT)

- **Pagos** (ver el CU): columna de monto pendiente, filtro por saldo, montos pagados y clases tomadas en el detalle.
- Filtro por categoría en el listado.
- Activar o desactivar un estudiante desde la fila.
- Guards por rol para las rutas del Administrador (`/escuela`, `/pagos`...), follow-up ya anotado en CU05.

## Qué NO incluyen estas UT

- El backend ([CU08-BE](../UT-BACKEND/README.md)) ni el mobile: el Administrador de escuela usa solo la web.
- El mapa y los campos `seleccion`/`ubicacion` del componente: son de [CU08a-FE](../../CU08a%20-%20Perfil%20de%20la%20persona/UT-FRONTEND/README.md).
- Que el Administrador cambie los datos personales de un estudiante.
- Cambios en `identidad`, `pagos` ni en los remotos React.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend o paquete: `gestion` (UT 01), `escuela` (UT 02 y 03) y `shell` (UT 04). `docs/` es `[DOCS]`.
