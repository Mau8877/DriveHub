# Unidades de trabajo — CU09-FE

**Caso de uso:** Gestionar instructores (Administrador de escuela): pantalla "Instructores" con listado (foto, nombre, licencia, vencimiento, categorías y estado), búsqueda, filtro de estado, detalle de solo lectura, alta y edición de la ficha profesional y la ubicación.
**Micro-frontend(s):** `escuela` (web) y `shell` (menú), más los paquetes compartidos `@drivehub/ui` y `@drivehub/gestion` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU09 - Gestionar instructores`](../../../CasosDeUso/CU09%20-%20Gestionar%20instructores.md)
**Recurso HTTP / contrato:** el de [CU09-BE](../UT-BACKEND/README.md#contrato-de-datos--recurso-http) (`/api/v1/instructores`, vía el gateway); pantalla `/escuela/instructores`

## Antes de implementar cualquier UT

**Requiere CU08-FE completo** ([CU08-FE](../../CU08%20-%20Gestionar%20estudiantes/UT-FRONTEND/README.md)): la pantalla de estudiantes es el modelo directo de esta, y `<dh-gestion>`, `<dh-select>`, `<dh-fecha>`, el tipo `foto` y el selector de ubicación ya existen. El backend (CU09-BE) puede ir en paralelo: la pantalla se prueba contra él al final.

Leer en [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (en especial "Nada de componentes nativos"), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md), [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Se reutiliza `<dh-gestion>`**: la pantalla es un esquema, no una tabla hecha a mano. Lo que el esquema todavía no sabe expresar se **amplía en el paquete para todos** (regla de GESTION_FRONTEND.md), no se resuelve con una excepción local.
2. **Faltan tres cosas en los paquetes compartidos**, y cada una es una UT: (a) una lista desplegable de **selección múltiple** (`<dh-select multiple>`) para elegir las categorías, porque `<select multiple>` está prohibido; (b) en `<dh-gestion>`, el campo `seleccion` **múltiple**, cuyas opciones salen del catálogo y que envía un arreglo de siglas; (c) un tipo de columna y de dato de detalle **`etiquetas`** (una fila de insignias pequeñas, para las categorías), la regla de validación **`fechaFutura`** (el vencimiento de la licencia), **`soloSiCambia`** (en la edición, esa regla solo se aplica si el valor cambió) y el valor inicial **`porDefecto: 'hoy'`** de un campo fecha (la contratación).
3. **Las categorías se cargan del catálogo** (`/api/v1/categorias-licencia`, ya usado por estudiantes). `SIN` ("sin categoría") se excluye en esta pantalla: un instructor enseña al menos una.
4. **Dos formas del mismo formulario**: el alta pide todo (datos personales, acceso, ubicación y ficha profesional); la edición **solo muestra editables la ficha y la ubicación**, y los datos personales aparecen como solo lectura (CA13). Mismo mecanismo que usa estudiantes para el correo.
5. **El vencimiento de la licencia** del alta debe ser posterior a hoy; al editar solo se valida si el Administrador lo cambió, para que un instructor con la licencia vencida pueda pasar a `LICENCIA` o `INACTIVO` sin tener que mentir la fecha. El backend manda; esto es feedback inmediato.
6. **Estados** con insignia: `ACTIVO` verde, `LICENCIA` ámbar, `INACTIVO` gris. El filtro de estado son tres opciones más "Todos" (control segmentado, ≤ 4 opciones).
7. **Menú**: "Instructores" justo debajo de "Estudiantes", solo para `ADMIN_ESCUELA`, con el icono `road`. Una sola ruta `/escuela/instructores` (el drawer maneja crear, editar y detalle).
8. **Errores de campo**: `ERR_IDE_01` → correo, `ERR_IDE_19` → CI, `ERR_IDE_02` → fecha de nacimiento, `ERR_ESC_08` → número de licencia, `ERR_ESC_10` → categorías; los demás, aviso general.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU09-FE-01](./UT-CU09-FE-01.md) | `<dh-select multiple>` en `@drivehub/ui` | — |
| [UT-CU09-FE-02](./UT-CU09-FE-02.md) | Campo de selección múltiple en `<dh-gestion>` | UT-CU09-FE-01 |
| [UT-CU09-FE-03](./UT-CU09-FE-03.md) | Etiquetas, `fechaFutura`, `soloSiCambia` y fecha por defecto en `<dh-gestion>` | — |
| [UT-CU09-FE-04](./UT-CU09-FE-04.md) | Pantalla de gestión de instructores (esquema) | UT-CU09-FE-02, UT-CU09-FE-03 |
| [UT-CU09-FE-05](./UT-CU09-FE-05.md) | Ruta `instructores` en el remoto `escuela` | UT-CU09-FE-04 |
| [UT-CU09-FE-06](./UT-CU09-FE-06.md) | Opción "Instructores" en el menú del shell | UT-CU09-FE-05 |

**Cadena:** `01 → 02`, `03` en paralelo; ambas → `04 → 05 → 06`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Listar (CA01 a CA04) | Tabla con foto, licencia, categorías y estado; búsqueda; filtro; vacío | 03, 04 |
| Detalle (CA06) | Drawer de solo lectura | 03, 04 |
| Alta (CA07 a CA11) | Formulario con categorías múltiples, reglas de fecha y errores en su campo | 01, 02, 03, 04 |
| Edición (CA13) | Solo ficha y ubicación; datos personales en solo lectura | 03, 04 |
| Menú (CA15) | Opción visible solo para el Administrador de escuela | 05, 06 |

Los criterios CA05, CA14, CA16, CA17 y CA18 son del backend ([CU09-BE](../UT-BACKEND/README.md#cobertura-del-caso-de-uso)).

## Follow-ups (no se implementan en estas UT)

- Aviso visual en la tabla de licencias próximas a vencer o vencidas.
- Seleccionar un instructor desde otras pantallas (asignación de autos y alumnos, CU10 en adelante).
- Mover la directiva `DhValor` a un paquete si una segunda app Angular usa formularios reactivos con los componentes propios.

## Qué NO incluyen estas UT

- Cambios en el backend (CU09-BE).
- Mostrar o editar datos de otras escuelas, o el Super admin gestionando instructores.
- La app móvil.
- Asignaciones de autos o alumnos a instructores.

## Commits sugeridos

Un commit por proyecto: `ui` (UT 01), `gestion` (UT 02 y 03), `escuela` (UT 04 y 05) y `shell` (UT 06); los documentos de esta carpeta, aparte, con `[DOCS]`.
