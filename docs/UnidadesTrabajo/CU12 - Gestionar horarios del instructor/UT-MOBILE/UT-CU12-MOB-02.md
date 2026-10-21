# UT-CU12-MOB-02 — `HorariosService`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-02` |
| **Nombre** | `HorariosService` |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | `UT-CU12-MOB-01` |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (un `service` recibe el cliente compartido y solo conoce los endpoints de su feature), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `features/auth/services/registro_service.dart` y su test.

## Solicitud de negocio

La app tiene que hablar con el backend para listar, crear y desactivar horarios y para listar, bloquear y desbloquear franjas.

## Objetivo

`HorariosService` expone una función por cada endpoint del CU12 y devuelve objetos tipados.

## Alcance

- `lib/features/horarios/services/horarios_service.dart`: constructor `HorariosService({Dio? dio}) : _dio = dio ?? ApiClient.instance`. Un error de negocio llega como `ApiException` (se relanza igual que en `RegistroService`); cualquier otro error se relanza tal cual.
  - `Future<Pagina<PlantillaHorario>> listarPlantillas({bool? activa, int page = 1, int pageSize = 50})` → `GET /plantillas-horario` con `page`, `pageSize` y `filter[activa]` solo si no es nulo.
  - `Future<PlantillaHorarioCreada> crearPlantilla(CrearPlantillaHorarioRequest request)` → `POST /plantillas-horario`.
  - `Future<void> desactivarPlantilla(String codigo)` → `POST /plantillas-horario/{codigo}/desactivar`.
  - `Future<Pagina<Franja>> listarFranjas({DateTime? fecha, EstadoFranja? estado, int page = 1, int pageSize = 50})` → `GET /franjas` con `filter[fecha]` (`yyyy-MM-dd`) y `filter[estado]` (el nombre en mayúsculas) solo si no son nulos.
  - `Future<Franja> bloquearFranja(String codigo)` → `POST /franjas/{codigo}/bloquear`; `Future<Franja> desbloquearFranja(String codigo)` → `POST /franjas/{codigo}/desbloquear`.
- Los corchetes de `filter[...]` los codifica `Dio` solos (ver ENDPOINTS_BACKEND.md); no se escriben a mano en la URL.
- Tests (con el mismo doble de `Dio` que usan los tests de `auth_service_test.dart`): cada función llama al método HTTP y la ruta correctas y manda los parámetros esperados (incluido que `filter[activa]` no viaja si es nulo); devuelve el objeto tipado (`Pagina`, `PlantillaHorarioCreada`, `Franja`); un error de negocio (`ERR_ACA_04`, `ERR_ACA_08`...) llega como `ApiException` con su código y mensaje.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/services/horarios_service.dart`
- `projects/mobile/test/features/horarios/services/horarios_service_test.dart`

## Fuera de alcance (no tocar)

- Estado y providers (UT 04) y pantallas.
- `ApiClient` y su interceptor (ya desempaquetan el sobre, agregan el Bearer y renuevan ante un 401).
- Endpoints de otros casos de uso.

## Resultado esperado

Con `HorariosService` se puede ejercer todo el contrato HTTP del CU12 desde Dart, con errores de negocio tipados.

## Validación

1. `flutter test` pasa con `horarios_service_test.dart` en verde.
2. `flutter analyze` sin errores.
