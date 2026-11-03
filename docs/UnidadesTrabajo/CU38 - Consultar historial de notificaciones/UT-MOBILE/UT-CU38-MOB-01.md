# UT-CU38-MOB-01 — Modelos, servicio y providers de la bandeja

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU38-MOB-01` |
| **Nombre** | Modelos, servicio y providers de la bandeja |
| **Caso de uso** | CU38-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU38-BE-04 |

## Antes de implementar

Leer [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Ver como modelo `features/notificaciones/services/preferencias_service.dart`, `providers/notificaciones_providers.dart` y `features/alumnos/providers/mis_alumnos_provider.dart` (listado por páginas).

## Solicitud de negocio

La app necesita cargar la bandeja, el contador y marcar avisos como leídos.

## Objetivo

La feature `notificaciones` puede pedir la bandeja por páginas con filtros, contar las no leídas y marcar uno o todos como leídos, con su estado en providers.

## Alcance

- `models/notificacion.dart`: `Notificacion` inmutable (`codigoNotificacion`, `tipo`, `titulo`, `mensaje`, `leida`, `creadaEn` como `DateTime`) con `fromJson` y `copyWith(leida:)`; `TipoNotificacion` (enum con `fromJson` y su texto en español). Un tipo desconocido no rompe el parseo: se muestra como «Aviso».
- `services/notificaciones_service.dart` (`NotificacionesService`, con el `ApiClient` compartido):
  - `listar({page, pageSize, soloNoLeidas, tipo})` → `Pagina<Notificacion>` (los `filter[...]` solo si se usan; el cliente los codifica).
  - `contarNoLeidas()` → `int`; `leer(codigo)` → `Notificacion`; `leerTodas()` → `int` (marcadas).
- `providers/bandeja_providers.dart` (archivo nuevo, no toca los providers de CU37):
  - `notificacionesServiceProvider` (sustituible en tests).
  - `bandejaFiltroUiProvider`: filtro actual (`soloNoLeidas`, `tipo`).
  - `bandejaProvider` (`AsyncNotifier`): lista acumulada con `hayMas`, `cargarMas()`, `recargar()`; reacciona al cambio de filtro y de persona de la sesión; un fallo al cargar más conserva lo cargado y no deja el estado en error; `marcarLeida(codigo)` actualiza la lista en seguida y llama al servicio (404 quita el aviso; otro fallo deja la marca local sin cambiar y lo relanza para que la pantalla avise); `marcarTodas()` deja todo leído y devuelve cuántos.
  - `conteoNoLeidasProvider`: el contador; un fallo de red lo deja en `null` (sin insignia), no en error; se invalida al marcar y al volver a la bandeja.
- Tests: el parseo de `Notificacion` (incluido un tipo desconocido); el servicio arma bien la ruta y los filtros; la lista acumula páginas y `hayMas` se apaga en la última; cambiar el filtro vuelve a la primera página; un fallo en una página posterior conserva lo cargado; `marcarLeida` baja el contador, deja el aviso sin leer si el servicio falla por la red (se reintenta al abrirlo otra vez) y lo quita con 404; `marcarTodas` deja el contador en cero; un fallo del contador no es error.

Archivos esperados (crear/tocar): `notificacion.dart`, `notificaciones_service.dart`, `bandeja_providers.dart` y sus tests en `test/features/notificaciones/`.

## Fuera de alcance (no tocar)

- Pantallas y la campana (UT-CU38-MOB-02 y 03).
- Los providers y el servicio de las preferencias (CU37).
- `core/network/`: ya trae `ApiClient` y `Pagina`.

## Resultado esperado

Un test de widget o de provider puede cargar la bandeja con un servicio falso, filtrar, paginar y marcar avisos sin pantalla.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` en verde.
2. Contra el stack, desde un test de integración o un script: el servicio de una cuenta con avisos devuelve su primera página y su contador.
