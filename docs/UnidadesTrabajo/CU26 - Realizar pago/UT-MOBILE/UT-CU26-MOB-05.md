# UT-CU26-MOB-05 — Rutas, sección «Pagos» del estudiante y control de rol

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-MOB-05` |
| **Nombre** | Rutas, sección «Pagos» del estudiante y control de rol |
| **Caso de uso** | CU26-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | UT-CU26-MOB-04 |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

El estudiante llega a la compra desde el menú, y quien no es estudiante no puede entrar.

## Objetivo

Las rutas de la compra, la pantalla base de la sección «Pagos» y el control de rol.

## Alcance

- `features/pagos/screens/pagos_screen.dart`: la pantalla de la sección «Pagos» del `EstudianteShell`. Por ahora tiene una tarjeta **Comprar horas** (`FilaAccion`) que va a `/payments/plans` y un `EstadoVacio` ("Todavía no tienes compras"); CU28 reemplaza el estado vacío por el historial y deja la tarjeta como acción.
- `layouts/estudiante/estudiante_shell.dart`: la sección «Pagos» pasa a usar `PagosScreen` (hoy muestra el cuerpo de prueba con la nota 'CU26 y CU28').
- `core/routing/app_router.dart`: las rutas `/payments/plans`, `/payments/summary`, `/payments/result` y `/payments/return` (el destino del enlace `drivehub://app/payments/return`, que **redirige** a `/payments/result`), todas **solo para el rol `ESTUDIANTE`**: con otro rol, el redirect global lleva a `/inicio` (mismo patrón que las rutas de horarios, que son solo de `INSTRUCTOR`); sin sesión, a `/login`. El resumen y el resultado sin una compra en curso **no** se redirigen desde el router (se reevaluaría al renovarse la sesión y sacaría al estudiante de la pantalla): cada una muestra su estado "No hay una compra en curso" con una salida.
- Tests del router: estudiante entra a las cuatro rutas; instructor y administrador son redirigidos; sin sesión va a `/login`; el resumen sin plan elegido vuelve a la lista.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/pagos/screens/pagos_screen.dart
- projects/mobile/lib/layouts/estudiante/estudiante_shell.dart
- projects/mobile/lib/core/routing/app_router.dart
- projects/mobile/test/features/pagos/rutas_pagos_test.dart

## Fuera de alcance (no tocar)

- El historial y el detalle de pagos (CU28).
- Cambios en el shell del instructor (CU29).

## Resultado esperado

Desde «Pagos» el estudiante empieza una compra y llega al resultado; otros roles no pueden entrar a esas rutas.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. Con la cuenta de estudiante de ejemplo, «Pagos» → «Comprar horas» lista los 5 planes del seeder; con la de instructor, esa ruta redirige a «Inicio».
