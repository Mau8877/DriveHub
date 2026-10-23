# UT-CU12-MOB-03 — Validadores del formulario de horario

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-03` |
| **Nombre** | Validadores del formulario de horario |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | — |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md) (validación nativa, mensajes en español, **mismas reglas que el backend**), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo `features/auth/validators/registro_validators.dart`. Las reglas están en el CU: [Reglas de negocio](../../../CasosDeUso/CU12%20-%20Gestionar%20horarios%20del%20instructor.md#reglas-de-negocio).

## Solicitud de negocio

El instructor tiene que enterarse de un dato inválido en el mismo formulario, antes de enviar, igual que el backend lo rechazaría.

## Objetivo

Existen las funciones de validación del formulario de horario, con las mismas reglas del backend y mensajes en español.

## Alcance

- `lib/features/horarios/validators/horarios_validators.dart` con funciones puras `String? Function(...)` (devuelven el mensaje o `null`):
  - `validarDuracion(String? valor)`: obligatoria, entero, entre 15 y 240 (límites como constantes `duracionMinima = 15` y `duracionMaxima = 240`).
  - `validarHoraFin(String? horaInicio, String? horaFin)`: ambas presentes; `horaFin` posterior a `horaInicio` (comparación por minutos del día sobre `HH:mm`).
  - `validarVentana(String? horaInicio, String? horaFin, String? duracion)`: el tramo entre inicio y fin es múltiplo exacto de la duración (solo se evalúa si las tres son válidas; si no, devuelve `null` para no duplicar mensajes).
  - `validarVigenteDesde(DateTime? desde, {required DateTime hoy})`: no anterior a `hoy` (compara solo la fecha); `null` se acepta (el backend toma hoy).
  - `validarVigenteHasta(DateTime? hasta, DateTime? desde, {required DateTime hoy})`: opcional; si está, no puede ser anterior a `desde` (o a `hoy` si `desde` es nulo).
- Mensajes en español y concretos (por ejemplo, "La hora de fin debe ser posterior a la de inicio", "La duración debe estar entre 15 y 240 minutos", "El tramo entre inicio y fin debe ser un múltiplo de la duración").
- `hoy` se recibe por parámetro para poder probar sin depender del reloj.
- Tests unitarios por función: válido, límites (15, 240, 14, 241), no numérico, vacío; fin igual y anterior al inicio; tramo de 4 horas con duración 60 (válido) y 90 (inválido); vigencias con fecha igual a hoy (válida), anterior (inválida), `hasta` igual a `desde` (válida) y anterior (inválida).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/validators/horarios_validators.dart`
- `projects/mobile/test/features/horarios/horarios_validators_test.dart`

## Fuera de alcance (no tocar)

- La pantalla del formulario (UT 05) y la comprobación de cruce con otros horarios, que solo puede hacer el backend (`ERR_ACA_04`).
- Cambiar las reglas del backend: si una regla cambia, se actualiza en los dos lados en el mismo cambio.

## Resultado esperado

El formulario puede rechazar localmente todo lo que el backend rechazaría por formato, hora, duración y vigencia.

## Validación

1. `flutter test` pasa con `horarios_validators_test.dart` en verde.
2. `flutter analyze` sin errores.
