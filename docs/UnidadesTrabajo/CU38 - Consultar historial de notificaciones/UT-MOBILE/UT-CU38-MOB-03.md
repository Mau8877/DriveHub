# UT-CU38-MOB-03 — Campana con contador en la barra superior

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU38-MOB-03` |
| **Nombre** | Campana con contador en la barra superior |
| **Caso de uso** | CU38-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU38-MOB-01, UT-CU38-MOB-02 |

## Antes de implementar

Leer [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md). Ver `layouts/shared/shell_con_barra.dart`, donde la campana de «Inicio» hoy muestra un snackbar.

## Solicitud de negocio

La persona sabe, sin abrir nada, si tiene avisos sin leer, y llega a ellos con un toque.

## Objetivo

La campana de la barra superior abre la bandeja y muestra cuántos avisos sin leer hay.

## Alcance

- `lib/features/notificaciones/widgets/campana_avisos.dart`: el `IconButton` de la campana con su insignia (`Badge` de Material) con el valor de `conteoNoLeidasProvider`: oculta con 0 o `null`, y «99+» desde 100. Al pulsarla navega a `/notificaciones` (`context.push`). Tooltip «Avisos», con el contador en la etiqueta de accesibilidad (`Avisos, 3 sin leer`).
- `layouts/shared/shell_con_barra.dart`: reemplaza el `IconButton` de «Avisos» y su snackbar por `CampanaAvisos`. Sigue en la sección «Inicio», tanto en el shell del estudiante como en el del instructor.
- El contador se refresca al abrir la app y al volver de `/notificaciones` (el `push` espera el regreso e invalida `conteoNoLeidasProvider`), y cuando la app vuelve del segundo plano (`AppLifecycleState.resumed`, ya observado por el shell).
- Tests: sin avisos no hay insignia; con 3 muestra «3»; con 120 muestra «99+»; un fallo del contador no muestra insignia ni error; pulsar la campana abre `/notificaciones` y, al volver, pide el contador de nuevo; el texto accesible incluye la cantidad.

Archivos esperados (crear/tocar): `campana_avisos.dart`, `shell_con_barra.dart` y los tests.

## Fuera de alcance (no tocar)

- La pantalla de la bandeja (UT-CU38-MOB-02).
- Cambiar la barra inferior ni agregar una pestaña.
- Actualizar el contador en vivo con un push (depende del token de Firebase, CU36 móvil).
- Cualquier cambio de los demás elementos de la barra (saldo de horas, avatar, aviso de correo).

## Resultado esperado

Una persona con dos avisos sin leer ve la campana con un «2»; la toca, abre uno, vuelve y la campana muestra «1».

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` en verde.
2. En un emulador contra el stack: enviar dos avisos, abrir la app (insignia «2»), leer uno, volver (insignia «1»), «Marcar todas» y volver (sin insignia).
