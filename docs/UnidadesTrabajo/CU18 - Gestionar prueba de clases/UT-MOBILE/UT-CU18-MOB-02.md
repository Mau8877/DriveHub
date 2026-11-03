# UT-CU18-MOB-02 — Providers de la prueba de clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-MOB-02` |
| **Nombre** | Providers de la prueba de clase |
| **Caso de uso** | CU18-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU18-MOB-01` |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers de datos con `AsyncValue` y nombres `{feature}DetailProvider`, `{feature}Provider`, `{feature}UiProvider`; una mutación invalida lo que cambia). Tomar como modelo UT-CU17-MOB-02 (`clase_dictada_provider.dart` y `gestion_clase_provider.dart`). Decisiones 2, 8 y 9 del [README](./README.md).

## Solicitud de negocio

Las hojas de evaluar y de ver la prueba necesitan los criterios, las pruebas de la clase y una forma de enviar la evaluación con el resultado a la vista.

## Objetivo

Existen los providers que traen el catálogo y las pruebas de una clase, y el que envía una evaluación y refresca la agenda.

## Alcance

- `providers/catalogo_pruebas_provider.dart` (nuevo): `catalogoPruebasProvider`, un `FutureProvider<CatalogoPruebas>` que llama a `ClasesService.catalogoDePruebas()`. No se destruye al cerrar la hoja (`keepAlive`): se pide una vez por sesión de la app. Se invalida al cerrar sesión por el mismo mecanismo que usen los demás providers de `clases` (no se agrega uno nuevo).
- `providers/pruebas_de_clase_provider.dart` (nuevo): `pruebasDeClaseDetailProvider`, un `FutureProvider.family<List<PruebaClase>, String>` por `claseCodigo`, con `autoDispose`.
- `providers/gestion_prueba_clase_provider.dart` (nuevo): `gestionPruebaClaseProvider`, un notifier con un método `Future<PruebaClase> evaluar(EvaluarClaseRequest)`:
  - Llama a `ClasesService.evaluarClase`.
  - Si sale bien, invalida `agendaProvider` (para que la agenda recalcule `evaluable` y `conPrueba`) y `pruebasDeClaseDetailProvider(claseCodigo)`, y devuelve la prueba guardada.
  - Un error se relanza **sin tragarlo** (`ApiException` con su código, o el error de red) para que la hoja decida qué mostrar (decisión 9). Además de los códigos que ya cierran la hoja en CU17, expone si un error es de los que obligan a refrescar y cerrar (`ERR_ACA_35`, `ERR_ACA_39`, `ERR_ACA_40`): también invalida la agenda en esos casos.
  - Un estado de «enviando» (`*UiProvider` o el estado del notifier, igual que `gestion_clase_provider`) para deshabilitar los botones.
- Tests (con `ClasesServiceFalso`): el catálogo se pide una sola vez aunque se lea dos veces; las pruebas de una clase se piden por código y una clase distinta pide de nuevo; `evaluar` bien → devuelve la prueba, llama al service con el request, invalida la agenda y las pruebas de esa clase; `evaluar` con `ERR_ACA_41` → relanza y **no** invalida; con `ERR_ACA_40` y `ERR_ACA_39` → relanza y sí invalida la agenda; con error de red → relanza y no invalida; mientras envía, el estado dice «enviando» y al terminar vuelve.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/providers/catalogo_pruebas_provider.dart`, `pruebas_de_clase_provider.dart`, `gestion_prueba_clase_provider.dart`
- `projects/mobile/test/features/clases/catalogo_pruebas_provider_test.dart`, `gestion_prueba_clase_provider_test.dart`, `pruebas_de_clase_provider_test.dart`

## Fuera de alcance (no tocar)

- Las hojas y los botones (UT 03 a 05).
- Los avisos y los textos de los mensajes: los arma la hoja.
- Editar o borrar una prueba.

## Resultado esperado

La interfaz tiene dónde pedir los criterios y las pruebas, y una acción que evalúa y deja la agenda al día.

## Validación

1. `flutter analyze` sin errores.
2. `flutter test test/features/clases/` pasa (en Linux, ver UT-CU18-MOB-01).
