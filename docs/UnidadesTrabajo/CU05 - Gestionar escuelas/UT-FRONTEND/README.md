# Unidades de trabajo — CU05-FE

**Caso de uso:** Gestionar escuelas en la web (Super admin): listado con búsqueda y filtro, crear una escuela con su Administrador, editar el nombre, desactivar y reactivar, con un menú propio del Super admin.
**Micro-frontend(s):** `escuela` (UT 03 a 07) y `shell` (UT 01, 02 y 08) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU05 - Gestionar escuelas`](../../../CasosDeUso/CU05%20-%20Gestionar%20escuelas.md)
**Recurso HTTP / contrato:** pantallas `/escuelas`, `/escuelas/nueva` y `/escuelas/:codigo/editar`; consume `GET`, `POST`, `PATCH`, `DELETE /api/v1/escuelas`, `POST /api/v1/escuelas/{codigo}/reactivar` y `GET /api/v1/escuelas/por-codigo/{codigo}` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (carpetas de una feature), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. El backend ([CU05-BE](../UT-BACKEND/README.md)) hace falta solo para la validación manual de la UT 08.

## Decisiones de diseño

1. **Las pantallas viven en el remoto `escuela`** y el shell las monta en `/escuelas`, una ruta de primer nivel (el shell es dueño de ese nivel). Es un conjunto de rutas propio, `./routes-escuelas`, igual que `identidad` expone `./routes-publicas`: el remoto no carga las rutas del Administrador (`/escuela/...`) para este caso. Ambos prefijos son distintos a propósito: `/escuelas` es del Super admin, `/escuela` es del Administrador.
2. **Menús por rol, sin mezclar.** `NavItem` lleva sus roles: el Super admin ve solo "Gestionar escuelas"; el Administrador de escuela ve los ítems de siempre. Ocultar el ítem no basta: la ruta `/escuelas` se protege con `rolGuard('SUPER_ADMIN')` y el backend vuelve a decidir por rol.
3. **Rutas en español**, porque solo las páginas públicas de `identidad` van en inglés (`/register`, `/account/...`).
4. **Una feature `escuelas`** con las carpetas de la convención común de React y Angular: `api/`, `screens/`, `schemas/`, `types/`, `tests/`.
5. **Las reglas del Administrador se duplican** desde el registro del estudiante de `identidad` (contraseña de 9 caracteres, edad mínima de 18, longitudes): ninguna app importa código de otra y las reglas de un mismo campo tienen que coincidir con el backend y con las demás apps.
6. **El slug se sugiere** a partir del nombre al crear y se puede ajustar; al editar solo se muestra, porque no se cambia.
7. **Editar usa la consulta que ya existe** (`GET /escuelas/por-codigo/{codigo}`), que devuelve la escuela en cualquier estado: no hace falta un endpoint nuevo.
8. **Desactivar se confirma en la misma fila**, sin librerías de diálogos; reactivar no se confirma porque es seguro y reversible.
9. **El aviso de éxito viaja por el estado del `Router`** hacia el listado y se muestra una sola vez; no hace falta un store global.

## Rutas y pantallas

| Ruta | Pantalla | Quién |
|------|----------|-------|
| `/escuelas` | Listado: búsqueda, filtro de estado, paginación, editar, desactivar, reactivar | `SUPER_ADMIN` |
| `/escuelas/nueva` | Formulario de la escuela y de su Administrador | `SUPER_ADMIN` |
| `/escuelas/:codigo/editar` | Nombre de la escuela (slug solo lectura) | `SUPER_ADMIN` |

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU05-FE-01](./UT-CU05-FE-01.md) | Menú por rol en el shell | `shell` | — |
| [UT-CU05-FE-02](./UT-CU05-FE-02.md) | `rolGuard` en el shell | `shell` | 01 |
| [UT-CU05-FE-03](./UT-CU05-FE-03.md) | Base de la feature `escuelas`: tipos, reglas y servicio | `escuela` | — |
| [UT-CU05-FE-04](./UT-CU05-FE-04.md) | Pantalla "Gestionar escuelas" (listado) | `escuela` | 03 |
| [UT-CU05-FE-05](./UT-CU05-FE-05.md) | Pantalla "Nueva escuela" | `escuela` | 03 |
| [UT-CU05-FE-06](./UT-CU05-FE-06.md) | Pantalla "Editar escuela" | `escuela` | 03 |
| [UT-CU05-FE-07](./UT-CU05-FE-07.md) | Rutas de gestión de escuelas expuestas por el remoto | `escuela` | 04, 05, 06 |
| [UT-CU05-FE-08](./UT-CU05-FE-08.md) | Montar `/escuelas` en el shell solo para el Super admin | `shell` | 01, 02, 07 |

**Cadena:** `01 → 02`; `03 → 04, 05, 06 → 07`; `01 + 02 + 07 → 08`. Las dos cadenas (shell y remoto) son independientes hasta la 08.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01 | Listado con búsqueda, filtro y paginación | 03, 04 |
| F1, A1, A2, A3, A4, CA02 a CA06 | Crear con validación y errores por campo | 03, 05 |
| F2, A6, A8, CA08 | Editar el nombre | 03, 06 |
| F3, F4, CA09, CA10 | Desactivar y reactivar | 03, 04 |
| A7, A9, A10, CA11 | Errores de estado mostrados con claridad | 04, 06 |
| CA12 | Solo Super admin | 02, 08 |
| CA13 | Menú distinto por rol | 01, 08 |

CA07, CA09 (efecto en el login), CA14 y CA15 son de backend; el mensaje "escuela inactiva" lo muestra el login del shell, que ya presenta el mensaje del backend.

## Follow-ups (no se implementan en estas UT)

- Mostrar el correo del Administrador de cada escuela en el listado.
- Guards por rol para las rutas del Administrador (`/escuela`, `/pagos`...).
- Estadísticas por escuela (alumnos, instructores) en el listado.

## Qué NO incluyen estas UT

- El backend ([CU05-BE](../UT-BACKEND/README.md)) ni el mobile: el Super admin usa solo la web.
- Cambios en `identidad`, `pagos` ni en los remotos React.
- Editar o cambiar al Administrador.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend: `escuela` (UT 03 a 07) y `shell` (UT 01, 02 y 08). `docs/` es `[DOCS]`.
