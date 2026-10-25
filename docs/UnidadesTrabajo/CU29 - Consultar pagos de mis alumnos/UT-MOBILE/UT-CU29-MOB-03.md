# UT-CU29-MOB-03 — Pantallas «Mis alumnos» y detalle del alumno, y sección del instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-MOB-03` |
| **Nombre** | Pantallas «Mis alumnos» y detalle del alumno, y sección del instructor |
| **Caso de uso** | CU29-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU29-MOB-02 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El instructor ve a sus alumnos con las horas y el tiempo que ha estado con cada uno.

## Objetivo

La sección «Alumnos» del instructor y la pantalla de detalle de cada alumno.

## Alcance

- `layouts/instructor/instructor_shell.dart`: la sección «Pagos» (cuerpo de prueba, 'CU29') pasa a llamarse **«Alumnos»**, con el icono `groups_outlined`, y usa `MisAlumnosScreen`. Se actualiza la nota de la sección y los tests del shell si los hay.
- `MisAlumnosScreen`: campo de búsqueda con icono, y la lista de alumnos (`misAlumnosProvider`) con el nombre y tres datos por alumno: **comprado**, **conmigo** y **restantes** (en horas); «Cargar más» al final; `RefreshIndicator`; estado vacío ("Todavía no tienes alumnos asignados" o "Sin resultados" según haya búsqueda); error con reintento.
- `AlumnoDetalleScreen` (`/students/:codigo`, solo `INSTRUCTOR`): arriba el nombre y el resumen (comprado, conmigo, restantes); luego **«Tiempo conmigo»**: las clases (fecha y horas), de la más reciente a la más antigua, o un aviso si todavía no hay; luego **«Pagos»**: plan, horas, fecha y estado (`Insignia`) de cada compra, sin acciones. Un `404` vuelve a la lista con un aviso; `503` muestra "No pudimos cargar los datos, inténtalo más tarde".
- `core/routing/app_router.dart`: la ruta `/students/:codigo`, con el control de rol de las rutas del instructor (otros roles → `/inicio`).
- Tests de widget: la lista y los tres datos; la búsqueda se aplica; vacío con y sin búsqueda; el detalle muestra el tiempo y los pagos; un alumno sin clases muestra el aviso; el instructor sin sesión o con otro rol no entra al detalle.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/alumnos/screens/mis_alumnos_screen.dart
- projects/mobile/lib/features/alumnos/screens/alumno_detalle_screen.dart
- projects/mobile/lib/layouts/instructor/instructor_shell.dart
- projects/mobile/lib/core/routing/app_router.dart
- projects/mobile/test/features/alumnos/mis_alumnos_screen_test.dart
- projects/mobile/test/features/alumnos/alumno_detalle_screen_test.dart

## Fuera de alcance (no tocar)

- El botón del comprobante (CU30).
- Acciones sobre pagos: la pantalla es de solo lectura.
- Los datos de contacto del alumno.

## Resultado esperado

El instructor ve, busca y abre a sus alumnos con sus horas, el tiempo conjunto y los pagos de cada uno.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Con la cuenta del instructor de ejemplo y alumnos asignados de prueba, «Alumnos» los lista con sus horas; un estudiante o administrador no entra a `/students/...`.
