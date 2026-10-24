# UT-CU12-MOB-11 — Pantalla «Horarios» con pestañas y formulario «Nuevo horario»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-11` |
| **Nombre** | Pantalla «Horarios» con pestañas y formulario «Nuevo horario» |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | `UT-CU12-MOB-10` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) y la guía visual [`projects/mobile/docs/design.md`](../../../../projects/mobile/docs/design.md) (secciones 6.7 y 6.8). Leer las [decisiones de diseño 10 y 11](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El instructor ve sus horarios con su estado, los pausa o reanuda desde la lista y crea uno nuevo eligiendo varios días de una vez.

## Objetivo

La sección «Horarios» y el formulario siguen el diseño de la propuesta B con el contrato nuevo.

## Alcance

- `HorariosScreen` (cuerpo, sin `Scaffold`): control segmentado **Plantillas | Franjas** arriba (la pestaña Franjas muestra el cuerpo de la UT 12), el interruptor «Ver inactivos» (clave `interruptorInactivos`, solo en Plantillas) y el botón ámbar **«Nuevo horario»** fijo abajo (clave `botonNuevoHorario`, solo en Plantillas). Se elimina el botón «Ver franjas».
- Tarjeta de horario: nombre, estado como `StatusChip` (Activa, Pausada, Inactiva), los siete círculos de día (activos en `segmentActive`), `HH:mm – HH:mm · N min` en azul de texto y la vigencia. Pausada e inactiva se dibujan como tarjeta vacía (borde punteado, sin sombra). Menú de opciones (tooltip «Opciones del horario»): en activa **Pausar** y **Desactivar**; en pausada **Reanudar** y **Desactivar**; en inactiva ninguno. Pausar y desactivar piden confirmación; reanudar no. Mensajes: «Horario pausado», «Horario reanudado», «Horario desactivado»; los errores del backend se muestran tal cual.
- Estados: cargando (esqueletos), vacío («Todavía no tienes horarios. Crea el primero.» con «Nuevo horario»), error con «Reintentar».
- `HorarioFormScreen`: campos Nombre, selector de los siete días (círculos de 44 dp, alternables, claves `dia1` a `dia7`), Desde las / Hasta las (selector de hora 24 h), duración segmentada 45 / 60 / 90 min, Vigente desde y Hasta (opcional), la **vista previa** en tarjeta ámbar suave («Se crearán N franjas por día, M por semana.», recalculada en vivo) y el botón ámbar «Guardar horario» (clave `botonGuardarHorario`, «Guardando…» al enviar). Mantiene las claves de campo actuales (`campoHoraInicio`, `campoHoraFin`, `campoDesde`, `campoHasta`) y reemplaza `campoDia` y `campoDuracion`. Validación al enviar con los mensajes de la UT 10 y el error del backend sobre el botón.
- Tests de widget: pestañas, tarjeta por estado, menú por estado, confirmaciones, vacío y error; formulario con varios días, vista previa que cambia, errores de cada campo y envío con el pedido esperado.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/screens/horarios_screen.dart`
- `projects/mobile/lib/features/horarios/screens/horario_form_screen.dart`
- `projects/mobile/lib/features/horarios/widgets/dias_semana.dart`
- `projects/mobile/test/features/horarios/horarios_screen_test.dart`
- `projects/mobile/test/features/horarios/horario_form_screen_test.dart`

## Fuera de alcance (no tocar)

- La vista de franjas (UT 12).
- El shell y las rutas (UT 12).

## Resultado esperado

La pantalla de horarios y su formulario son los del diseño y manejan varios días, estados y vista previa.

## Validación

1. `flutter test` pasa con los casos nuevos en verde.
2. `flutter analyze` sin avisos.
