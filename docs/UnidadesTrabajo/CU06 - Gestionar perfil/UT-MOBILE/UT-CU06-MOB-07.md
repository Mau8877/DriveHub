# UT-CU06-MOB-07 — Cambiar el correo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU06-MOB-07` |
| **Nombre** | Cambiar el correo |
| **Caso de uso** | CU06-MOB |
| **Microservicio** | `projects/mobile` (feature `perfil`, `core/routing`) |
| **Depende de** | UT-CU06-MOB-03, UT-CU06-MOB-04 |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La persona pide cambiar su correo desde la app, abre el enlace en el navegador del teléfono y, al volver, ve su correo nuevo.

## Objetivo

La pantalla "Cambiar correo" (flujo F4) en `/perfil/correo`.

## Alcance

- `lib/features/perfil/screens/cambiar_correo_screen.dart`: muestra el correo actual y un `Form` con contraseña actual y correo nuevo (formato válido, ≤ 120 caracteres; reglas del registro).
  - Envía a `perfilProvider.solicitarCambioCorreo`; indicador de progreso y botón deshabilitado mientras envía.
  - Éxito: mensaje "Te enviamos un correo a {correoNuevo}. Tu correo no cambia hasta que abras el enlace (vale 24 horas)." y el formulario queda en ese estado con "Pedir otro enlace".
  - Errores: `ERR_IDE_14` bajo la contraseña; `ERR_IDE_01` ("Ese correo ya está en uso") y `ERR_IDE_18` ("Ese ya es tu correo") bajo el correo nuevo; un fallo del servidor de correo (500) como "No pudimos enviar el correo. Intenta más tarde".
- **Ver el correo nuevo al volver (CA18):** con una solicitud pendiente, al pasar a `AppLifecycleState.resumed` (`WidgetsBindingObserver`, como el `UsuarioShell` de CU04b) llama a `perfilProvider.recargar()`; si el correo cambió, muestra "Tu correo ahora es {correo}" y quita el estado pendiente. Como la cuenta pasa a verificada, el `UsuarioShell` ya renueva la sesión al volver si el correo estaba sin verificar (CU04b) y el botón "Verificar correo" desaparece.
- `lib/core/routing/app_router.dart`: `GoRoute` `/perfil/correo` (cubierta por el redirect global). El botón de `PerfilScreen` (UT 04) navega ahí.
- Tests: validadores de correo nuevo (formato y largo) y el mapeo de `ERR_IDE_01`/`ERR_IDE_14`/`ERR_IDE_18` a su mensaje; la lógica que decide si el correo "cambió" al recargar (función pura).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/perfil/screens/cambiar_correo_screen.dart`
- `projects/mobile/lib/features/perfil/validators/perfil_validators.dart`
- `projects/mobile/lib/core/routing/app_router.dart`
- `projects/mobile/test/features/perfil/perfil_validators_test.dart`

## Fuera de alcance (no tocar)

- La página que abre el enlace (es de la web, UT-CU06-FE-09) y los deep links.
- Cambiar el correo sin confirmar.
- El botón "Verificar correo" del `UsuarioShell` (CU04b).

## Resultado esperado

La persona pide el cambio, ve qué falta hacer y, al volver a la app tras abrir el enlace, ve su correo nuevo.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` pasan.
2. Manual (con el backend de CU06): pedir el cambio, abrir el correo en Mailpit desde el navegador del teléfono, tocar el enlace y volver a la app: el correo nuevo aparece sin cerrar sesión.
