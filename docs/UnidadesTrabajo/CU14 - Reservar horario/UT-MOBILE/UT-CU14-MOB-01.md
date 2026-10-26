# UT-CU14-MOB-01 — Modelos de franjas disponibles y reservas

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU14-MOB-01` |
| **Nombre** | Modelos de franjas disponibles y reservas |
| **Caso de uso** | CU14-MOB |
| **Microservicio** | `mobile` (app) |
| **Depende de** | — |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md#modelos-de-datos-dtos) (clases inmutables, `const`, `fromJson`/`toJson` escritos a mano, `camelCase`), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) (se prueba el parseo cuando el mapeo no es trivial). Tomar como modelo `lib/features/horarios/models/franja.dart` y `lib/features/pagos/models/`.

## Solicitud de negocio

La app necesita representar las franjas ofrecidas, los instructores, las reservas y el resultado de confirmar, tal como los entrega el backend.

## Objetivo

Existen los modelos Dart del CU14, con su parseo probado.

## Alcance

En `lib/features/clases/models/`:

- `franja_disponible.dart`: `FranjaDisponible` (`codigo`, `fecha` `DateTime` solo fecha, `horaInicio`, `horaFin` `String` `HH:mm`, `instructorCodigo`, `instructorNombre`) con `fromJson`.
- `instructor_disponible.dart`: `InstructorDisponible` (`codigo`, `nombre`) con `fromJson`.
- `estado_reserva.dart`: `enum EstadoReserva { confirmada, completada, noAsistio, cancelada }` con `fromJson(String)` (`CONFIRMADA`, `COMPLETADA`, `NO_ASISTIO`, `CANCELADA`; un valor desconocido no rompe: se trata como `confirmada`).
- `reserva.dart`: `Reserva` (`codigo`, `fecha`, `horaInicio`, `horaFin`, `instructorCodigo`, `instructorNombre`, `estado`, `terminada`) con `fromJson`.
- `franja_rechazada.dart`: `FranjaRechazada` (`franjaCodigo`, `fecha`, `horaInicio`, `horaFin`, `instructorNombre`, `error`, `mensaje`) con `fromJson`.
- `reservas_resultado.dart`: `ReservasResultado` (`reservadas` `List<Reserva>`, `rechazadas` `List<FranjaRechazada>`, `horasDisponibles` `int?`) con `fromJson`.
- `reservar_franjas_request.dart`: `ReservarFranjasRequest` (`franjaCodigos`) con `toJson`.
- `momento_dia.dart`: `enum MomentoDia { todos, manana, tarde, noche }` con la etiqueta en español y `horaDesde`/`horaHasta` (`String?`) según la decisión 6 del README.
- `Pagina<T>` ya existe en `lib/core/network/pagina.dart`: se reutiliza, no se copia.
- Tests unitarios de `fromJson` con el JSON real del contrato: una franja, una reserva `CONFIRMADA` terminada, un resultado con una reservada y una rechazada, `horasDisponibles` nulo, un estado desconocido, y que `MomentoDia.noche.horaHasta` es nulo.

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/clases/models/franja_disponible.dart`, `instructor_disponible.dart`, `estado_reserva.dart`, `reserva.dart`, `franja_rechazada.dart`, `reservas_resultado.dart`, `reservar_franjas_request.dart`, `momento_dia.dart`
- `projects/mobile/test/features/clases/models_test.dart`

## Fuera de alcance (no tocar)

- Llamadas a la API (UT 02), providers (UT 03) y pantallas.
- `lib/features/horarios/models/franja.dart` (es la franja del instructor, otro contrato).
- `lib/core/network/pagina.dart`.

## Resultado esperado

El resto de las UT trabajan con objetos tipados en vez de mapas de JSON.

## Validación

1. `flutter test` pasa con los tests de modelos en verde.
2. `flutter analyze` sin errores.
