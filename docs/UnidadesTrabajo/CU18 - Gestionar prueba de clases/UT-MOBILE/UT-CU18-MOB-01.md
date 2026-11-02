# UT-CU18-MOB-01 — Modelos y `ClasesService`: criterios, evaluar y consultar pruebas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-MOB-01` |
| **Nombre** | Modelos y `ClasesService`: criterios, evaluar y consultar pruebas |
| **Caso de uso** | CU18-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (DTOs inmutables en `models/`, `fromJson`/`toJson` a mano, solo el service construye y parsea; filtros con corchetes que `Dio` codifica solo), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo UT-CU17-MOB-01 (`ClaseDictada`, `CerrarClaseRequest`, `AsistenciaClase` y los métodos de CU17 en `ClasesService`) y el contrato del [README de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http).

## Solicitud de negocio

La app tiene que poder pedir los criterios, enviar una evaluación y leer las pruebas de una clase.

## Objetivo

Los modelos de la prueba y los tres métodos del service existen, y la agenda lee `evaluable` y `conPrueba`.

## Alcance

- `lib/features/clases/models/` (clases inmutables, campos `final`, constructor `const`):
  - `tipo_prueba.dart`: enum `TipoPrueba` (`manejo`, `estacionamiento`, `ciudad`) con `fromJson`/`toJson` hacia `MANEJO`/`ESTACIONAMIENTO`/`CIUDAD`.
  - `criterio_del_catalogo.dart`: `CriterioDelCatalogo(codigo, descripcion, maximo)` y `TipoDelCatalogo(tipo, etiqueta, criterios)`; `CatalogoPruebas(tipos)` con `TipoDelCatalogo de(TipoPrueba)`. `fromJson` desde `data.tipos`.
  - `evaluar_clase_request.dart`: `EvaluarClaseRequest(claseCodigo, tipo, criterios: List<PuntajeDeCriterio>, observaciones?)` y `PuntajeDeCriterio(codigo, puntaje)`; `toJson` (no envía `observaciones` si es nulo o está en blanco).
  - `prueba_clase.dart`: `PruebaClase(codigo, claseCodigo, tipo, criterios: List<CriterioEvaluado>, puntajeTotal, resultado, observaciones?, fecha)` con `CriterioEvaluado(codigo, descripcion, puntaje, maximo)` y el enum `ResultadoPrueba` (`aprobado`, `reprobado`). Sin ids internos. Un getter `aprobada`.
  - `clase_agenda.dart` (se edita): `bool evaluable` y `bool conPrueba`, ambos con default `false`; `fromJson` los lee con `?? false` (un backend viejo que no los manda se lee como `false`), como `cerrable`.
- `lib/features/clases/services/clases_service.dart` (se edita), con el `ApiClient` de `core/network/`:
  - `Future<CatalogoPruebas> catalogoDePruebas()` → `GET /pruebas-clase/criterios`.
  - `Future<PruebaClase> evaluarClase(EvaluarClaseRequest)` → `POST /pruebas-clase`.
  - `Future<List<PruebaClase>> pruebasDeClase(String claseCodigo)` → `GET /pruebas-clase` con `filter[claseCodigo]` y `pageSize` de 20 (una clase tiene a lo sumo tres, así que una página alcanza).
- `test/helpers/clases_falsos.dart` (se edita): `ClasesServiceFalso` gana los tres métodos (con respuestas configurables y registro de las llamadas) y constructores de prueba `catalogoDePruebaDePrueba`, `pruebaClaseDePrueba`.
- Tests: `fromJson` de cada modelo con la respuesta completa (varios criterios, `observaciones` nulo y presente); `toJson` del request (orden y claves, sin `observaciones` en blanco); `ClaseAgenda` sin `evaluable`/`conPrueba` → `false` y con ellos → los lee; el service llama a la ruta, el método y los parámetros correctos (con el filtro `filter[claseCodigo]` bien codificado) y devuelve los modelos; un `status: failed` llega como `ApiException` con su código (`ERR_ACA_39`, `ERR_ACA_40` y `ERR_ACA_41`).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/models/tipo_prueba.dart`, `criterio_del_catalogo.dart`, `evaluar_clase_request.dart`, `prueba_clase.dart`
- `projects/mobile/lib/features/clases/models/clase_agenda.dart`
- `projects/mobile/lib/features/clases/services/clases_service.dart`
- `projects/mobile/test/helpers/clases_falsos.dart`
- `projects/mobile/test/features/clases/clases_models_test.dart`, `clases_service_test.dart`

## Fuera de alcance (no tocar)

- Providers, hojas y botones de la agenda (UT 02 a 05).
- Copiar el catálogo de criterios a la app: siempre se pide al servidor.
- Modelos para editar o borrar una prueba.

## Resultado esperado

La app puede pedir el catálogo, evaluar una clase y leer sus pruebas, con modelos tipados y probados.

## Validación

1. `flutter analyze` sin errores.
2. `flutter test test/features/clases/` pasa (en esta máquina Windows no corre `flutter test`: se corre en Linux sobre una copia, como CU15 a CU17).
