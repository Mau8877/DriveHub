# UT-CU17-MOB-04 — «Cerrar clase» y «Editar» en la agenda

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-MOB-04` |
| **Nombre** | «Cerrar clase» y «Editar» en la agenda |
| **Caso de uso** | CU17-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU17-MOB-01`, `UT-CU17-MOB-03` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (solo colores del tema; el estado va en texto), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo cómo `ClaseAgendaCard` ofrece «Cancelar» con `alCancelar` (UT-CU16-MOB-04). Decisión 3 del [README](./README.md).

## Solicitud de negocio

El instructor ve su agenda y, justo en cada clase, el botón que corresponde: cerrarla si ya empezó, o editarla si ya la cerró.

## Objetivo

Cada clase de la agenda ofrece «Cerrar clase» o «Editar» según lo que diga el servidor, y los abre.

## Alcance

- `widgets/clase_agenda_card.dart` (se edita): dos callbacks opcionales nuevos, `alCerrar` y `alEditar` (como `alCancelar`, la tarjeta no usa Riverpod).
  - «Cerrar clase» (`Key('cerrar-<codigo>')`, botón tonal) se dibuja solo si `clase.cerrable` y se le pasó `alCerrar`.
  - «Editar» (`Key('editar-<codigo>')`, botón de texto) se dibuja solo si `clase.claseCodigo != null` y se le pasó `alEditar`.
  - «Cancelar» (CU16) no cambia. Una clase tiene a lo sumo uno de los tres botones; las demás ninguno.
- `widgets/lista_de_agenda.dart` (se edita): pasa `alCerrar: () => mostrarCerrarClase(context, clase)` y `alEditar: () => mostrarEditarClase(context, clase)`.
- La insignia de la tarjeta no cambia: una confirmada que ya empezó y no terminó sigue diciendo «Confirmada»; una que terminó sin cerrarse, «Pendiente de cierre»; al cerrar pasa a «Completada» o «No asistió» con la agenda refrescada.
- Tests de widget (agenda con `ClasesServiceFalso`): una clase `cerrable` ofrece «Cerrar clase» y ninguna otra acción; una `Completada` o `No asistió` con `claseCodigo` ofrece «Editar»; una `cancelable` sigue ofreciendo solo «Cancelar»; una `Completada` sin `claseCodigo` (dato viejo), una `Cancelada` y una confirmada que no empezó no ofrecen cerrar ni editar; tocar «Cerrar clase» abre la hoja de cerrar y tocar «Editar» abre la de editar con los datos precargados; de punta a punta en la pantalla, cerrar una clase la deja `Completada` con «Editar» en vez de «Cerrar clase»; sin los callbacks los botones no se dibujan. Los tests de CU16 de la tarjeta y la agenda siguen en verde.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/clase_agenda_card.dart`, `lista_de_agenda.dart`
- `projects/mobile/test/features/clases/clase_agenda_card_test.dart`, `agenda_instructor_screen_test.dart`
- `projects/mobile/test/features/clases/cerrar_clase_agenda_test.dart` (el recorrido de punta a punta por la pantalla)

## Fuera de alcance (no tocar)

- La hoja, sus validaciones y sus avisos (UT 03).
- El encabezado de día, el estado vacío y «Cancelar» (CU16).
- La prueba de clase (CU18), y mostrar la asistencia en la tarjeta.
- Rutas nuevas, ni pantallas completas.

## Resultado esperado

El instructor cierra una clase desde su agenda y la corrige después, y ve en cada clase solo la acción que corresponde.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Prueba manual (ver prerrequisitos del README): una clase que ya empezó ofrece «Cerrar clase»; al cerrarla queda «Completada» y ofrece «Editar»; una que aún no empezó solo ofrece «Cancelar»; el estudiante ve la clase en su «Historial» como `Realizada` o `No asistió`.
