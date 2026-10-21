# UT-CU04b-MOB-02 — Botón "Verificar correo" en el `UsuarioShell`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-MOB-02` |
| **Nombre** | Botón "Verificar correo" en el `UsuarioShell` |
| **Caso de uso** | CU04b-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | UT-CU04b-MOB-01 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La persona debe ver que su correo falta por verificar y poder pedir el correo desde la app.

## Objetivo

El `AppBar` muestra "Verificar correo" mientras falte y se oculta al volver a la app tras verificar.

## Alcance

- `layouts/usuario/usuario_shell.dart`: en `AppBar.actions`, junto a "Cerrar sesión", un `TextButton` "Verificar correo" visible solo si `sesion.usuario?.correoVerificado == false`. Al tocarlo llama a `verificarCorreo()`, queda deshabilitado mientras corre y muestra un `SnackBar`: "Te enviamos un correo para verificar tu dirección" o "No pudimos enviar el correo. Intenta más tarde".
- El estado de la pantalla observa `WidgetsBindingObserver`: en `AppLifecycleState.resumed`, si el correo sigue sin verificar, llama a `renovar()` (así el botón desaparece después de abrir el enlace en el navegador).
- Tests de widget: el botón no aparece con el correo verificado; aparece sin verificar; tocarlo llama a `verificarCorreo` y muestra el aviso; el error muestra el otro mensaje; `resumed` renueva solo sin verificar.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/layouts/usuario/usuario_shell.dart`
- `projects/mobile/test/layouts/usuario/usuario_shell_test.dart`
- `projects/mobile/test/helpers/sesion_falsa.dart`

## Fuera de alcance (no tocar)

- `AdminShell`: la app no es para administradores.
- Mostrar la dirección de correo o un menú de perfil.
- Abrir el correo desde la app o deep links.

## Resultado esperado

Desde cualquier sección de la app se puede pedir el correo de verificación mientras falte.

## Validación

1. `flutter test` y `flutter analyze` sin errores.
2. Manual (con el backend de CU04b): pedir el correo, abrir el enlace en el navegador del teléfono y volver a la app: el botón desaparece.
