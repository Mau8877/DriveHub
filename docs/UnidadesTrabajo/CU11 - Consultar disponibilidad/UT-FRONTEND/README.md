# Unidades de trabajo — CU11-FE

**Caso de uso:** Consultar disponibilidad (Administrador de escuela): pantalla "Disponibilidad" con los instructores de la escuela para un día, su disponibilidad, sus franjas, el estado de sus autos, con filtros de día, momento del día, disponibilidad y autos, y búsqueda.
**Micro-frontend(s):** `escuela` (web) y `shell` (menú), más el paquete compartido `@drivehub/gestion` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU11 - Consultar disponibilidad`](../../../CasosDeUso/CU11%20-%20Consultar%20disponibilidad.md)
**Recurso HTTP / contrato:** el de [CU11-BE](../UT-BACKEND/README.md#contrato-de-datos--recurso-http) (`/api/v1/disponibilidad-instructores`, vía el gateway); pantalla `/escuela/disponibilidad`

## Antes de implementar cualquier UT

**Requiere CU10-FE completo** (la pantalla de asignación de autos, con `<dh-gestion>` y los esquemas de gestión ya en uso). El backend (CU11-BE) puede ir en paralelo: la pantalla se prueba contra él al final.

Leer en [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) (en especial "Nada de componentes nativos"), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md) y [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Se reutiliza `<dh-gestion>`**: la pantalla es un esquema de solo lectura (sin crear, editar ni acciones). Lo que el esquema no sabe expresar se **amplía en el paquete para todos** (regla de GESTION_FRONTEND.md).
2. **Falta una cosa en `@drivehub/gestion`: un filtro de fecha.** Hoy un filtro solo ofrece opciones fijas (un control segmentado o una lista). La disponibilidad necesita elegir un día. Se agrega el tipo de filtro `fecha`, que usa `<dh-fecha>` (nunca un `<input type="date">` nativo), con valor por defecto `hoy` y el calendario limitado a hoy en adelante. Es la UT 01.
3. **Una fila por instructor**, identificada por el `codigo` de su ficha (`idCampo: 'instructorCodigo'`). Las placas se dibujan como etiquetas (`tipo: 'etiquetas'`, `etiqueta: 'placa'`) y un instructor sin autos muestra "Sin auto".
4. **Insignias con el color de los tokens:** disponibilidad (`DISPONIBLE` verde, `COMPLETO` azul, `SIN_HORARIO` neutro, `NO_DISPONIBLE` ámbar) y estado de los autos (`DISPONIBLE` verde, `NO_DISPONIBLE` ámbar, `SIN_AUTO` neutro).
5. **Filtros:** día (`filter[fecha]`, hoy por defecto), momento del día (`filter[momento]`, "Todo el día" quita el filtro), disponibilidad (`filter[disponibilidad]`) y autos (`filter[auto]`). La búsqueda del componente (`search`) cubre el nombre del instructor y la placa, marca y modelo de sus autos.
6. **Menú:** "Disponibilidad" dentro de "Gestionar escuela", debajo de "Asignación de autos", solo para `ADMIN_ESCUELA`, con el icono `calendar`. Una sola ruta `/escuela/disponibilidad`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU11-FE-01](./UT-CU11-FE-01.md) | Filtro de fecha en `<dh-gestion>` | — |
| [UT-CU11-FE-02](./UT-CU11-FE-02.md) | Pantalla y ruta de disponibilidad en `escuela` | UT-CU11-FE-01 |
| [UT-CU11-FE-03](./UT-CU11-FE-03.md) | Opción "Disponibilidad" en el menú del shell | UT-CU11-FE-02 |

**Cadena:** `01 → 02 → 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal (CA01 a CA03, CA06, CA07, CA10) | Tabla con disponibilidad, franjas y autos, y el vacío | 02 |
| F1 (CA04, CA05) | Otro día y momento del día | 01, 02 |
| F2 (CA08, CA09) | Filtros por disponibilidad y por autos, y búsqueda | 02 |
| Menú (CA14) | Opción visible solo para el Administrador de escuela | 03 |

Los criterios CA11, CA12, CA13, CA15 y CA16 son del backend ([CU11-BE](../UT-BACKEND/README.md#cobertura-del-caso-de-uso)).

## Follow-ups (no se implementan en estas UT)

- Un acceso directo desde un instructor sin auto a "Asignación de autos".
- Aviso visual cuando un instructor activo no tiene horario en los próximos días.

## Qué NO incluyen estas UT

- Cambios en el backend (CU11-BE).
- Reservar, bloquear o cambiar el estado de nada desde la pantalla.
- La vista del instructor o del estudiante, ni la app móvil.

## Commits sugeridos

Un commit por proyecto: `gestion` (UT 01), `escuela` (UT 02) y `shell` (UT 03); los documentos de esta carpeta, aparte, con `[DOCS]`.
