# UT-CU04b-MOB-01 — Sesión con `correoVerificado` y pedir la verificación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU04b-MOB-01` |
| **Nombre** | Sesión con `correoVerificado` y pedir la verificación |
| **Caso de uso** | CU04b-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La app necesita saber si el correo está verificado y poder pedir el correo de verificación.

## Objetivo

`sesionProvider.notifier.verificarCorreo()` pide el correo y la sesión se puede refrescar.

## Alcance

- `features/auth/models/usuario_sesion.dart`: campo `correoVerificado` (`bool`); `fromJson` lee `json['correoVerificado'] as bool? ?? false` (las sesiones ya guardadas en el teléfono no lo traen) y `toJson` lo escribe.
- `features/auth/services/auth_service.dart`: `Future<void> solicitarVerificacionCorreo()` → `POST /auth/verificar-correo` sin cuerpo (el Bearer lo pone el interceptor), mismo manejo de `ApiException` que los demás métodos.
- `features/auth/providers/sesion_provider.dart`: `Future<void> verificarCorreo()` llama al servicio; si responde `ApiException` `ERR_IDE_10` llama a `renovar()` para actualizar la sesión y no relanza; otro error se relanza. Para refrescar al volver a la app se reutiliza `renovar()` (ya público).
- Tests: el modelo parsea y escribe el campo (con y sin él en el JSON); `solicitarVerificacionCorreo` pega a la ruta correcta; `ERR_IDE_10` renueva sin error; otro error se relanza.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/models/usuario_sesion.dart`
- `projects/mobile/lib/features/auth/services/auth_service.dart`
- `projects/mobile/lib/features/auth/providers/sesion_provider.dart`
- `projects/mobile/test/features/auth/auth_service_test.dart`
- `projects/mobile/test/features/auth/sesion_provider_test.dart`
- `projects/mobile/test/helpers/sesion_falsa.dart`

## Fuera de alcance (no tocar)

- El botón y el ciclo de vida de la app (UT 02).
- La página que confirma el enlace: es web.

## Resultado esperado

`SesionNotifier` expone si el correo está verificado y puede pedir el correo de verificación.

## Validación

1. `flutter test` y `flutter analyze` sin errores.
