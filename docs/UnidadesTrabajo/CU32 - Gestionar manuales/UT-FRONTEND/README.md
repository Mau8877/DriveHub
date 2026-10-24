# Unidades de trabajo — CU32-FE

**Caso de uso:** Gestionar manuales: pantalla de manuales de la escuela (listado, creación con PDF, edición, publicar y archivar).
**Microservicio(s) / micro-frontend(s):** `contenido` (web, React) y el paquete compartido `@drivehub/gestion` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU32 - Gestionar manuales.md`](../../../CasosDeUso/CU32%20-%20Gestionar%20manuales.md)
**Recurso HTTP / contrato:** pantalla `/contenido`, sobre `/api/v1/manuales` ([UT-BACKEND](../UT-BACKEND/README.md))

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) y [RUTAS_NAVEGACION_FRONTEND.md](../../../../.claude/rules/frontend/RUTAS_NAVEGACION_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Se amplía `<dh-gestion>`, no se hace una excepción local.** Hacen falta tres cosas que el esquema no expresaba: subir un archivo (`tipo: 'archivo'`: lo sube al elegirlo, a `campo.subida`, y al enviar solo viaja su descriptor si es nuevo), escribir varias etiquetas (`tipo: 'lista'`: texto separado por comas que viaja como arreglo) y una columna con enlace (`tipo: 'enlace'`). `solicitar()` acepta ahora un `FormData`.
- **Primera pantalla de gestión en React.** React 19 pasa el esquema como propiedad (`<dh-gestion esquema={...} />`); se declara el elemento en `src/dh-gestion.d.ts`.
- **Una sola ruta**: la raíz del remoto (`/contenido`) es la pantalla de manuales.
- **El PDF se abre directo desde su URL pública** (bucket de lectura pública), en otra pestaña con `rel="noopener noreferrer"`.
- **Categorías fijas** (`GENERAL`, `A`, `B`, `C`) en el esquema mientras el catálogo de la escuela no tenga caso de uso.
- **Los dev servers de Docker suman `contenido`** (`docker-compose.yml`) y el lockfile suma `@drivehub/gestion` en su `package.json`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU32-FE-01](./UT-CU32-FE-01.md) | Ampliar `<dh-gestion>` con `archivo`, `lista` y `enlace` | — |
| [UT-CU32-FE-02](./UT-CU32-FE-02.md) | Esquema, pantalla y ruta de manuales | 01, BE-05 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, F1 a F4 | Listado, creación con PDF, edición, publicar y archivar | 01, 02 |
| A1, A2 | Validación y archivo en el cliente | 01, 02 |
| A3 | `ERR_CON_03` bajo el campo del PDF | 02 |

## Follow-ups (no se implementan en estas UT)

- Categorías desde el catálogo de la escuela.
- Pantalla de consulta para el estudiante (CU34, móvil).

## Qué NO incluyen estas UT

- Cambios en el shell: `/contenido` ya está montado y el menú ya lo ofrece al Administrador.
- Mobile: el CU32 es solo web.
