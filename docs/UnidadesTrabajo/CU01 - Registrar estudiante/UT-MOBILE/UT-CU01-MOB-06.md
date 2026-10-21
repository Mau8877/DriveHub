# UT-CU01-MOB-06 — Pantalla de registro y aviso en el login

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-MOB-06` |
| **Nombre** | Pantalla de registro y aviso en el login |
| **Caso de uso** | CU01-MOB |
| **Aplicación** | `mobile` |
| **Depende de** | UT-CU01-MOB-02, UT-CU01-MOB-03, UT-CU01-MOB-05 |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (`Form` + `TextFormField` + `validator`, errores en español), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (widgets de una sola feature viven en la feature; tema desde `ThemeData`, sin colores propios), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) y [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md).

## Solicitud de negocio

Es la pantalla que ve el estudiante en la app: debe guiarlo campo por campo y, al terminar, llevarlo al login.

## Objetivo

Existe `RegistroScreen` conectada al provider, a las reglas de validación y a la navegación.

## Alcance

- `lib/features/auth/screens/registro_screen.dart` (`ConsumerStatefulWidget`) con `Form` y `TextFormField` para: nombres, apellidos, fecha de nacimiento (selector `showDatePicker`, de solo lectura en el campo), correo (teclado de correo), contraseña (oculta, con botón de mostrar), teléfono (opcional, teclado numérico) y código de escuela. Validadores de `registro_validators.dart`; mensajes en español inline.
- Botón "Registrarme" que valida el formulario, arma `RegistroEstudianteRequest` (con el código de escuela en minúsculas y sin espacios) y llama a `registroProvider.registrar`; se deshabilita y muestra progreso mientras `AsyncLoading`.
- Errores del backend mapeados con `errorDeRegistro`: `correoEnUso` bajo el correo ("Este correo ya está registrado"), `menorDeEdad` bajo la fecha ("Debes tener al menos 18 años"), `escuelaNoDisponible` bajo el código de escuela ("La escuela no existe o no está disponible"), los demás en un aviso general (`SnackBar`) sin texto técnico.
- Éxito (CA02): `context.go('/login?registrado=1')` y `LoginPlaceholderScreen` muestra un `SnackBar`/aviso "Tu cuenta fue creada. Inicia sesión para continuar." cuando recibe ese parámetro.
- Reemplaza el marcador temporal de `/register` de UT 02 por esta pantalla.
- Test de widget (opcional según TESTING_MOBILE.md, pero se hace uno para el flujo crítico): con el service sustituido, un formulario válido llama a `registrar` y navega al login; con correo en uso aparece el mensaje bajo el campo.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/screens/registro_screen.dart`
- `projects/mobile/lib/features/auth/screens/login_placeholder_screen.dart`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/test/features/auth/registro_screen_test.dart`

## Fuera de alcance (no tocar)

- El login real, tokens y `flutter_secure_storage` (CU02).
- Consultar el nombre de la escuela antes de enviar.
- Widgets en `shared/widgets/` (aún no los comparte una segunda feature).
- Cambios en la paleta o el tema.

## Resultado esperado

Con el backend levantado, un estudiante completa el formulario con el código `escuelaexample`, se registra y llega al login con el aviso.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
2. Con el stack levantado y el backend aplicado: en el emulador, registrar un estudiante mayor de 18 con el código `escuelaexample` → vuelve al login con el aviso, y en la base de Identidad existe el usuario con rol `ESTUDIANTE`.
3. Con un código inexistente aparece el error bajo "Código de escuela"; con una fecha de hace menos de 18 años, bajo la fecha; con un correo repetido, bajo el correo.
