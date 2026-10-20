# UT-CU04-MOB-01 — Pantalla "Recuperar contraseña" y su servicio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04-MOB-01` |
| **Nombre** | Pantalla "Recuperar contraseña" y su servicio |
| **Caso de uso** | CU04-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La persona que olvidó su contraseña puede pedir el enlace desde la app.

## Objetivo

Existe `RecuperarScreen`, que pide el correo y muestra siempre el mismo mensaje.

## Alcance

- `features/auth/services/auth_service.dart`: `Future<void> recuperar(String email)` → `POST /auth/recuperar` (público, mismo manejo de `ApiException`).
- `features/auth/providers/recuperar_provider.dart`: `AsyncNotifier` que envuelve la llamada y expone `AsyncValue<void>` (idle / cargando / enviado / error).
- `features/auth/screens/recuperar_screen.dart`: `Form` con un campo de correo (obligatorio y formato válido, en español, la misma regla que el login/registro), botón "Enviar enlace" deshabilitado mientras carga. Al enviar muestra el mensaje fijo "Si el correo está registrado y verificado, te enviamos un enlace. Revisa tu bandeja y ábrelo en tu navegador." y, siempre visible, "La recuperación solo funciona si verificaste tu correo desde la app." Un error de red muestra "No pudimos procesar la solicitud. Intenta más tarde." Botón "Volver a iniciar sesión".
- Tests: el servicio pega a la ruta con `{ email }`; el formulario no envía vacío ni con formato inválido; con uno válido llama al servicio y muestra el mensaje fijo; el error de red muestra el otro mensaje.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/services/auth_service.dart`
- `projects/mobile/lib/features/auth/providers/recuperar_provider.dart`
- `projects/mobile/lib/features/auth/screens/recuperar_screen.dart`
- `projects/mobile/test/features/auth/auth_service_test.dart`
- `projects/mobile/test/features/auth/recuperar_screen_test.dart`

## Fuera de alcance (no tocar)

- La ruta del router y el enlace en el login (UT 02).
- Pantalla de nueva contraseña: es la página web del enlace.
- Deep links.

## Resultado esperado

Existe la pantalla que pide el enlace de recuperación y siempre responde lo mismo.

## Validación

1. `flutter test` y `flutter analyze` sin errores.
