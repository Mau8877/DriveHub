# Unidades de trabajo — CU42-BE

**Caso de uso:** Consultar historial de ayudas IA: listar, ver y marcar «útil» las ayudas del tablero de la persona.
**Microservicio(s) / micro-frontend(s):** `asistente-ia` (FastAPI, capas, DynamoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU42 - Consultar historial de ayudas IA.md`](../../../CasosDeUso/CU42%20-%20Consultar%20historial%20de%20ayudas%20IA.md)
**Recurso HTTP / contrato:** `/api/v1/ayudas-tablero` (ya enrutado por el gateway a `asistente-ia`)

## Antes de implementar cualquier UT

Leer [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Misma ruta que CU40, otros verbos:** `GET /ayudas-tablero` (listado), `GET /ayudas-tablero/{codigo}` (detalle) y `PATCH /ayudas-tablero/{codigo}` (opinión), todos con el rol `ESTUDIANTE`. Se reutilizan el modelo, los schemas, el repositorio y el service de CU40.
- **Paginación `page`/`pageSize` sobre DynamoDB:** DynamoDB no salta registros, así que para la página N se piden los N × tamaño más recientes y se descartan los anteriores; el total sale de una consulta `COUNT`. Es correcto y simple para un historial personal; si creciera mucho se pasaría a un cursor (`ponytail`: documentado en el repositorio).
- **El detalle busca por código dentro de la partición de la persona** (`FilterExpression` sobre `codigoAyudaTablero`): la clave real es `usuarioCodigo` + `sk`, y `sk` lleva la fecha, que el cliente no conoce. Una ayuda de otra persona nunca se encuentra, así que responde `404 ERR_IA_03` igual que una inexistente.
- **La opinión es un `update_item` condicionado** (`SET util`, con `attribute_exists`): no reescribe el ítem.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `GET` | `/api/v1/ayudas-tablero?page&pageSize` | 200 | `AyudaTableroResponse[]` + `meta` |
| `GET` | `/api/v1/ayudas-tablero/{codigo}` | 200 | `AyudaTableroResponse` |
| `PATCH` | `/api/v1/ayudas-tablero/{codigo}` (`{ "util": true }`) | 200 | `AyudaTableroResponse` |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU42-BE-01](./UT-CU42-BE-01.md) | Historial de ayudas del tablero | CU40-BE-02 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 2 a 5, CA01 a CA05 | Listado, detalle y opinión | 01 |
| A5 a A7, CA06, CA07 | Ayuda ajena, paginación inválida y rol | 01 |

## Follow-ups (no se implementan en estas UT)

- Cursor en lugar de páginas, borrar ayudas y filtros.

## Qué NO incluyen estas UT

- Guardar ayudas (CU40) ni la app ([UT-MOBILE](../UT-MOBILE/README.md)).
