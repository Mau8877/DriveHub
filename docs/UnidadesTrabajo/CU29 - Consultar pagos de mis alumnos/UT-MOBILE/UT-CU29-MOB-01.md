# UT-CU29-MOB-01 — Modelos y servicio de las horas de los alumnos

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU29-MOB-01` |
| **Nombre** | Modelos y servicio de las horas de los alumnos |
| **Caso de uso** | CU29-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | — |

## Antes de implementar

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Tomar como modelo la feature `features/horarios/` (models, service, providers, validators, screens y tests).

## Solicitud de negocio

La app del instructor necesita leer a sus alumnos y el detalle de cada uno, tipados.

## Objetivo

`HorasAlumnosService` llama a los endpoints de CU29 y devuelve modelos tipados.

## Alcance

- `features/alumnos/models/` (nueva feature, inmutables, `const`, `fromJson` a mano): `HoraAlumno(estudianteCodigo, nombres, apellidos, horasCompradas, horasGastadasConmigo, horasRestantes)` con un getter `nombreCompleto`, `ConsumoClase(claseCodigo, fecha, horas)`, `PagoAlumno(codigo, planNombre, horas, estado, creadoEn)` (el `estado` reutiliza `EstadoPagoVisible` de CU28) y `HorasAlumnoDetalle(resumen, consumos, pagos)`.
- `features/alumnos/services/horas_alumnos_service.dart` (usa `Pagina<T>` de `core/network/`, mismo patrón de `_llamar`): `listar({search, page, pageSize = 20})` → `Pagina<HoraAlumno>` desde `GET /horas-alumnos`; `detalle(estudianteCodigo)` → `HorasAlumnoDetalle` desde `GET /horas-alumnos/{estudianteCodigo}`. Los errores de negocio llegan como `ApiException` (`ERR_PAG_12`, `ERR_PAG_15`).
- Tests: parseo de cada modelo (con consumos y pagos vacíos); el servicio con un adaptador de Dio falso: URL, `search` y `Pagina` con `meta`, y los errores propagados.

Archivos esperados (crear/tocar):

- projects/mobile/lib/features/alumnos/models/hora_alumno.dart
- projects/mobile/lib/features/alumnos/models/consumo_clase.dart
- projects/mobile/lib/features/alumnos/models/pago_alumno.dart
- projects/mobile/lib/features/alumnos/models/horas_alumno_detalle.dart
- projects/mobile/lib/features/alumnos/services/horas_alumnos_service.dart
- projects/mobile/test/features/alumnos/modelos_alumnos_test.dart
- projects/mobile/test/features/alumnos/horas_alumnos_service_test.dart

## Fuera de alcance (no tocar)

- Providers y pantallas (UT 02 y 03).
- El botón del comprobante (CU30).

## Resultado esperado

Se pueden pedir los alumnos del instructor y el detalle de cada uno con modelos tipados.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile`.
