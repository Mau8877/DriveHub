# Unidades de trabajo — CU10-FE

**Caso de uso:** Asignar auto a instructor (Administrador de escuela): pantalla "Asignación de autos" con el listado de autos y su instructor, búsqueda, filtro de asignados o sin asignar, y las acciones asignar, cambiar de instructor, liberar y ver el historial.
**Micro-frontend(s):** `escuela` (web) y `shell` (menú), más el paquete compartido `@drivehub/gestion` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU10 - Asignar auto a instructor`](../../../CasosDeUso/CU10%20-%20Asignar%20auto%20a%20instructor.md)
**Recurso HTTP / contrato:** el de [CU10-BE](../UT-BACKEND/README.md#contrato-de-datos--recurso-http) (`/api/v1/asignaciones-vehiculo`, vía el gateway); pantalla `/escuela/asignacion-autos`

## Antes de implementar cualquier UT

**Requiere CU07-FE y CU09-FE completos** (la pantalla de autos y la de instructores, con `<dh-gestion>`, `<dh-select>` y `<dh-fecha>` ya en uso). El backend (CU10-BE) puede ir en paralelo: la pantalla se prueba contra él al final.

Leer en [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (en especial "Nada de componentes nativos"), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Se reutiliza `<dh-gestion>`**: la pantalla es un esquema. Lo que el esquema no sabe expresar se **amplía en el paquete para todos** (regla de GESTION_FRONTEND.md), no con una excepción local.
2. **Faltan dos cosas en `@drivehub/gestion`**, y cada una es una UT: (a) una **acción con formulario**: hoy una acción solo puede pedir confirmación, y "Asignar" y "Cambiar instructor" necesitan elegir un instructor y una fecha; (b) un **detalle en forma de lista** para el historial, que trae un arreglo de filas y no un registro.
3. **La acción con formulario abre el mismo drawer** de crear y editar (secciones y campos del esquema), con su propio título y botón, y envía el cuerpo con el método y la ruta de la acción. Los campos reutilizan los tipos existentes (`seleccion` con `opcionesDe`, `fecha` con `porDefecto: 'hoy'` y `hastaHoy`).
4. **El instructor se elige de `instructores` filtrado por activos** (`opcionesDe` con `params: { 'filter[estado]': 'ACTIVO' }`; el cliente ya codifica los corchetes). En "Cambiar instructor" el instructor actual sigue apareciendo: si se elige el mismo, el backend responde `ERR_ESC_16` y se muestra bajo el campo.
5. **Identificador de la fila:** `idCampo: 'vehiculoCodigo'`, porque el recurso es la asignación de cada auto (ver CU10-BE, decisión 1).
6. **Visibilidad de las acciones** (`visibleSi`): "Asignar" solo en autos sin instructor y `DISPONIBLE`; "Cambiar instructor" y "Liberar" solo en autos con instructor; "Ver historial" en todos.
7. **Errores de campo:** `ERR_ESC_13`, `ERR_ESC_16` y `ERR_ESC_18` → instructor; `ERR_ESC_17`, `ERR_ESC_09` y `ERR_ESC_19` no tienen campo y salen como aviso general.
8. **Menú:** "Asignación de autos" dentro de "Gestionar escuela", debajo de "Autos", solo para `ADMIN_ESCUELA`. Una sola ruta `/escuela/asignacion-autos`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU10-FE-01](./UT-CU10-FE-01.md) | Acciones con formulario en `<dh-gestion>` | — |
| [UT-CU10-FE-02](./UT-CU10-FE-02.md) | Detalle en forma de lista en `<dh-gestion>` | — |
| [UT-CU10-FE-03](./UT-CU10-FE-03.md) | Pantalla y ruta de asignación de autos en `escuela` | UT-CU10-FE-01, UT-CU10-FE-02 |
| [UT-CU10-FE-04](./UT-CU10-FE-04.md) | Opción "Asignación de autos" en el menú del shell | UT-CU10-FE-03 |

**Cadena:** `01` y `02` en paralelo → `03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Listar (CA01 a CA03) | Tabla con instructor y fecha, búsqueda, filtro y vacío | 03 |
| F1, F2 (CA04 a CA08) | Asignar y cambiar con formulario; errores bajo el campo | 01, 03 |
| F3 (CA10) | Liberar con confirmación | 03 |
| F4 (CA13) | Historial del auto | 02, 03 |
| Menú (CA15) | Opción visible solo para el Administrador de escuela | 04 |

Los criterios CA09, CA11, CA12, CA14, CA16 y CA17 son del backend ([CU10-BE](../UT-BACKEND/README.md#cobertura-del-caso-de-uso)).

## Follow-ups (no se implementan en estas UT)

- Mostrar el instructor asignado en la pantalla de autos (CU07).
- Aviso visual cuando un auto asignado está en mantenimiento o de baja.

## Qué NO incluyen estas UT

- Cambios en el backend (CU10-BE).
- El auto del instructor en la app móvil, y la disponibilidad (CU11).
- Asignar estudiantes a instructores.

## Commits sugeridos

Un commit por proyecto: `gestion` (UT 01 y 02), `escuela` (UT 03) y `shell` (UT 04); los documentos de esta carpeta, aparte, con `[DOCS]`.
