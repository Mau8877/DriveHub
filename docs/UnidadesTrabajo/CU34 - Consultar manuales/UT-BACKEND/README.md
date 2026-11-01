# Unidades de trabajo — CU34-BE

**Caso de uso:** Consultar manuales: el Estudiante lista y lee los manuales publicados de su escuela.
**Microservicio(s) / micro-frontend(s):** `contenido` (Laravel, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU34 - Consultar manuales.md`](../../../CasosDeUso/CU34%20-%20Consultar%20manuales.md)
**Recurso HTTP / contrato:** `/api/v1/manuales/publicados` (el recurso `manuales` ya está enrutado por el gateway a `contenido`)

## Antes de implementar cualquier UT

Leer [LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **Sub-ruta `publicados` en vez de abrir `GET /manuales` a otro rol.** El listado y el detalle del Administrador (CU32) siguen exclusivos de `ADMIN_ESCUELA`; el Estudiante tiene su propio par de rutas de solo lectura, con su propio middleware de rol. Se registran **antes** del grupo del Administrador porque `publicados` no es un código de manual.
- **Se reutiliza todo lo de CU32:** `ManualRepository::listar` (con el estado fijo en `PUBLICADO`), `ManualResource`, `ListarManualesRequest` (el `filter[estado]` que llegue se ignora) y los códigos de error. No hay migración ni código de error nuevo.
- **Un manual no publicado es un `404 ERR_CON_01`**, el mismo que el de otra escuela: no se revela que existe.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `GET` | `/api/v1/manuales/publicados?page&pageSize&search&filter[categoria]` | 200 | `ManualResource[]` + `meta` |
| `GET` | `/api/v1/manuales/publicados/{codigo}` | 200 | `ManualResource` |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU34-BE-01](./UT-CU34-BE-01.md) | Consulta de manuales publicados del Estudiante | — |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 2 y 3, CA01 a CA03 | Listado, búsqueda, categoría y detalle de publicados | 01 |
| A5, A6, A7, CA04, CA05 | Rol, escuela del token y manual no publicado | 01 |

## Follow-ups (no se implementan en estas UT)

- La consulta del Instructor, si algún día se pide.

## Qué NO incluyen estas UT

- Cambios a la gestión del Administrador (CU32) ni a los tutoriales.
