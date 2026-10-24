# UT-CU12-MOB-10 — Validadores, vista previa y providers

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU12-MOB-10` |
| **Nombre** | Validadores, vista previa y providers |
| **Caso de uso** | CU12-MOB |
| **Microservicio** | `projects/mobile` (Flutter) |
| **Depende de** | `UT-CU12-MOB-08`, `UT-CU12-MOB-09` |

## Antes de implementar

[FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md). Leer la [decisión de diseño 9](./README.md#decisiones-de-diseño).

## Solicitud de negocio

El formulario avisa qué falta y le muestra al instructor cuántas franjas va a crear antes de guardar.

## Objetivo

Las reglas del formulario y el estado de las pantallas quedan listos para las pantallas nuevas.

## Alcance

- `validators/horarios_validators.dart`: agregar `validarNombre` (obligatorio, hasta 60 caracteres) y `validarDias` (al menos uno); el resto de las reglas no cambia. Mensajes en español, como los del backend.
- Vista previa: función pura `vistaPreviaFranjas(horaInicio, horaFin, duracion, cantidadDias)` que devuelve `(porDia, porSemana)` o `null` si los datos todavía no alcanzan o no son válidos. Se prueba aparte, sin widgets.
- `providers/horarios_provider.dart`: `horariosIncluirInactivosUiProvider` se conserva; `horariosListProvider` pide activos y pausados, y además inactivos cuando el interruptor está encendido (un provider de estados deriva el conjunto). `HorariosNotifier` agrega `pausar` y `reanudar`, que invalidan el listado, igual que `desactivar`.
- `providers/franjas_provider.dart`: `franjasSemanaUiProvider` (el lunes de la semana elegida; semana actual por defecto, con `anterior`/`siguiente`); `franjasListProvider` pide las franjas del lunes al domingo (`pageSize` 100) en lugar de una fecha; `FranjasNotifier` agrega `bloquearDia` y `desbloquearDia` (devuelven cuántas franjas cambiaron y refrescan la lista). Se conserva `bloquear`/`desbloquear` de una franja.
- Tests: cada validador; vista previa con datos válidos, con duración que no divide la ventana y con campos vacíos; los providers con un servicio falso (estados pedidos, pausar y reanudar invalidan, la semana cambia, bloquear el día refresca).

Archivos esperados (crear/tocar):

- `projects/mobile/lib/features/horarios/validators/horarios_validators.dart`
- `projects/mobile/lib/features/horarios/providers/horarios_provider.dart`
- `projects/mobile/lib/features/horarios/providers/franjas_provider.dart`
- `projects/mobile/test/features/horarios/horarios_validators_test.dart`
- `projects/mobile/test/features/horarios/horarios_provider_test.dart`

## Fuera de alcance (no tocar)

- Pantallas (UT 11 y 12).

## Resultado esperado

El formulario valida el nombre y los días, calcula la vista previa, y los providers manejan estados, semana y día completo.

## Validación

1. `flutter test` pasa con los casos nuevos en verde.
2. `flutter analyze` sin avisos.
