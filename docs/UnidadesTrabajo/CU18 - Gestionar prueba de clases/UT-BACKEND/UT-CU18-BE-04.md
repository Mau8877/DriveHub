# UT-CU18-BE-04 — DTOs y reglas de la prueba

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-04` |
| **Nombre** | DTOs y reglas de la prueba |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU18-BE-01`, `UT-CU18-BE-02` |

## Antes de implementar

[CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (DTOs como `record`, nunca un Model), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md). Tomar como modelo UT-CU17-BE-04 (`CerrarClaseRequest`, `ReglasDeClase`, `ReglasDeClaseLimites`). Decisiones 7 y 9 del [README](./README.md).

## Solicitud de negocio

Lo que el instructor envía tiene que validarse igual siempre, y el total y el resultado tienen que calcularse en un solo lugar.

## Objetivo

Los DTOs de la prueba existen y `ReglasDePrueba` valida los criterios contra el catálogo y calcula el total y el resultado.

## Alcance

- `features/clases/dto/` (`record`):
  - `EvaluarClaseRequest(UUID claseCodigo, TipoPrueba tipo, List<PuntajeDeCriterio> criterios, String observaciones)` con `@NotNull` en `claseCodigo`, `tipo` y `criterios`, `@Valid` sobre cada criterio y `@Size(max = 500)` en `observaciones`.
  - `PuntajeDeCriterio(String codigo, Double puntaje)` con `@NotBlank` en `codigo` y `@NotNull` en `puntaje`.
  - `PruebaClaseResponse(UUID codigo, UUID claseCodigo, TipoPrueba tipo, List<CriterioEvaluacion> criterios, double puntajeTotal, ResultadoEvaluacion resultado, String observaciones, LocalDate fecha)` y un `static de(PruebaClase prueba, UUID claseCodigo)`. Sin ids internos.
  - `CatalogoPruebasResponse(List<TipoDelCatalogo> tipos)` y su `static de(...)` desde `CatalogoDePruebas`.
- `ReglasDePrueba` (nueva clase, `features/clases/service/`, `@Component`; usa `CatalogoDePruebas`):
  - `static final double PUNTAJE_APROBACION = 70` y `PUNTAJE_MAXIMO_POR_CRITERIO = 10` (esta última sale del catálogo, no se repite el número).
  - `List<CriterioEvaluacion> validarYArmarCriterios(TipoPrueba tipo, List<PuntajeDeCriterio> enviados)`: comprueba que los códigos enviados son **exactamente** los del tipo (ni falta uno, ni sobra, ni se repite, ni es de otro tipo) y que cada puntaje está entre 0 y el máximo en pasos de 0,5; devuelve los criterios en el **orden del catálogo**, con `descripcion` y `maximo` copiados de él. Cualquier incumplimiento → `ValidationException` `ERR_ACA_41` con un mensaje en español que dice qué corregir (por ejemplo «Falta puntuar: Control del volante»).
  - `double calcularTotal(List<CriterioEvaluacion> criterios)`: (suma de `puntaje` ÷ suma de `maximo`) × 100, redondeado a 2 decimales `HALF_UP`.
  - `ResultadoEvaluacion resultadoDe(double total)`: `APROBADO` con 70 o más; `REPROBADO` con menos.
  - `String limpiarObservaciones(String texto)`: recorta; en blanco → `null`.
- Tests: criterios completos válidos → mismos criterios en el orden del catálogo con su descripción y máximo; falta uno, sobra uno, uno repetido, uno de otro tipo, lista vacía → `ERR_ACA_41`; puntaje negativo, mayor que 10, de 7,3 (no es múltiplo de 0,5) → `ERR_ACA_41`; `0` y `10` y `9,5` son válidos; el total: todo en 10 → 100,00; todo en 0 → 0,00; todo en 7 → 70,00 y `APROBADO`; un caso que da 69,99 o menos → `REPROBADO`; el redondeo a 2 decimales; observaciones en blanco → `null` y con espacios alrededor se recortan; Bean Validation: `claseCodigo`, `tipo` o `criterios` nulos, un criterio sin código o sin puntaje y observaciones de 501 → violaciones.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/dto/EvaluarClaseRequest.java`, `PuntajeDeCriterio.java`, `PruebaClaseResponse.java`, `CatalogoPruebasResponse.java`
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/clases/service/ReglasDePrueba.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/service/ReglasDePruebaTest.java`
- `projects/backend/academico/src/test/java/com/drivehub/academico/features/clases/dto/EvaluarClaseRequestTest.java`

## Fuera de alcance (no tocar)

- Guardar la prueba o su nota (UT 05) y los endpoints (UT 08).
- Corregir una prueba: no hay `ActualizarPruebaRequest`.
- Un resultado por criterio (aprobado/reprobado de cada uno): solo cuenta el total.

## Resultado esperado

Cualquier cuerpo de evaluación se valida y puntúa por una sola ruta, con el mismo resultado en el servicio y en los tests.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `academico` pasan.
2. El corte de 70 y el máximo de 10 están en un solo lugar cada uno.
