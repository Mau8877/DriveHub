# UT-CU02-MOB-06 — Pantalla de inicio de sesión

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-06` |
| **Nombre** | Pantalla de inicio de sesión |
| **Caso de uso** | CU02-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU02-MOB-03, UT-CU02-MOB-05 |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Es la puerta de entrada de la app: debe decir con claridad qué falló y llevar al inicio al ingresar.

## Objetivo

`LoginScreen` reemplaza a `LoginPlaceholderScreen`.

## Alcance

- `features/auth/screens/login_screen.dart` (`ConsumerStatefulWidget`): `Form` con correo y contraseña (con botón de mostrar), validadores de `registro_validators.dart` para correo y contraseña obligatorios, mensajes en español; botón con progreso mientras envía; botón secundario "Crear cuenta" hacia `/registro`.
- Al enviar llama a `sesionProvider.notifier.iniciarSesion`. Éxito → el redirect del router lleva a `/inicio` (la pantalla no navega a mano). Errores por código: `ERR_IDE_04` → "Correo o contraseña incorrectos"; `ERR_IDE_05` → el mensaje del servidor (cuándo reintentar); `ERR_IDE_06` → "Tu cuenta está inactiva. Contacta a tu administrador"; `ERR_IDE_07` → "La escuela de tu cuenta está suspendida"; `ERR_IDE_08` → el mensaje del servidor (indica la plataforma correcta); otros → aviso general sin texto técnico.
- Conserva el aviso "Tu cuenta fue creada. Inicia sesión para continuar." cuando llega con `?registrado=1` (CU01) y muestra el `motivo` de una sesión perdida si lo hay.
- Borrar `login_placeholder_screen.dart` y actualizar el router y `registro_screen_test.dart` para usar `LoginScreen`.
- Tests de widget con el servicio sustituido: formulario inválido no llama a la API; éxito llama a `iniciarSesion`; cada código de error muestra su mensaje; el aviso de cuenta creada aparece con `registrado=1`; el motivo de sesión perdida aparece.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/screens/login_screen.dart`
- `projects/mobile/lib/features/auth/screens/login_placeholder_screen.dart` (se borra)
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/test/features/auth/login_screen_test.dart`, `registro_screen_test.dart`

## Fuera de alcance (no tocar)

- Recuperar contraseña (CU04) y cerrar sesión (CU03).
- Biometría o "recordarme".
- Pantallas internas de `UsuarioShell`.

## Resultado esperado

Un estudiante o instructor inicia sesión en la app, llega al inicio y, al reabrir la app, entra sin escribir la contraseña.

## Validación

1. `flutter analyze` y `flutter test` en `projects/mobile` pasan.
2. Con el stack levantado: en el emulador, registrar un estudiante e iniciar sesión con él entra al inicio; un administrador recibe el mensaje de usar la web; cerrar y abrir la app conserva la sesión.
