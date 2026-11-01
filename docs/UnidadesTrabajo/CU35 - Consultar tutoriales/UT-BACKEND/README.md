# Unidades de trabajo — CU35-BE

**Caso de uso:** Consultar tutoriales: el Estudiante y el Instructor listan y leen los tutoriales publicados de su escuela dirigidos a su audiencia.
**Microservicio(s) / micro-frontend(s):** `contenido` (Laravel, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU35 - Consultar tutoriales.md`](../../../CasosDeUso/CU35%20-%20Consultar%20tutoriales.md)
**Recurso HTTP / contrato:** `/api/v1/tutoriales/publicados` (el recurso `tutoriales` ya está enrutado por el gateway a `contenido`)

## Antes de implementar cualquier UT

Leer [LARAVEL_BACKEND.md](../../../../.claude/rules/backend/LARAVEL_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **Sub-ruta `publicados`**, como en manuales (CU34): la gestión del instructor (CU33) sigue exclusiva de `INSTRUCTOR`; la consulta tiene su propio par de rutas de solo lectura con `rol:ESTUDIANTE,INSTRUCTOR`, registradas antes de `/{codigo}`.
- **La audiencia sale del rol del token:** quien tiene el rol `INSTRUCTOR` ve lo dirigido a `INSTRUCTOR`; el resto, lo dirigido a `ESTUDIANTE`. La consulta filtra por `escuela_codigo`, `estado = PUBLICADO` y `audiencia`.
- **Un tutorial no publicado o de otra audiencia es un `404 ERR_CON_05`**, el mismo que el inexistente: no se revela que existe.
- Se reutilizan `TutorialRepository`, `TutorialResource`, `ListarTutorialesRequest` y los códigos de CU33; no hay migración ni código de error nuevo.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `GET` | `/api/v1/tutoriales/publicados?page&pageSize&search&filter[categoria]&filter[tipo]` | 200 | `TutorialResource[]` + `meta` |
| `GET` | `/api/v1/tutoriales/publicados/{codigo}` | 200 | `TutorialResource` |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU35-BE-01](./UT-CU35-BE-01.md) | Consulta de tutoriales publicados | — (usa lo construido en CU33) |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 2 y 3, CA01 a CA03 | Listado, búsqueda, tipo y detalle de publicados por audiencia | 01 |
| A5, A6, A7, CA04, CA05 | Rol, escuela del token y tutorial no publicado o ajeno | 01 |

## Follow-ups (no se implementan en estas UT)

- Registrar qué tutoriales vio cada persona.

## Qué NO incluyen estas UT

- La gestión del instructor (CU33) ni la app móvil.
