# UT-CU14-MOB-02 — `ClasesService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-MOB-02` |
| **Nombre** | `ClasesService` |
| **Caso de uso** | CU14-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU14-MOB-01` |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md#servicios-por-feature) (el service recibe el `ApiClient` compartido y solo conoce los endpoints de su feature), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `lib/features/pagos/services/pagos_service.dart` (el método `_llamar` que traduce errores) y `lib/features/horarios/services/horarios_service.dart`.

## Solicitud de negocio

La app necesita llamar al backend para ver las franjas ofrecidas, los instructores, confirmar reservas y listar las clases.

## Objetivo

Existe `ClasesService`, que habla con los endpoints del CU14 y entrega modelos tipados.

## Alcance

En `lib/features/clases/services/clases_service.dart`, sobre el `ApiClient` de `core/network/`:

- `Future<Pagina<FranjaDisponible>> franjasDisponibles({DateTime? fecha, MomentoDia momento = MomentoDia.todos, String? instructorCodigo, int page = 1, int pageSize = 100})`: `GET /franjas/disponibles` con `filter[fecha]` (`yyyy-MM-dd`), `filter[horaDesde]`, `filter[horaHasta]`, `filter[instructor]`; los filtros nulos no se mandan.
- `Future<List<InstructorDisponible>> instructoresDisponibles({DateTime? fecha})`: `GET /franjas/instructores`.
- `Future<ReservasResultado> reservar(List<String> franjaCodigos)`: `POST /reservas`.
- `Future<Pagina<Reserva>> reservas({required VistaReservas vista, int page = 1, int pageSize = 50})`: `GET /reservas` con `filter[vista]`; `VistaReservas` es un `enum { proximas, historial }` definido en este archivo o en `models/`.
- Los errores (`ApiException` con el código de negocio, o de red) se propagan al llamador tal cual; los mensajes de error ya vienen en español.
- Tests con un `ApiClient` simulado: cada método arma la ruta y los parámetros correctos (el filtro de la noche manda solo `horaDesde`), los filtros nulos no viajan, `reservar` manda `franjaCodigos`, y un error `failed` del backend se propaga como `ApiException`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/services/clases_service.dart`
- `projects/mobile/test/features/clases/clases_service_test.dart`

## Fuera de alcance (no tocar)

- Providers, pantallas y lógica de selección.
- `PagosService` y el saldo (ya existe `saldoDeHoras()` en Pagos; se reutiliza en la UT 04).
- Reintentos automáticos: el reintento lo decide el usuario.

## Resultado esperado

Los providers pueden pedir franjas, instructores y reservas, y confirmar reservas con una llamada tipada.

## Validación

1. `flutter test` pasa con los tests del service en verde.
2. `flutter analyze` sin errores.
