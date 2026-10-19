# UT-CU02-MOB-03 — `sesionProvider`: login, renovación y recuperación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU02-MOB-03` |
| **Nombre** | `sesionProvider`: login, renovación y recuperación |
| **Caso de uso** | CU02-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU02-MOB-01, UT-CU02-MOB-02 |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md)

## Solicitud de negocio

La app necesita un único lugar que sepa si hay sesión, la renueve antes de que venza y la recupere al abrir sin pedir la contraseña.

## Objetivo

Un provider de Riverpod maneja el estado de la sesión móvil.

## Alcance

- `features/auth/providers/sesion_provider.dart`: `SesionNotifier extends Notifier<EstadoSesion>` con `EstadoSesion` (`cargando` | `sinSesion` | `conSesion(usuario)`) más un `motivo` opcional cuando se pierde la sesión.
- `restaurar()` (se llama una vez al arrancar): lee `SesionStorage`; si hay refresh token, renueva con `AuthService.renovar`; si sirve, deja `conSesion`; si no, borra el almacenamiento y queda `sinSesion`.
- `iniciarSesion(email, password)`: `AuthService.login`, guarda en `SesionStorage` y programa la renovación; `ApiException` se relanza para que la pantalla la muestre.
- `renovar()`: una sola a la vez (comparte el `Future` en curso); guarda la sesión nueva (el refresh token rota) y devuelve el access token; si falla, borra la sesión y deja `sinSesion` con el `motivo` (solo para `ERR_IDE_05` a `ERR_IDE_08`) y devuelve `null`.
- Renovación proactiva con un `Timer` al 80 % de `expiraEnSegundos`; se cancela al perder la sesión y al programar otro.
- `obtenerToken()` para el interceptor.
- Tests con `ProviderContainer`, servicio y storage falsos y `fakeAsync`: login guarda y queda con sesión; restaurar con sesión guardada vigente y sin ella; renovar dos veces a la vez hace una sola llamada; la renovación ocurre al 80 %; un fallo borra la sesión y fija el motivo.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/auth/providers/sesion_provider.dart`
- `projects/mobile/test/features/auth/sesion_provider_test.dart`

## Fuera de alcance (no tocar)

- El interceptor de Dio (UT 04), el router (UT 05) y la pantalla (UT 06).
- Cerrar sesión (CU03).
- Validar roles en el cliente: lo hace el backend (`ERR_IDE_08`).

## Resultado esperado

El estado de la sesión se inicia, se recupera, se renueva solo y se pierde de forma ordenada.

## Validación

1. `flutter analyze` y `flutter test` en `projects/mobile` pasan.
