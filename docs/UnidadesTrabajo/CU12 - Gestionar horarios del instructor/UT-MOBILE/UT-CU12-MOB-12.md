# UT-CU12-MOB-12 — Franjas de la semana, bloquear un día y rutas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-12` |
| **Nombre** | Franjas de la semana, bloquear un día y rutas |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | `UT-CU12-MOB-10`, `UT-CU12-MOB-11` |

## Antes de implementar

[WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [`projects/mobile/docs/design.md`](../../../../projects/mobile/docs/design.md) (secciones 4.4 y 6.9) y [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md). Leer la [decisión de diseño 12](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El instructor ve su semana de un vistazo y bloquea un día entero con un toque cuando no puede atender.

## Objetivo

La pestaña Franjas muestra la semana por días, con resumen de estados y las acciones de bloqueo.

## Alcance

- `FranjasView` (cuerpo, sin `Scaffold`; reemplaza a `FranjasScreen`): selector de semana («Semana 26 – 30 oct» con anterior y siguiente, tooltips `Semana anterior` y `Semana siguiente`), chips de resumen (N Libres, N Reservadas, N Bloqueadas) y las franjas agrupadas por día con un subtítulo («Lunes 26»), en mosaicos de dos columnas con el estado en `StatusChip`.
- Acción por franja: tocar una libre o bloqueada ofrece **Bloquear** o **Desbloquear** (mismos textos y comportamiento que hoy, incluida la regla de no ofrecer nada en reservadas ni en las que ya empezaron). Acción por día: en cada subtítulo, un botón **«Bloquear día»** o **«Desbloquear día»** según haya franjas libres o bloqueadas, con confirmación; al terminar avisa «N franjas bloqueadas» o «No había franjas libres».
- Estados: cargando, vacío («Aún no tienes franjas. Crea un horario para generarlas.»), error con «Reintentar» y pull-to-refresh.
- Router y shell: se elimina la ruta `/schedules/slots` y el redirect sigue cubriendo `/schedules/*` para quien no es instructor. `InstructorShell` no cambia.
- Tests de widget: semana con franjas de varios días, resumen, bloquear y desbloquear una franja, bloquear un día (con y sin franjas libres), navegar de semana, error de negocio del backend, vacío y reintento; el router rechaza a un estudiante en `/schedules/new`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/screens/franjas_screen.dart`
- `projects/mobile/lib/features/horarios/widgets/`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/test/features/horarios/franjas_screen_test.dart`
- `projects/mobile/test/core/app_router_test.dart`

## Fuera de alcance (no tocar)

- Mostrar el vehículo de la franja (CU10).
- Reservar o cancelar franjas (CU14, CU15).

## Resultado esperado

La pestaña Franjas es la vista semanal del diseño y permite bloquear un día completo.

## Validación

1. `flutter test` pasa con los casos nuevos en verde.
2. `flutter analyze` sin avisos.
