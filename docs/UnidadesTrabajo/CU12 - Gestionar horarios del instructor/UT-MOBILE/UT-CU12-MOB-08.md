# UT-CU12-MOB-08 — Modelos con nombre, varios días y estado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-08` |
| **Nombre** | Modelos con nombre, varios días y estado |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (convención de `models/`). El contrato está en el [README de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http).

## Solicitud de negocio

La app tiene que entender un horario con nombre, varios días y estado, y mostrar cuántas franjas generará.

## Objetivo

Los modelos reflejan el contrato nuevo del backend.

## Alcance

- `models/estado_plantilla.dart`: `enum EstadoPlantilla { activa, pausada, inactiva }` con `valorApi` (`ACTIVA`, `PAUSADA`, `INACTIVA`) y `fromJson` que falla con un valor desconocido (mismo patrón que `EstadoFranja`).
- `PlantillaHorario`: quitar `diaSemana` y `activa`; agregar `nombre`, `diasSemana` (`List<int>`) y `estado`. Un getter `activa` (`estado == activa`) conserva el uso actual.
- `PlantillaHorario.franjasPorDia` y `franjasPorSemana`: `(minutos entre horaInicio y horaFin) ~/ duracionClaseMin` y ese valor por la cantidad de días. Sirven también para la vista previa (UT 10).
- `CrearPlantillaHorarioRequest`: quitar `diaSemana`; agregar `nombre` y `diasSemana`; `toJson` los envía.
- `models/dia_bloqueado.dart`: `DiaBloqueado` (`fecha`, `franjasAfectadas`) con `fromJson`.
- `models/fechas.dart`: agregar `lunesDeLaSemana(DateTime)` y `letraDeDia(int dia)` (`L M X J V S D`).
- Tests: `fromJson` de horario con varios días y de cada estado, y un estado desconocido que lanza; cálculo de franjas por día y por semana; `toJson` del request; `DiaBloqueado.fromJson`; `lunesDeLaSemana` en domingo y en lunes.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/models/estado_plantilla.dart`
- `projects/mobile/lib/features/horarios/models/plantilla_horario.dart`
- `projects/mobile/lib/features/horarios/models/plantilla_horario_creada.dart`
- `projects/mobile/lib/features/horarios/models/crear_plantilla_horario_request.dart`
- `projects/mobile/lib/features/horarios/models/dia_bloqueado.dart`
- `projects/mobile/lib/features/horarios/models/fechas.dart`
- `projects/mobile/test/features/horarios/horarios_models_test.dart`

## Fuera de alcance (no tocar)

- Llamadas a la API (UT 09).
- Pantallas (UT 11 y 12).

## Resultado esperado

Los modelos leen y escriben el contrato nuevo, con las cuentas de franjas listas para la vista previa.

## Validación

1. `flutter test` pasa con los casos nuevos en verde.
2. `flutter analyze` sin avisos.
