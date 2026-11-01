# Unidades de trabajo — CU40-BE

**Caso de uso:** Analizar foto del tablero: el servidor guarda el resultado del análisis que hizo el teléfono, con su miniatura opcional.
**Microservicio(s) / micro-frontend(s):** `asistente-ia` (FastAPI, capas, DynamoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU40 - Analizar foto del tablero.md`](../../../CasosDeUso/CU40%20-%20Analizar%20foto%20del%20tablero.md)
**Recurso HTTP / contrato:** `/api/v1/ayudas-tablero` (ya enrutado por el gateway a `asistente-ia`)

## Antes de implementar cualquier UT

Leer [FASTAPI_BACKEND.md](../../../../.claude/rules/backend/FASTAPI_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **El servidor no hace inferencia.** El requisito del proyecto es IA local en el móvil: el análisis corre en el teléfono y el backend solo valida y guarda el resultado. Por eso no hay cliente de modelo, ni claves de un proveedor de IA, ni GPU.
- **Es el primer servicio FastAPI que valida el JWT.** Se porta el diseño de Contenido (Laravel): RS256 contra el JWKS de Identidad, emisor `drivehub-identidad`, sin confiar en el gateway. En FastAPI son dependencias (`usuario_actual`, `exigir_rol`) y no un middleware. `PyJWKClient` cachea las claves 5 minutos. La persona sale del `sub` del token.
- **`multipart/form-data` con un campo `datos` (JSON) y una parte `miniatura`.** Un solo POST guarda el resultado y la miniatura, sin subida previa como en los manuales: la miniatura es opcional y pequeña.
- **`SeveridadTablero` pasa de texto a enum** (`INFORMATIVA`, `ADVERTENCIA`, `CRITICA`), como pedía el comentario del modelo. `imagenKey` pasa a opcional.
- **Miniatura sin credenciales de MinIO = ayuda sin miniatura** (desarrollo sin MinIO); con credenciales y MinIO caído es `503 ERR_IA_02` para no perder en silencio una foto que la persona mandó.
- **Campo `module` de los logs:** `module` es un atributo reservado de `LogRecord` y `extra` no puede pisarlo (lanza `KeyError`). Se pasa como `modulo` y el formatter JSON lo escribe como `module`. Se corrige además el uso existente en `creador_tablas.py`.

## Contrato de datos / recurso HTTP

| Método | Path | Status | Cuerpo | `data` |
| --- | --- | --- | --- | --- |
| `POST` | `/api/v1/ayudas-tablero` | 201 | `multipart`: `datos` (JSON) y `miniatura` (JPEG/PNG ≤ 500 KB, opcional) | `AyudaTableroResponse` |

`datos`: `{ indicadores[1..3]{ codigo, nombre, confianza 0..1, severidad }, respuesta (≤ 800), modelo (≤ 60), latenciaMs }`.
`AyudaTableroResponse`: `{ codigo, indicadores[], respuesta, modelo, latenciaMs, imagenUrl, util, creadaEn }`. Nunca salen `usuarioCodigo` ni `sk`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU40-BE-01](./UT-CU40-BE-01.md) | Seguridad JWT, configuración e infraestructura de Asistente IA | — |
| [UT-CU40-BE-02](./UT-CU40-BE-02.md) | Guardar la ayuda del tablero | BE-01 |

**Cadena:** `01 → 02`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 5, CA03, CA05 | Guardar la ayuda con o sin miniatura | 02 |
| A6, A7, CA06, CA07 | Miniatura y datos inválidos, MinIO caído | 02 |
| A8, CA08 | Rol Estudiante y sesión | 01, 02 |

## Follow-ups (no se implementan en estas UT)

- Limpieza de miniaturas huérfanas si se borran ayudas.

## Qué NO incluyen estas UT

- El historial ([CU42-BE](../../CU42%20-%20Consultar%20historial%20de%20ayudas%20IA/UT-BACKEND/README.md)), las sesiones de CU39, MCP (CU46) ni la app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).
