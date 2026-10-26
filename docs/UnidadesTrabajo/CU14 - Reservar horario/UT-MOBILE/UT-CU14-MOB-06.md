# UT-CU14-MOB-06 — Pestaña «Reservar»: filtros y selección

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-MOB-06` |
| **Nombre** | Pestaña «Reservar»: filtros y selección |
| **Caso de uso** | CU14-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU14-MOB-03`, `UT-CU14-MOB-04`, `UT-CU14-MOB-05` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (Cockpit: tarjetas opacas, contraste AA, nada de colores literales), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Leer `lib/features/horarios/screens/franjas_view.dart` (tira de días y lista de franjas del instructor) como referencia de interacción. Decisiones 5 y 6 del [README](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El estudiante necesita encontrar rápido el horario que le sirve, filtrando por día, momento del día e instructor, y elegir varias franjas viendo cuántas horas gasta.

## Objetivo

La pestaña «Reservar» lista las franjas libres con sus filtros, permite elegir y quitar franjas respetando las horas disponibles, y ofrece «Confirmar».

## Alcance

- `widgets/reservar_tab.dart`, que reemplaza el `EstadoVacio` de la pestaña Reservar:
  - **Día:** tira horizontal con los próximos 14 días (`lun 27`, día del mes resaltado, hoy marcado); elegir uno actualiza `reservarFechaUiProvider`.
  - **Momento del día:** chips `Todos`, `Mañana`, `Tarde`, `Noche` (`MomentoDia`).
  - **Instructor:** lista desplegable con «Todos los instructores» y los de `instructoresDisponiblesListProvider`; no se usa un `DropdownButton` nativo del sistema si el proyecto ya tiene otro selector en `shared/widgets/` (si no lo tiene, el `DropdownMenu` de Material con el tema del Cockpit).
  - **Lista de franjas** (`franjasDisponiblesListProvider`), cada una en una `FranjaCard`: hora (`08:00 – 09:00`), nombre del instructor y el estado visual de elegida / no elegida (borde y marca de verificación con colores del tema). Tocar alterna la selección con `franjasSeleccionadasUiProvider.alternar`.
  - Con 0 horas, o con tantas elegidas como horas, o con 10: las franjas no elegidas se ven deshabilitadas, y al tocarlas aparece un aviso breve («Ya elegiste todas las clases que te alcanzan» o, sin horas, «No tienes horas: compra un plan») con el acceso a `/payments/plans`.
  - **Barra inferior fija «Confirmar (N)»** (botón relleno de ámbar, **uno por pantalla**), visible solo con al menos una franja elegida; abre el resumen de la UT 07. Muestra también «Quitar todas».
  - Las franjas elegidas se conservan al cambiar de día, momento o instructor, aunque la franja ya no esté en la lista visible (la barra y el indicador siguen contándola).
  - Vacío por los filtros → `EstadoVacio` «No hay horarios libres con estos filtros» con «Quitar filtros». Carga con esqueleto. Error (red o `ERR_ACA_17`) → `AvisoCockpit` con «Reintentar».
  - Una franja del día de hoy que ya empezó según el reloj del teléfono no se muestra.
- Tests de widget: cada filtro actualiza su provider; tocar elige y quita; no deja elegir más que las horas (ni más de 10) y muestra el aviso; con 0 horas aparece el acceso a comprar; la barra «Confirmar (N)» aparece y desaparece con la selección; la selección sobrevive al cambio de filtro; vacío y error con su acción; el texto cumple el contraste del tema (no se usan colores literales).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/widgets/reservar_tab.dart`, `franja_card.dart`, `filtros_reservar.dart` (día, momento e instructor)
- `projects/mobile/lib/features/clases/screens/clases_estudiante_screen.dart` (solo para montar la pestaña)
- `projects/mobile/test/features/clases/reservar_tab_test.dart`

## Fuera de alcance (no tocar)

- El resumen, la confirmación y el resultado (UT 07): acá «Confirmar» solo abre lo que la UT 07 define.
- Mis clases e Historial (UT 05) y el indicador de horas (UT 04).
- Paginación visible o «Cargar más» (follow-up).
- Cancelar, reprogramar y filtros por vehículo.

## Resultado esperado

El estudiante filtra los horarios libres, elige las franjas que quiere viendo cuántas horas gastaría, y puede pasar a confirmarlas.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
3. Manual: con el backend arriba y franjas libres del instructor de ejemplo, filtrar por día y por «Mañana» muestra solo las del rango; elegir dos franjas con 5 horas hace que el indicador muestre «5 h → 3 h»; con 5 elegidas y 5 horas, las demás se deshabilitan.
