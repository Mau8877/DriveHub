# UT-CU02-MOB-04 — Interceptor de sesión en `ApiClient` (Bearer y reintento por 401)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-04` |
| **Nombre** | Interceptor de sesión en `ApiClient` (Bearer y reintento por 401) |
| **Caso de uso** | CU02-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU02-MOB-03 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

Toda llamada de la app debe llevar la sesión y, si recibe un rechazo por sesión vencida, renovarla una vez y repetirse.

## Objetivo

`ApiClient` agrega el Bearer y reintenta una vez tras renovar, sin que `core/` importe nada de `features/`.

## Alcance

- `core/network/api_client.dart`: `ApiClient.configurarSesion({obtenerToken, renovar})` guarda dos callbacks (`String? Function()` y `Future<String?> Function()`); `core/` no importa `features/`. Un interceptor nuevo (antes del que desempaqueta el sobre): agrega `Authorization: Bearer` si hay token, salvo en las rutas que empiezan con `/auth/` (login y renovación no llevan Bearer).
- Ante un `401` de una petición no reintentada (marca en `extra`) y que no sea de `/auth/`: llama a `renovar()`; con token nuevo repite la petición con ese Bearer; con `null` rechaza con el error original. Nunca más de un reintento.
- `main.dart`/`MyApp`: al construir, conecta `ApiClient.configurarSesion` con `sesionProvider` (token y `renovar`), y llama a `restaurar()` una vez.
- Tests con el adaptador de Dio falso: el Bearer se agrega; no se agrega a `/auth/login`; 401 → renueva → repite con el token nuevo; 401 con renovación `null` → error sin repetir; sin bucle en un segundo 401.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/core/network/api_client.dart`
- `projects/mobile/lib/main.dart`
- `projects/mobile/test/core/api_client_sesion_test.dart`

## Fuera de alcance (no tocar)

- Mover lógica de sesión a `core/`.
- El router (UT 05).
- Cambiar el desempaquetado del sobre ni `ApiException`.

## Resultado esperado

Las llamadas de la app llevan la sesión y se recuperan solas de un 401.

## Validación

1. `flutter analyze` y `flutter test` en `projects/mobile` pasan.
