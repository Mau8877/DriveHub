# Unidades de trabajo — CU20-BE

**Caso de uso:** Configurar simulacro escrito: crear, listar, editar, desactivar y activar simulacros, aislado por escuela.
**Microservicio(s) / micro-frontend(s):** `academico` (Spring Boot, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU20 - Configurar simulacro escrito.md`](../../../CasosDeUso/CU20%20-%20Configurar%20simulacro%20escrito.md)
**Recurso HTTP / contrato:** `/api/v1/simulacros` (ya enrutado por el gateway a `academico`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **Simulacros por escuela.** `SimulacroConfig` no tenía `escuelaCodigo`. Se agrega (y `creadoPorCodigo`) y el filtro es explícito en el repositorio con la escuela del claim `escuela`, igual que en CU19. El índice compuesto empieza por la escuela (`escuelaCodigo, categoria, activo`): es el que usará CU21 para ofrecer el simulacro activo de una categoría.
- **Un simulacro no se borra, se desactiva** (`activo`). `deletedAt` queda sin usar, pero las consultas lo siguen filtrando porque MongoDB no lo hace solo.
- **Las reglas que cruzan campos son del servicio** (`ReglasSimulacro`): los temas no se repiten (sin distinguir mayúsculas) y **las cantidades suman exactamente `nroPreguntas`**. Las anotaciones del DTO solo cubren cada campo por separado.
- **`PATCH` parcial con revalidación:** lo no enviado no se toca; si llega el número de preguntas o las reglas, se valida el conjunto (cambiar solo el número revalida las reglas guardadas).
- **Nombre único por escuela** sin distinguir mayúsculas (`ERR_ACA_23`); al editar, cambiar solo mayúsculas del propio nombre no es un duplicado.
- **No se consulta el banco al configurar:** que un tema tenga suficientes preguntas activas es asunto de CU21 al armar el intento (el CU lo declara fuera de alcance).
- **Listado con `MongoTemplate`** y filtros opcionales (`SimulacroConfigRepositoryCustom`), búsqueda como expresión regular escapada sobre el nombre.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `POST` | `/api/v1/simulacros` | 201 | `SimulacroResponse` |
| `GET` | `/api/v1/simulacros?page&pageSize&search&filter[categoria]&filter[activo]` | 200 | `SimulacroResponse[]` + `meta` |
| `GET` | `/api/v1/simulacros/{codigo}` | 200 | `SimulacroResponse` |
| `PATCH` | `/api/v1/simulacros/{codigo}` | 200 | `SimulacroResponse` |
| `POST` | `/api/v1/simulacros/{codigo}/desactivar` | 200 | `SimulacroResponse` |
| `POST` | `/api/v1/simulacros/{codigo}/activar` | 200 | `SimulacroResponse` |

`SimulacroResponse`: `codigo`, `nombre`, `categoria`, `duracionMin`, `nroPreguntas`, `puntajeAprobacion`, `reglas[{ tema, cantidad }]`, `activo`, `creadoEn`. El id interno Long nunca sale.

Códigos de error nuevos: `ERR_ACA_21` a `ERR_ACA_23` ([docs/errors](../../../errors/academico/ERRORES_ACADEMICO.md)).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU20-BE-01](./UT-CU20-BE-01.md) | Modelo por escuela y códigos de error | — |
| [UT-CU20-BE-02](./UT-CU20-BE-02.md) | Repositorio y listado de simulacros | 01 |
| [UT-CU20-BE-03](./UT-CU20-BE-03.md) | Servicio y reglas de los simulacros | 01, 02 |
| [UT-CU20-BE-04](./UT-CU20-BE-04.md) | Controller, errores documentados y seed | 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Listar con búsqueda, filtros y paginación | 02, 04 |
| F1 | Crear con reglas por tema | 03, 04 |
| F2 | Editar y revalidar | 03, 04 |
| F3, F4 | Desactivar y activar | 03, 04 |
| A1 a A8 | Validación, reglas, nombre repetido, 404, estado repetido, rol, token sin escuela | 01, 03, 04 |

## Follow-ups (no se implementan en estas UT)

- Aviso al configurar cuando un tema no tiene suficientes preguntas activas.
- Test de integración con MongoDB real (Testcontainers) de los criterios de aceptación.
- Filtro por tenant en `intentos_simulacro` (CU21).

## Qué NO incluyen estas UT

- CU21 a CU23.
- Cambios en el gateway: `simulacros` ya está enrutado a `academico`.
