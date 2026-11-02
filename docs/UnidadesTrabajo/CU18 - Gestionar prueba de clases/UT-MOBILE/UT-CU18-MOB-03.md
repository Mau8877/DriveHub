# UT-CU18-MOB-03 — La hoja para evaluar una clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-MOB-03` |
| **Nombre** | La hoja para evaluar una clase |
| **Caso de uso** | CU18-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU18-MOB-02` |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (`Form` y `validator` nativos, mensajes en español, mismas reglas que el backend), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (Cockpit: colores solo del tema, el estado va en texto; `AvisoCockpit`, `Insignia`, `IndicadorBoton`, `avisar`), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo UT-CU17-MOB-03 (`FormularioClaseSheet`, `validadores_clase.dart`, `mensajes_de_cierre.dart`, `mostrarCerrarClase`). Decisiones 2 a 6, 8, 9 y 10 del [README](./README.md).

## Solicitud de negocio

El instructor, justo después de la clase, puntúa cada criterio, ve cómo va el total y confirma con un toque.

## Objetivo

`mostrarEvaluarClase(context, clase)` abre una hoja que puntúa los criterios de un tipo, muestra el total en vivo y envía la evaluación.

## Alcance

- `widgets/validadores_prueba.dart` (nuevo, funciones puras; mismas reglas que `ReglasDePrueba` del backend):
  - `double? totalDePrueba(Map<String, double?> puntajes, List<CriterioDelCatalogo> criterios)`: nulo si falta alguno; si no, (suma ÷ suma de máximos) × 100 con dos decimales (`HALF_UP`).
  - `ResultadoPrueba resultadoDePrueba(double total)`: `aprobado` con 70 o más.
  - `String? validarObservacionesDePrueba(String?)`: hasta 500 caracteres, medidos sin recortar (como el backend); mensaje en español.
  - `String? validarCriteriosDePrueba(...)`: «Falta puntuar: {descripción}» con el primero sin puntaje.
- `widgets/evaluar_clase_sheet.dart` (nuevo): `EvaluarClaseSheet`, un `ConsumerStatefulWidget` en una hoja inferior.
  - Cabecera: el estudiante, la fecha y la hora de la clase.
  - Lee `catalogoPruebasProvider` y `pruebasDeClaseDetailProvider(claseCodigo)`. Mientras cargan, un indicador; si fallan, el error con «Reintentar» y «Volver» (no un formulario vacío).
  - **Tipo**: `SegmentedButton` de tres (`Manejo`, `Estacionamiento`, `Ciudad`); los tipos que ya tienen prueba quedan deshabilitados con el texto «Hecha». Arranca en el primer tipo disponible.
  - **Criterios** del tipo elegido, una fila cada uno (`Key('criterio-<codigo>')`): descripción, `Slider` de 0 a su máximo con 20 divisiones y el valor a la derecha («—» sin puntuar, «7,5» con coma decimal). Sin puntuar: control atenuado. Cambiar de tipo reinicia los puntajes.
  - **Observaciones**: `TextFormField` multilínea con contador de 500 (`maxLength` con `MaxLengthEnforcement.none`, como CU17) y opcional.
  - **Total en vivo**: «Total: 72,5 / 100» (con «—» mientras falte alguno) y, con todos puntuados, una `Insignia` «Aprobado» o «Reprobado».
  - Botones: «Volver» y «Guardar prueba» (`Key('guardar-prueba')`), apagado hasta que todos los criterios tengan puntaje; mientras envía, `IndicadorBoton` y todo deshabilitado. La hoja no se cierra tocando afuera ni deslizando (`isDismissible: false`, `enableDrag: false`).
  - Al guardar llama a `gestionPruebaClaseProvider.evaluar(...)` con los criterios en el orden del catálogo. Éxito: cierra la hoja y `avisar` «Prueba registrada: {puntaje} puntos, {aprobado|reprobado}.» con lo que devolvió el servidor. Errores según la decisión 9: `ERR_ACA_35`, `ERR_ACA_39` y `ERR_ACA_40` cierran la hoja y avisan con el mensaje del backend; `ERR_ACA_41` y otros errores de negocio se muestran en un `AvisoCockpit` dentro de la hoja, con lo escrito intacto; un error de red también, y «Guardar prueba» pasa a «Reintentar».
- `widgets/mensajes_de_prueba.dart` (nuevo): los textos del aviso de éxito y de los errores, como `mensajes_de_cierre.dart`.
- `mostrarEvaluarClase(BuildContext, ClaseAgenda)` (en `evaluar_clase_sheet.dart`): abre la hoja con `showModalBottomSheet` (`isScrollControlled: true`).
- Tests de widget: carga el catálogo y muestra los 5 criterios del primer tipo; «Guardar prueba» apagado con criterios sin puntuar y encendido con todos; el total y el resultado se actualizan al mover un control (todo en 8 → «80 / 100», «Aprobado»; todo en 6 → «Reprobado»); cambiar de tipo muestra otros criterios y reinicia los puntajes; un tipo ya hecho aparece deshabilitado; observaciones de 501 → mensaje y no envía; el envío manda el tipo, los criterios en el orden del catálogo y las observaciones; éxito → cierra y avisa con el resultado; `ERR_ACA_40` y `ERR_ACA_39` → cierra y avisa; `ERR_ACA_41` → queda abierta con el aviso y lo puntuado; error de red → queda abierta con «Reintentar»; con el envío en curso los botones están deshabilitados; no se cierra tocando afuera; catálogo que falla → «Reintentar» y «Volver». Pruebas unitarias de `validadores_prueba.dart` (total, redondeo, corte de 70, observaciones).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/validadores_prueba.dart`, `evaluar_clase_sheet.dart`, `mensajes_de_prueba.dart`
- `projects/mobile/test/features/clases/validadores_prueba_test.dart`, `evaluar_clase_sheet_test.dart`

## Fuera de alcance (no tocar)

- Los botones de la agenda que abren la hoja (UT 05).
- Ver la prueba ya guardada (UT 04).
- Editar o borrar una prueba, y criterios que el instructor pueda cambiar.
- Colores o estilos propios: todo sale del tema.

## Resultado esperado

El instructor puntúa los criterios de un tipo, ve el total en vivo y registra la prueba; los errores no le hacen perder lo que escribió.

## Validación

1. `flutter analyze` sin errores.
2. `flutter test test/features/clases/` pasa (en Linux, ver UT-CU18-MOB-01).
3. Prueba manual (con el prerrequisito del README): puntuar los cinco criterios de «Manejo» y guardar muestra el aviso con el resultado.
