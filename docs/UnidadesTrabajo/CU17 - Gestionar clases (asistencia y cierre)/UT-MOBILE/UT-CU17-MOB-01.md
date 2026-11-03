# UT-CU17-MOB-01 — Modelos y `ClasesService`: cerrar, ver y corregir una clase

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU17-MOB-01` |
| **Nombre** | Modelos y `ClasesService`: cerrar, ver y corregir una clase |
| **Caso de uso** | CU17-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md#modelos-de-datos-dtos) (DTOs inmutables a mano, `camelCase`, solo el service parsea). Tomar como modelo `clase_agenda.dart`, `reprogramar_request.dart` y `ClasesService` (UT-CU16-MOB-01).

## Solicitud de negocio

La app tiene que poder cerrar una clase, leerla y corregirla, y saber por la agenda cuándo ofrecer cada botón.

## Objetivo

Existen los modelos de la clase dictada y de sus pedidos, la agenda trae `cerrable` y `claseCodigo`, y `ClasesService` llama a las tres rutas nuevas.

## Alcance

- `models/asistencia_clase.dart`: `enum AsistenciaClase { presente('PRESENTE'), tarde('TARDE'), ausente('AUSENTE') }` con `valorApi`, `etiqueta` en español («Presente», «Tarde», «Ausente») y `fromJson` (un valor desconocido cae en `presente`, el caso que no pierde nada). Sigue el patrón de `EstadoReserva`.
- `models/clase_dictada.dart`: `ClaseDictada` (inmutable, constructor `const`, `fromJson` a mano) con `codigo`, `fecha` (`DateTime` solo fecha, con el parseo de `Reserva`), `estudianteCodigo`, `estudianteNombre`, `asistencia`, `tema`, `observaciones` (`String?`), `kmInicio` y `kmFin` (`int?`).
- `models/cerrar_clase_request.dart`: `CerrarClaseRequest(reservaCodigo, asistencia, tema, observaciones, kmInicio, kmFin)` con `toJson()` que **omite** los campos nulos y manda `asistencia` como `valorApi`.
- `models/actualizar_clase_request.dart`: `ActualizarClaseRequest(tema, observaciones, kmInicio, kmFin)` con `toJson()` que manda **solo los no nulos** (un texto vacío `''` sí se manda: así se borra) y `bool get estaVacio` (no cambia nada).
- `models/clase_agenda.dart` (CU16, se edita): campos `cerrable` (`bool`, por defecto `false` si falta) y `claseCodigo` (`String?`).
- `ClasesService` (se editan, sin tocar los existentes), con el `_llamar` existente (errores de negocio como `ApiException`):
  - `Future<ClaseDictada> cerrarClase(CerrarClaseRequest request)` → `POST /clases`.
  - `Future<ClaseDictada> claseDictada(String codigoClase)` → `GET /clases/{codigo}`.
  - `Future<ClaseDictada> actualizarClase(String codigoClase, ActualizarClaseRequest request)` → `PATCH /clases/{codigo}`.
- Tests: `fromJson` de `ClaseDictada` con todos los campos y con los opcionales `null`; cada valor de `AsistenciaClase`; `CerrarClaseRequest.toJson` omite los nulos; `ActualizarClaseRequest.toJson` manda solo lo no nulo, manda `''` y `estaVacio`; `ClaseAgenda` lee `cerrable` y `claseCodigo` (y con los campos ausentes lee `false` y `null`); el service con un Dio simulado manda el método, la ruta y el cuerpo correctos en las tres llamadas, y convierte un error de negocio (`ERR_ACA_37`, `ERR_ACA_38`, `ERR_ACA_35`) en `ApiException`. Los tests de `ClaseAgenda` y de `ClasesService` existentes siguen en verde.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/models/asistencia_clase.dart`, `clase_dictada.dart`, `cerrar_clase_request.dart`, `actualizar_clase_request.dart`, `clase_agenda.dart`
- `projects/mobile/lib/features/clases/services/clases_service.dart`
- `projects/mobile/test/features/clases/clases_models_test.dart`, `clases_service_test.dart`
- `projects/mobile/test/helpers/clases_falsos.dart` (los métodos nuevos del doble y `claseDictadaDePrueba`; `claseDeAgendaDePrueba` gana `cerrable` y `claseCodigo`)

## Fuera de alcance (no tocar)

- Providers, la hoja y los botones (UT 02 a 04).
- Los métodos y modelos de CU14, CU15 y CU16 salvo los dos campos de `ClaseAgenda`.

## Resultado esperado

Con un Dio simulado, la app cierra, lee y corrige una clase tipadas, y la agenda dice cuándo cerrar y cuándo editar.

## Validación

1. `flutter test` pasa (en Linux/Docker si hace falta, ver las notas de CU15-MOB).
2. `flutter analyze` sin errores.
