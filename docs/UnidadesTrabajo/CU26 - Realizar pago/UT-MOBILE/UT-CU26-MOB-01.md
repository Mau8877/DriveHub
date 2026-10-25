# UT-CU26-MOB-01 — Enlace de retorno y apertura de la página de pago

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU26-MOB-01` |
| **Nombre** | Enlace de retorno y apertura de la página de pago |
| **Caso de uso** | CU26-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

Al pagar, el estudiante sale a la página de la pasarela y tiene que poder volver a la app, también desde el enlace de la página de retorno.

## Objetivo

La app puede abrir una dirección web en una pestaña del navegador y reconoce el enlace `drivehub://app/payments/return`.

## Alcance

- Dependencia `url_launcher` en `pubspec.yaml` (versión fijada).
- Android (`android/app/src/main/AndroidManifest.xml`): un `intent-filter` para el esquema `drivehub` con host `app`, y la declaración `queries` para abrir enlaces `https`. iOS (`ios/Runner/Info.plist`): el esquema `drivehub` en `CFBundleURLTypes` y `LSApplicationQueriesSchemes` con `https`.
- `core/routing/app_router.dart`: el enlace `drivehub://app/payments/return` llega a la ruta `/payments/return`, que **redirige** a `/payments/result` (la pantalla de la UT 04). Sin una compra en curso, redirige a `/inicio`. Es la misma ruta de [UT-CU26-BE-07](../UT-BACKEND/UT-CU26-BE-07.md): el enlace de la página de retorno del backend.
- `core/network/abrir_pagina_de_pago.dart`: `Future<bool> abrirPaginaDePago(String url)` abre la dirección con `launchUrl(..., mode: LaunchMode.inAppBrowserView)` (pestaña del navegador dentro de la app); solo acepta `https` (cualquier otra dirección se rechaza y devuelve `false`) y atrapa los errores de la plataforma.
- Test del router: el enlace con y sin compra en curso redirige donde corresponde; test de `abrirPaginaDePago` con `UrlLauncherPlatform` falso: rechaza `http`, `javascript:` y vacío; acepta `https`.

Archivos esperados (crear/tocar):

- projects/mobile/pubspec.yaml
- projects/mobile/android/app/src/main/AndroidManifest.xml
- projects/mobile/ios/Runner/Info.plist
- projects/mobile/lib/core/routing/app_router.dart
- projects/mobile/lib/core/network/abrir_pagina_de_pago.dart
- projects/mobile/test/core/abrir_pagina_de_pago_test.dart
- projects/mobile/test/core/app_router_pagos_test.dart

## Fuera de alcance (no tocar)

- Las pantallas de compra (UT 04).
- Servicios y providers de pagos (UT 02 y 03).
- Un visor web propio o `webview_flutter`.

## Resultado esperado

La app abre la página de pago de Stripe dentro de una pestaña del navegador y vuelve a ella desde el enlace de retorno.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
2. En un emulador, abrir `adb shell am start -a android.intent.action.VIEW -d drivehub://app/payments/return` lleva a la app.
