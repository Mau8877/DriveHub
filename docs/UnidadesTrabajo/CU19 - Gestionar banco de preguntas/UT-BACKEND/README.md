# Unidades de trabajo — CU19-BE

**Caso de uso:** Gestionar banco de preguntas: crear, listar, editar, desactivar y activar preguntas, aislado por escuela.
**Microservicio(s) / micro-frontend(s):** `academico` (Spring Boot, capas, MongoDB) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU19 - Gestionar banco de preguntas.md`](../../../CasosDeUso/CU19%20-%20Gestionar%20banco%20de%20preguntas.md)
**Recurso HTTP / contrato:** `/api/v1/preguntas` (ya enrutado por el gateway a `academico`)

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) (capas), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md) (MongoDB), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

- **Banco por escuela.** `Pregunta` no tenía `escuelaCodigo`. Se agrega y el filtro es explícito en el repositorio con la escuela del claim `escuela`, igual que en CU07 y CU32. El índice compuesto pasa a empezar por la escuela (`escuelaCodigo, categoria, tema, activa`): es el que usará CU20 para sacar preguntas por categoría y tema.
- **Una pregunta no se borra, se desactiva** (`activa`). `deletedAt` queda sin usar en este caso de uso, pero las consultas lo siguen filtrando porque MongoDB no lo hace solo.
- **Las letras de las opciones las pone el servicio** (A a F, por orden): el cliente solo manda texto y si es correcta. Así no hay ids repetidos ni saltos. Al editar las opciones se reemplazan todas.
- **Las reglas que cruzan campos son del servicio** (`ReglasPregunta`): cuántas opciones y cuántas correctas según el tipo. Las anotaciones del DTO solo cubren cada campo por separado. El esquema de la pantalla repite las reglas de un solo campo y las de filas (de 2 a 6, con texto, una correcta); las que dependen del tipo las valida solo el backend.
- **`PATCH` parcial con revalidación:** lo no enviado no se toca; si llega el tipo o las opciones, se valida el conjunto completo (cambiar solo el tipo revalida las opciones guardadas).
- **Listado con `MongoTemplate`** y filtros opcionales (`PreguntaRepositoryCustom`): un método derivado no puede expresar "filtro presente o ausente". La búsqueda es una expresión regular escapada (`Pattern.quote`) sobre enunciado y tema.
- **El `GlobalExceptionHandler` de Académico aprendió a responder 400** a un cuerpo ilegible, un valor de enumeración desconocido o un parámetro con formato inválido (antes caían en el 500).
- **Sin imagen:** la pregunta tiene un campo `imagenUrl`, pero no se gestiona en este caso de uso.

## Contrato de datos / recurso HTTP

| Método | Path | Status | `data` |
| --- | --- | --- | --- |
| `POST` | `/api/v1/preguntas` | 201 | `PreguntaResponse` |
| `GET` | `/api/v1/preguntas?page&pageSize&search&filter[categoria]&filter[tema]&filter[tipo]&filter[dificultad]&filter[activa]` | 200 | `PreguntaResponse[]` + `meta` |
| `GET` | `/api/v1/preguntas/{codigo}` | 200 | `PreguntaResponse` |
| `PATCH` | `/api/v1/preguntas/{codigo}` | 200 | `PreguntaResponse` |
| `POST` | `/api/v1/preguntas/{codigo}/desactivar` | 200 | `PreguntaResponse` |
| `POST` | `/api/v1/preguntas/{codigo}/activar` | 200 | `PreguntaResponse` |

`PreguntaResponse`: `codigo`, `categoria`, `tema`, `tipo`, `enunciado`, `opciones[{ id, texto, correcta }]`, `explicacion`, `dificultad`, `activa`, `creadoEn`. El id interno Long nunca sale.

Códigos de error nuevos: `ERR_ACA_01` a `ERR_ACA_03` ([docs/errors](../../../errors/academico/ERRORES_ACADEMICO.md)).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU19-BE-01](./UT-CU19-BE-01.md) | Modelo por escuela, errores y manejo de 400 | — |
| [UT-CU19-BE-02](./UT-CU19-BE-02.md) | Repositorio y listado de preguntas | 01 |
| [UT-CU19-BE-03](./UT-CU19-BE-03.md) | Servicio y reglas de las preguntas | 01, 02 |
| [UT-CU19-BE-04](./UT-CU19-BE-04.md) | Controller, errores documentados y seed | 03 |

**Cadena:** `01 → 02 → 03 → 04`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Listar con búsqueda, filtros y paginación | 02, 04 |
| F1 | Crear con opciones según el tipo | 03, 04 |
| F2 | Editar y revalidar | 03, 04 |
| F3, F4 | Desactivar y activar | 03, 04 |
| A1 a A7 | Validación, opciones, 404, estado repetido, rol, token sin escuela | 01, 03, 04 |

## Follow-ups (no se implementan en estas UT)

- Imagen de la pregunta (almacenamiento propio de Académico o reutilizar el de Contenido).
- Importar y exportar preguntas.
- Catálogo de categorías de licencia.
- Filtro por tenant en `simulacros_config` e `intentos_simulacro` (CU20 y CU21).

## Qué NO incluyen estas UT

- CU20 a CU23.
- Cambios en el gateway: `preguntas` ya está enrutado a `academico`.
