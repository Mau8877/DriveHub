# UT-CU45-MOB-02 — Servicios de avisos y preferencias por GraphQL

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU45-MOB-02` |
| **Nombre** | Servicios de avisos y preferencias por GraphQL |
| **Caso de uso** | CU45-MOB |
| **Microservicio** | app móvil |
| **Depende de** | UT-CU45-MOB-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (modelos y servicios por feature) y el contrato de [UT-BACKEND](../UT-BACKEND/README.md).

## Solicitud de negocio

La persona ve y cambia sus avisos y preferencias igual que antes, pero la app los pide por GraphQL.

## Objetivo

Los seis métodos de `NotificacionesService` y `PreferenciasService` tienen una versión por GraphQL con el mismo resultado y los mismos errores.

## Alcance

- `features/notificaciones/services/documentos_graphql.dart`: seis documentos (`consultaBandeja`, `consultaConteoNoLeidas`, `mutacionMarcarLeida`, `mutacionMarcarTodasLeidas`, `consultaPreferencias`, `mutacionGuardarPreferencias`) que piden solo los campos que leen los modelos.
- `notificaciones_graphql_service.dart`: `NotificacionesGraphqlService extends NotificacionesService` (`listar`, `contarNoLeidas`, `leer`, `leerTodas`). Los filtros viajan como `null` si no se usan; la página se arma con `Pagina.fromJson({data, meta})`.
- `preferencias_graphql_service.dart`: `PreferenciasGraphqlService extends PreferenciasService` (`obtener`, `guardar`). Guardar manda `toJson(email:)` como `entrada`.
- Se comprobó una vez que los seis documentos son válidos contra el esquema real del servicio, que las variables se aceptan y que la respuesta trae exactamente los campos del modelo.
- Tests: `test/features/notificaciones/notificaciones_graphql_test.dart` (variables enviadas, mapeo a los modelos, tipo desconocido, bandeja vacía, errores `ERR_NOT_04`, `ERR_NOT_03` y `ERR_SYS_01`).

**Archivos esperados (crear/tocar):** los tres archivos de `services/` y el test.

## Fuera de alcance (no tocar)

- Los modelos, las pantallas y los providers de datos.
- Los servicios de REST (siguen igual).
- Registrar el dispositivo push.

## Resultado esperado

Con GraphQL como transporte, los seis métodos devuelven lo mismo que sus versiones de REST.

## Validación

1. `flutter test test/features/notificaciones/notificaciones_graphql_test.dart` pasa.
2. Los tests de pantalla y de providers de notificaciones (con servicios falsos) pasan sin cambios.
