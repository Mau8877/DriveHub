# UT-CU06-MOB-06 — Cambiar la contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-MOB-06` |
| **Nombre** | Cambiar la contraseña |
| **Caso de uso** | CU06-MOB |
| **Microservicio** | `projects/mobile` (feature `perfil`, `core/routing`) |
| **Depende de** | UT-CU06-MOB-03, UT-CU06-MOB-04 |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La persona cambia su contraseña desde la app sin cerrar su sesión.

## Objetivo

La pantalla "Cambiar contraseña" (flujo F3) en `/perfil/contrasena`.

## Alcance

- `lib/features/perfil/screens/cambiar_contrasena_screen.dart`: `Form` con contraseña actual, nueva y confirmación (`obscureText` con botón para mostrar u ocultar). Reglas de la nueva **reutilizadas del registro** (9 a 64 caracteres); la confirmación debe coincidir y la nueva no puede ser igual a la actual (validación local; el backend lo reafirma con `ERR_IDE_15`).
- Envía a `perfilProvider.cambiarContrasena` (con `dispositivo: 'MOVIL'`); indicador de progreso y botón deshabilitado mientras envía.
  - Éxito: `SnackBar` "Contraseña actualizada. Cerramos tus otras sesiones." y vuelve al perfil. La sesión del teléfono **no** se cierra ni se renueva.
  - `ERR_IDE_14` → "La contraseña actual no es correcta" bajo el primer campo; `ERR_IDE_15` y otros errores con su mensaje en español, sin vaciar lo escrito.
- `lib/features/perfil/validators/perfil_validators.dart`: validadores `contrasenaNueva`, `confirmacion` y `distintaDeLaActual`.
- `lib/core/routing/app_router.dart`: `GoRoute` `/perfil/contrasena` (no es pública: la cubre el redirect global por sesión). El botón de `PerfilScreen` (UT 04) navega ahí.
- Tests: los validadores (largo, confirmación distinta, igual a la actual) y el mapeo de `ERR_IDE_14`/`ERR_IDE_15`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/perfil/screens/cambiar_contrasena_screen.dart`
- `projects/mobile/lib/features/perfil/validators/perfil_validators.dart`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/test/features/perfil/perfil_validators_test.dart`

## Fuera de alcance (no tocar)

- Recuperar la contraseña sin la actual (CU04).
- Cerrar la sesión del teléfono o pedir un nuevo login.
- Medidor de fortaleza de contraseña.

## Resultado esperado

La persona cambia su contraseña con la actual y sigue con la sesión abierta en el teléfono.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
2. Manual (con el backend de CU06): cambiar con la actual incorrecta (error) y correcta (aviso); la web abierta con la misma cuenta pierde su sesión al intentar renovar.
