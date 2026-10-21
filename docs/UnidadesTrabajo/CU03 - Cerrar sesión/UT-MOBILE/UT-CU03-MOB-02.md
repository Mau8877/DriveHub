# UT-CU03-MOB-02 — Botón "Cerrar sesión" en el `UsuarioShell`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU03-MOB-02` |
| **Nombre** | Botón "Cerrar sesión" en el `UsuarioShell` |
| **Caso de uso** | CU03-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | UT-CU03-MOB-01 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md)

## Solicitud de negocio

La persona necesita un lugar visible desde cualquier sección de la app para salir.

## Objetivo

El `AppBar` del `UsuarioShell` tiene un botón que cierra la sesión.

## Alcance

- `layouts/usuario/usuario_shell.dart`: pasar a `ConsumerStatefulWidget` (o `ConsumerState`) y agregar en `AppBar.actions` un `IconButton(icon: Icons.logout, tooltip: 'Cerrar sesión')` que llama a `ref.read(sesionProvider.notifier).cerrarSesion()`. Sin navegación manual: el redirect del router lleva a `/login`.
- Test de widget (ligero): el botón existe con su tooltip y al tocarlo llama a `cerrarSesion` una vez (provider sobrescrito con un notifier falso).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/layouts/usuario/usuario_shell.dart`
- `projects/mobile/test/layouts/usuario/usuario_shell_test.dart`

## Fuera de alcance (no tocar)

- `AdminShell`.
- Diálogo de confirmación, menú de perfil y el nombre de la persona.
- El router y el provider (UT 01).

## Resultado esperado

Desde cualquier sección del inicio se puede cerrar sesión con un toque y la app vuelve al login.

## Validación

1. `flutter test` y `flutter analyze` sin errores.
2. Manual (con el backend de CU03): iniciar sesión, tocar el botón → login; cerrar y reabrir la app → sigue en login sin recuperar la sesión.
