# UT-CU06-MOB-04 — Avatar compartido y pantalla de perfil (datos personales)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-MOB-04` |
| **Nombre** | Avatar compartido y pantalla de perfil (datos personales) |
| **Caso de uso** | CU06-MOB |
| **Microservicio** | `projects/mobile` (`shared`, `layouts/usuario`, feature `perfil`) |
| **Depende de** | UT-CU06-MOB-03 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La persona ve su avatar en la barra y, desde la pestaña "Perfil", sus datos y puede corregir los personales.

## Objetivo

El avatar compartido, la pantalla de perfil dentro de la pestaña "Perfil" y la edición de nombres, apellidos, teléfono y fecha de nacimiento (flujos "ver" y F1).

## Alcance

- `lib/shared/widgets/avatar_usuario.dart` (`AvatarUsuario`, crea la carpeta `shared/widgets/`): círculo con `Image.network(fotoUrl)` o, sin foto o si la imagen falla (`errorBuilder`), las iniciales de nombres y apellidos con los colores de `AppColors`/`Theme`. Parámetros: `nombres`, `apellidos`, `fotoUrl`, `radio`.
- `lib/layouts/usuario/usuario_shell.dart`: el `AppBar` muestra `AvatarUsuario` (leyendo `sesionProvider`) junto a las acciones actuales; al tocarlo, selecciona la pestaña "Perfil". El cuerpo de la pestaña "Perfil" pasa a ser `PerfilScreen` (las otras pestañas no cambian).
- `lib/features/perfil/screens/perfil_screen.dart`: lee `perfilProvider` (cargando, error con "Reintentar"). Muestra: bloque de solo lectura (correo con "Verificado"/"Sin verificar", rol legible, escuela si tiene) y el formulario de datos personales (`Form` + `TextFormField` y selector de fecha) con "Guardar cambios" (deshabilitado sin cambios o mientras guarda; solo manda lo modificado; teléfono vaciado → `null`). Éxito: `SnackBar` "Datos guardados". `ApiException` `ERR_IDE_02` → "Debes tener al menos 18 años" bajo la fecha; otros códigos con su mensaje en español. Deja los accesos a "Cambiar contraseña" y "Cambiar correo" (botones que navegan a `/perfil/contrasena` y `/perfil/correo`, que crean las UT 06 y 07) y un lugar para la sección de la foto (UT 05).
- `lib/features/perfil/validators/perfil_validators.dart`: validadores de nombres, apellidos, teléfono y fecha **reutilizando los límites y reglas de `features/auth/validators/registro_validators.dart`** (80, 80, 20, edad ≥ 18); mensajes en español.
- Tests: `perfil_validators` (límites, vacío, edad mínima, coincidencia con las reglas del registro). No se exigen widget tests de la pantalla.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/shared/widgets/avatar_usuario.dart`
- `projects/mobile/lib/layouts/usuario/usuario_shell.dart`
- `projects/mobile/lib/features/perfil/screens/perfil_screen.dart`
- `projects/mobile/lib/features/perfil/validators/perfil_validators.dart`
- `projects/mobile/test/features/perfil/perfil_validators_test.dart`

## Fuera de alcance (no tocar)

- La foto, la contraseña y el correo (UT 05 a 07).
- `AdminShell` y las otras pestañas del `UsuarioShell`.
- Editar el correo, el rol o la escuela desde este formulario.

## Resultado esperado

En la pestaña "Perfil" la persona ve su cuenta y guarda cambios en sus datos personales; el avatar aparece en la barra.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
2. Manual (con el backend de CU06): iniciar sesión con un estudiante, abrir "Perfil", cambiar nombres y ver la barra actualizada; poner una fecha que dé menos de 18 años y ver el error.
