# UT-CU18-BE-02 — El catálogo de criterios por tipo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-02` |
| **Nombre** | El catálogo de criterios por tipo |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | — |

## Antes de implementar

[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (enums y `record` para datos sin comportamiento). Decisiones 3 y 7 del [README](./README.md) y la tabla de criterios de las reglas de negocio del CU.

## Solicitud de negocio

Los criterios con que se evalúa son los mismos para todos los instructores y tienen que poder cambiarse en un solo lugar.

## Objetivo

`CatalogoDePruebas` entrega, para cada `TipoPrueba`, su etiqueta y sus cinco criterios (clave, descripción y máximo 10), y sabe buscar un criterio por clave.

## Alcance

- `CatalogoDePruebas` (nueva clase, `features/clases/service/`, sin estado, constantes estáticas; no es un bean):
  - `record CriterioDelCatalogo(String codigo, String descripcion, double maximo)` y `record TipoDelCatalogo(TipoPrueba tipo, String etiqueta, List<CriterioDelCatalogo> criterios)` (anidados o en `features/clases/model/`).
  - `List<TipoDelCatalogo> tipos()`: los tres tipos, en el orden `MANEJO`, `ESTACIONAMIENTO`, `CIUDAD`; cada uno con 5 criterios de máximo `10`, en el orden de la tabla del CU. Etiquetas: `Manejo`, `Estacionamiento`, `Ciudad`.
  - `TipoDelCatalogo de(TipoPrueba tipo)`.
  - Claves de criterio legibles en `snake_case` y **únicas dentro de su tipo** (por ejemplo `espejos`, `volante`, `pedales_y_cambios`, `senales`, `distancia_y_velocidad` para `MANEJO`); las demás siguen el mismo estilo según la tabla del CU.
- Tests: hay tres tipos en el orden dicho; cada uno tiene 5 criterios con máximo 10; las claves de un tipo no se repiten; `de(tipo)` devuelve el tipo pedido; las descripciones no están vacías.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/CatalogoDePruebas.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/CatalogoDePruebasTest.java`

## Fuera de alcance (no tocar)

- Validar un cuerpo contra el catálogo y calcular el total (UT 04).
- El endpoint que sirve el catálogo (UT 06 y 08).
- Guardar el catálogo en la base o hacerlo configurable por escuela.
- `TipoPrueba` y `CriterioEvaluacion` ya existentes: no cambian.

## Resultado esperado

Hay una única fuente en código de los criterios de cada tipo de prueba.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. Los 15 criterios y sus claves coinciden con la tabla del CU18.
