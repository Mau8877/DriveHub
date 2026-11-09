# UT-CU36-MOB-02 — Registro del dispositivo: mensajería, servicios y reglas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU36-MOB-02` |
| **Nombre** | Registro del dispositivo: mensajería, servicios y reglas |
| **Caso de uso** | CU36-MOB |
| **Microservicio** | app móvil |
| **Depende de** | UT-CU36-MOB-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (servicios por feature y GraphQL) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

El teléfono de la persona tiene que quedar registrado en Notificaciones para recibir sus avisos, sin que un fallo del push moleste a nadie.

## Objetivo

Existe la lógica que registra y quita el teléfono con las reglas de la decisión de diseño 3 a 5.

## Alcance

`lib/features/push/`:

- `services/mensajeria_push.dart`: la interfaz `MensajeriaPush` (permiso, token, token renovado, avisos en primer plano) y `MensajeriaFirebase`, su implementación sobre `firebase_messaging` (Firebase se inicia la primera vez que se usa; `getToken` y `onTokenRefresh` se mantienen a propósito, ver la decisión 8).
- `services/dispositivos_service.dart`: `DispositivosService` (REST) y `DispositivosGraphqlService` (GraphQL) con `registrar` y `quitar`; `services/documentos_graphql.dart` con las dos mutaciones.
- `services/dispositivo_push_storage.dart`: el código del dispositivo en `flutter_secure_storage` (nunca el token).
- `services/registro_de_push.dart`: `RegistroDePush` con `activar` y `desactivar`, que nunca lanzan.
- `providers/push_provider.dart`: los providers, sustituibles en los tests; el servicio según `NOTIFICACIONES_TRANSPORTE`.
- Tests: `registro_de_push_test.dart` y `dispositivos_service_test.dart`.
- Se comprobó una vez que las dos mutaciones son válidas contra el esquema real del servicio y aceptan las variables que manda la app.

**Archivos esperados (crear/tocar):** los de arriba y `test/helpers/push_falsos.dart`.

## Fuera de alcance (no tocar)

- La conexión con la sesión (UT 03).
- El backend.

## Resultado esperado

Con permiso y token, el teléfono se registra una vez por persona; sin ellos, no pasa nada.

## Validación

1. `flutter test test/features/push/registro_de_push_test.dart test/features/push/dispositivos_service_test.dart` pasa.
2. Sin permiso o sin Firebase, `activar` termina sin lanzar y no vuelve a pedir el permiso.
3. `desactivar` espera a un registro en curso, así no queda un dispositivo huérfano.
