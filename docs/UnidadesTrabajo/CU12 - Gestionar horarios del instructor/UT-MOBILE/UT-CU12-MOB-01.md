# UT-CU12-MOB-01 — Modelos de horarios y franjas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-01` |
| **Nombre** | Modelos de horarios y franjas |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md#modelos-de-datos-dtos) (clases inmutables, `fromJson`/`toJson` a mano, `camelCase`), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) (el parseo no trivial se prueba). Tomar como modelo `features/auth/models/`.

## Solicitud de negocio

La app necesita representar lo que envía y recibe del backend al gestionar horarios, sin trabajar con mapas sueltos.

## Objetivo

Existen los DTOs Dart del CU12, con su parseo probado.

## Alcance

En `lib/features/horarios/models/`, una clase por archivo, campos `final` y constructor `const`:

- `plantilla_horario.dart`: `PlantillaHorario` con `codigo`, `diaSemana` (int), `horaInicio`, `horaFin` (String `HH:mm`), `duracionClaseMin`, `vigenteDesde` (`DateTime`, solo fecha), `vigenteHasta` (`DateTime?`), `activa`; `factory fromJson`.
- `plantilla_horario_creada.dart`: `PlantillaHorarioCreada` con un `PlantillaHorario horario` y `franjasGeneradas` (int); `factory fromJson` (el JSON trae los campos del horario más `franjasGeneradas` en el mismo nivel).
- `crear_plantilla_horario_request.dart`: `CrearPlantillaHorarioRequest` con `diaSemana`, `horaInicio`, `horaFin`, `duracionClaseMin`, `vigenteDesde` (`DateTime?`), `vigenteHasta` (`DateTime?`); `toJson` con las fechas como `yyyy-MM-dd` y **sin** los campos nulos.
- `franja.dart`: `Franja` con `codigo`, `fecha` (`DateTime`, solo fecha), `horaInicio`, `horaFin`, `estado`; y `enum EstadoFranja { libre, reservada, bloqueada }` con `fromJson('LIBRE' | 'RESERVADA' | 'BLOQUEADA')`; un valor desconocido lanza `FormatException` (mejor fallar que mostrar un estado inventado).
- `pagina.dart`: `Pagina<T>` con `items`, `page`, `pageSize`, `totalItems`, `totalPages`, y un `factory` que arma la página desde `{ 'data': [...], 'meta': {...} }` (lo que entrega `ApiClient` en los listados) con una función `T Function(Map<String, dynamic>)`.
- Tests unitarios: cada `fromJson` con un JSON completo y con los opcionales ausentes (`vigenteHasta` nulo); `toJson` del request sin nulos y con fechas `yyyy-MM-dd`; estados válidos e inválido; `Pagina` con items y `meta`.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/models/plantilla_horario.dart`, `plantilla_horario_creada.dart`, `crear_plantilla_horario_request.dart`, `franja.dart`, `pagina.dart`
- `projects/mobile/test/features/horarios/models/` (un test por archivo, o uno solo `horarios_models_test.dart`)

## Fuera de alcance (no tocar)

- El service que llama a la API (UT 02) y los providers (UT 04).
- `json_serializable` ni generación de código: se escribe a mano (decisión del proyecto).
- Cualquier modelo de `features/auth/`.

## Resultado esperado

La app puede convertir las respuestas del backend en objetos tipados y armar el request de creación.

## Validación

1. `flutter test` pasa.
2. `flutter analyze` sin errores.
