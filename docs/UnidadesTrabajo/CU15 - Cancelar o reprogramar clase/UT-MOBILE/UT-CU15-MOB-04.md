# UT-CU15-MOB-04 — Hoja de reprogramar

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU15-MOB-04` |
| **Nombre** | Hoja de reprogramar |
| **Caso de uso** | CU15-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | `UT-CU15-MOB-02`, `UT-CU15-MOB-03` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) (hoja, no ruta), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Decisiones 1, 6, 7 y 8 del [README](./README.md). Tomar como modelo `reservar_tab.dart`, `filtros_reservar.dart`, `franja_card.dart` y `resumen_reserva_sheet.dart`.

## Solicitud de negocio

El estudiante que no puede ir a una clase la cambia a otro horario libre sin perder la hora, eligiendo la nueva franja como si fuera a reservar.

## Objetivo

«Reprogramar» abre una hoja con las franjas libres y sus filtros; el estudiante elige una, confirma, y la clase queda en la franja nueva.

## Alcance

- `widgets/reprogramar_sheet.dart`: `Future<void> mostrarReprogramar(BuildContext, WidgetRef, Reserva)` con `showModalBottomSheet(isScrollControlled: true)` a casi toda la altura. Contenido:
  - Encabezado con la clase actual («Reprogramar: lun 27 oct, 08:00 con {instructor}»).
  - `FiltrosReservar` (día, momento, instructor; son los de la pestaña «Reservar», decisión 6).
  - La lista de `franjasDisponiblesListProvider` con `FranjaCard`; elegir una la marca (`franjaParaReprogramarUiProvider`, una sola) y volver a tocarla la quita. Se oculta la franja en la que ya está la clase.
  - Estados: cargando (esqueleto como `ReservarTab`), vacío (`EstadoVacio` «No hay franjas libres con estos filtros», A1), error con «Reintentar».
  - Pie fijo con «Volver» y «Reprogramar» (deshabilitado hasta elegir una franja).
- Al confirmar: llama a `gestionReservaProvider.reprogramar(reserva.codigo, franja.codigo)`; mientras tanto el botón muestra `IndicadorBoton` y todo queda deshabilitado. En éxito cierra la hoja, vacía la selección y muestra `avisar('Clase reprogramada para el {fecha} a las {hora}')`.
- Errores (decisión 8): `ERR_ACA_18` → `AvisoCockpit` «Otro estudiante ya reservó esa franja. Elige otra.», la hoja sigue abierta, se vacía la selección y las franjas se refrescan; `ERR_ACA_10` o `ERR_ACA_20` → su `mensaje` en un `AvisoCockpit` y la hoja sigue abierta; `ERR_ACA_34`, `ERR_ACA_33` o `ERR_ACA_32` → cierra la hoja y avisa con el `mensaje` (la lista ya se refrescó); de red → `AvisoCockpit` con «Reintentar» y la selección intacta.
- La hoja se cierra con la selección vacía (`vaciar()` también al descartarla).
- `widgets/acciones_reserva.dart` (UT 03): «Mis clases» pasa a `AccionesReserva` el callback `alReprogramar` que abre esta hoja.
- Tests de widget con el provider y el servicio simulados: muestra las franjas y oculta la actual; elegir y volver a tocar alterna la selección; «Reprogramar» está deshabilitado sin selección; en éxito cierra y avisa; `ERR_ACA_18` mantiene la hoja y refresca; `ERR_ACA_34` la cierra; sin franjas muestra el estado vacío; no se puede confirmar dos veces.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/reprogramar_sheet.dart`
- `projects/mobile/lib/features/clases/widgets/lista_de_reservas.dart` (solo para pasar el callback)
- `projects/mobile/test/features/clases/widgets/reprogramar_sheet_test.dart`

## Fuera de alcance (no tocar)

- `ReservarTab`, su selección múltiple y el resumen de confirmar de CU14.
- Mostrar o modificar el saldo de horas: reprogramar no lo toca.
- Filtros propios de la hoja (follow-up).
- Cancelar (UT 03) y cualquier cambio de backend.

## Resultado esperado

El estudiante mueve una clase a otra franja libre desde «Mis clases»; la clase aparece con su nueva fecha, hora e instructor y el saldo queda igual.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Prueba manual (ver prerrequisitos del README): en una clase de más de 12 horas tocar «Reprogramar», elegir otra franja y confirmar → «Mis clases» muestra la clase en la franja nueva, el saldo sigue igual y la franja anterior vuelve a estar libre en «Reservar». Con dos estudiantes, hacer que otro tome la franja elegida antes de confirmar → aviso «Otro estudiante ya reservó esa franja» y la clase queda en su franja original. En una clase de menos de 12 horas no aparece el botón «Reprogramar».
