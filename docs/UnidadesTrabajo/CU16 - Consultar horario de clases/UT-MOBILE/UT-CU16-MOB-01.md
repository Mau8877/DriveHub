# UT-CU16-MOB-01 — Modelos y `ClasesService`: agenda y cancelar por el instructor

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU16-MOB-01` |
| **Nombre** | Modelos y `ClasesService`: agenda y cancelar por el instructor |
| **Caso de uso** | CU16-MOB |
| **Microservicio** | `projects/mobile` (feature `clases`) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md#modelos-de-datos-dtos) (DTOs inmutables a mano, `camelCase`, solo el service parsea). Tomar como modelo `reserva.dart`, `resultado_cancelacion.dart` y `ClasesService` (UT-CU15-MOB-01).

## Solicitud de negocio

La app tiene que poder pedir la agenda de un día y cancelar una clase del instructor, y entender lo que responde el servidor.

## Objetivo

Existen los modelos de la agenda y los dos métodos de `ClasesService` que llaman al backend.

## Alcance

- `models/clase_agenda.dart`: `ClaseAgenda` (inmutable, constructor `const`, `fromJson` a mano) con `codigo`, `fecha` (`DateTime` de solo fecha, leída con el parseo que ya usa `Reserva`), `horaInicio`, `horaFin`, `estudianteCodigo`, `estudianteNombre`, `estado` (`EstadoReserva` existente; un valor desconocido cae en `confirmada`), `terminada` y `cancelable` (ambos por defecto `false` si faltan).
- `models/resultado_cancelacion_instructor.dart`: `ResultadoCancelacionInstructor(ClaseAgenda clase, HoraDeCancelacion horaDevuelta)` con `fromJson`. Reutiliza `HoraDeCancelacion` (`PERDIDA` no ocurre pero se tolera).
- `ClasesService` (se editan, sin tocar los existentes):
  - `Future<Pagina<ClaseAgenda>> agenda({required DateTime fecha, int page = 1, int pageSize = 50})` → `GET /reservas/agenda` con `filter[fecha]` en `yyyy-MM-dd`, `page` y `pageSize`.
  - `Future<ResultadoCancelacionInstructor> cancelarComoInstructor(String codigoReserva)` → `POST /reservas/{codigo}/cancelar-por-instructor`.
  - Ambos con el `_llamar` existente (errores de negocio como `ApiException`).
- Tests: `fromJson` de `ClaseAgenda` con cada estado y con campos opcionales ausentes; `ResultadoCancelacionInstructor.fromJson` con `DEVUELTA` y `EN_PROCESO`; el service con un Dio simulado manda `filter[fecha]` con el formato correcto, la ruta de cancelar, y convierte un error de negocio en `ApiException`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/models/clase_agenda.dart`, `resultado_cancelacion_instructor.dart`
- `projects/mobile/lib/features/clases/services/clases_service.dart`
- `projects/mobile/test/features/clases/models/clase_agenda_test.dart`, `resultado_cancelacion_instructor_test.dart`
- `projects/mobile/test/features/clases/services/clases_service_test.dart`

## Fuera de alcance (no tocar)

- Providers y pantallas (UT 02 a 04).
- Los métodos y modelos del estudiante (`reservas`, `cancelar`, `Reserva`...): no cambian.

## Resultado esperado

Con un Dio simulado, la app lee la agenda de un día y cancela una clase tipadas.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
