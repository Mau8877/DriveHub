# UT-CU45-MOB-03 — Bandera de transporte y cableado de los providers

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-MOB-03` |
| **Nombre** | Bandera de transporte y cableado de los providers |
| **Caso de uso** | CU45-MOB |
| **Microservicio** | app móvil |
| **Depende de** | UT-CU45-MOB-02 |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) (providers de servicio sustituibles en tests).

## Solicitud de negocio

Si el backend no tiene GraphQL encendido o algo falla, el equipo tiene que poder volver a REST sin recompilar la app.

## Objetivo

Un valor del `.env` decide si los avisos y las preferencias se piden por GraphQL o por REST.

## Alcance

- `lib/core/network/transporte.dart`: `enum Transporte { rest, graphql }` y `transporteDesde(String?)` (GraphQL salvo que diga `rest`, sin importar mayúsculas ni espacios).
- `features/notificaciones/providers/transporte_provider.dart`: `transporteNotificacionesProvider`, que lee `NOTIFICACIONES_TRANSPORTE` del `.env` (sin `.env` cargado, GraphQL).
- `notificacionesServiceProvider` y `preferenciasServiceProvider` entregan la versión de GraphQL o la de REST según el transporte.
- `projects/mobile/.env.example`: la variable documentada.
- Tests: `transporte` en `notificaciones_graphql_test.dart` (valores por defecto, `rest`, lectura del `.env`, qué servicio entrega cada provider).

**Archivos esperados (crear/tocar):** `transporte.dart`, `transporte_provider.dart`, `bandeja_providers.dart`, `notificaciones_providers.dart`, `.env.example`.

## Fuera de alcance (no tocar)

- Una bandera para otras features: se agrega con la primera feature que se migre.
- Cambiar el transporte con la app corriendo (se lee al crear el provider).

## Resultado esperado

Sin tocar nada la app usa GraphQL; con `NOTIFICACIONES_TRANSPORTE=rest` usa las rutas de siempre.

## Validación

1. `flutter test` completo pasa y `flutter analyze` no tiene hallazgos.
2. Con `NOTIFICACIONES_TRANSPORTE=rest` en el `.env`, `notificacionesServiceProvider` entrega el servicio de REST.
3. Con el backend levantado (`docker compose up -d notificaciones`), abrir «Notificaciones» y «Preferencias» en la app muestra los mismos datos que con `rest`.
