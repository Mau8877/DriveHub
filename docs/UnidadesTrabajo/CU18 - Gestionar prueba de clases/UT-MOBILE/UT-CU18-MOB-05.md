# UT-CU18-MOB-05 — «Evaluar» y «Ver prueba» en la agenda

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-MOB-05` |
| **Nombre** | «Evaluar» y «Ver prueba» en la agenda |
| **Caso de uso** | CU18-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU18-MOB-01`, `UT-CU18-MOB-03`, `UT-CU18-MOB-04` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (solo colores del tema; el estado va en texto), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo UT-CU17-MOB-04 (`alCerrar` y `alEditar` en `ClaseAgendaCard`). Decisión 3 del [README](./README.md).

## Solicitud de negocio

El instructor ve su agenda y, en cada clase cerrada, el botón que corresponde: evaluarla si todavía le falta alguna prueba, o ver las que ya hizo.

## Objetivo

Cada clase de la agenda ofrece «Evaluar» y «Ver prueba» según lo que diga el servidor, y los abre.

## Alcance

- `widgets/clase_agenda_card.dart` (se edita): dos callbacks opcionales nuevos, `alEvaluar` y `alVerPrueba` (como `alCerrar`, la tarjeta no usa Riverpod).
  - «Evaluar» (`Key('evaluar-<codigo>')`, botón tonal) se dibuja solo si `clase.evaluable` y se le pasó `alEvaluar`.
  - «Ver prueba» (`Key('ver-prueba-<codigo>')`, botón de texto) se dibuja solo si `clase.conPrueba` y se le pasó `alVerPrueba`.
  - «Cancelar», «Cerrar clase» y «Editar» (CU16 y CU17) no cambian. «Editar» y «Evaluar» pueden estar a la vez en una clase cerrada: la fila de acciones se acomoda sin desbordar en pantallas angostas (`Wrap`).
- `widgets/lista_de_agenda.dart` (se edita): pasa `alEvaluar: () => mostrarEvaluarClase(context, clase)` y `alVerPrueba: () => mostrarPruebasDeClase(context, clase)`.
- La insignia de la tarjeta no cambia: la clase sigue diciendo «Completada».
- Tests de widget (agenda con `ClasesServiceFalso`): una clase `evaluable` ofrece «Evaluar»; una `conPrueba` ofrece «Ver prueba»; una con las dos ofrece ambas (más «Editar»); una con las tres pruebas hechas ofrece solo «Ver prueba»; una `Completada` ni evaluable ni con prueba (dato viejo), una `No asistió`, una `Cancelada` y una `Confirmada` no ofrecen ninguna de las dos; sin los callbacks los botones no se dibujan; tocar «Evaluar» abre la hoja de evaluar y «Ver prueba» la de ver; de punta a punta en la pantalla, evaluar una clase hace que la agenda se refresque: aparece «Ver prueba» y, con los tres tipos hechos, desaparece «Evaluar»; los tests de CU16 y CU17 de la tarjeta y la agenda siguen en verde.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/clase_agenda_card.dart`, `lista_de_agenda.dart`
- `projects/mobile/test/features/clases/clase_agenda_card_test.dart`, `agenda_instructor_screen_test.dart`
- `projects/mobile/test/features/clases/evaluar_clase_agenda_test.dart` (el recorrido de punta a punta por la pantalla)

## Fuera de alcance (no tocar)

- Las hojas, sus validaciones y sus avisos (UT 03 y 04).
- El encabezado de día, el estado vacío y las acciones de CU16 y CU17.
- Rutas nuevas, ni pantallas completas.
- Mostrar el puntaje o el resultado en la tarjeta.

## Resultado esperado

El instructor evalúa una clase desde su agenda y ve la prueba después, y en cada clase solo aparecen las acciones que corresponden.

## Validación

1. `flutter analyze` sin errores.
2. `flutter test test/features/clases/` pasa (en Linux, ver UT-CU18-MOB-01).
3. Prueba manual en el teléfono (con el prerrequisito del README y UT-CU18-BE-09): evaluar una clase cerrada con `Presente`, ver su prueba y comprobar que la nota aparece en «Mis notas» del estudiante.
