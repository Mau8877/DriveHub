# UT-CU21-MOB-01 — Modelos, servicio y providers de simulacros

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU21-MOB-01` |
| **Nombre** | Modelos, servicio y providers de simulacros |
| **Caso de uso** | CU21-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU21-BE-03 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) y [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md).

## Solicitud de negocio

La app habla con la API de simulacros y guarda el estado del intento mientras se rinde.

## Objetivo

Los DTO, el servicio y los providers de la feature `simulacros`.

## Alcance

- `models/`: `SimulacroDisponible`, `TipoPregunta`, `IntentoIniciado` (con pregunta y opción), `IntentoResultado` (con corrección), `IntentoResumen`; inmutables, `fromJson` escrito a mano.
- `services/simulacros_service.dart`: `disponibles`, `iniciar`, `finalizar` (manda solo lo marcado, con las opciones ordenadas), `corregido` e `historial`; errores como `ApiException`.
- `providers/`: `simulacrosServiceProvider`, `simulacrosDisponiblesProvider`, `intentosHistorialProvider`, `intentoCorregidoProvider` y `rendirProvider` (`RendirNotifier`: iniciar, elegir, irA, finalizar, descartar).
- `widgets/mensajes_de_simulacro.dart`: mensajes en español por código de error, `formatearTiempo` y `formatearPuntaje`.
- Tests: `simulacros_service_test.dart`, `rendir_provider_test.dart` y `test/helpers/simulacros_falsos.dart`.

Archivos esperados (crear/tocar): los de `lib/features/simulacros/{models,services,providers,widgets}` y sus tests.

## Fuera de alcance (no tocar)

- Las pantallas y el router (UT-02).

## Resultado esperado

El servicio manda y lee lo que define el contrato, y el estado del intento se comporta según el tipo de pregunta.

## Validación

1. `flutter test test/features/simulacros` y `flutter analyze` en `projects/mobile`.
