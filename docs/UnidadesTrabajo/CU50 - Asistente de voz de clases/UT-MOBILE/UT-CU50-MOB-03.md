# UT-CU50-MOB-03 — Ejecutar las consultas sobre la pantalla

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-03` |
| **Nombre** | Ejecutar las consultas sobre la pantalla |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU50-MOB-01, UT-CU50-MOB-02 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Leer `features/clases/providers/clases_pestana_provider.dart` y `franjas_disponibles_provider.dart`.

## Solicitud de negocio

Decir «busca horarios por la mañana con Carlos» debe dejar la pantalla como si la persona hubiera tocado esos filtros.

## Objetivo

Un ejecutor que aplica las acciones de consulta usando los providers de UI que ya existen en `features/clases`.

## Alcance

- `features/asistente/services/ejecutor_de_consultas.dart` (recibe `Ref`): `ver_mis_clases` y `ver_historial` eligen la pestaña (`clasesPestanaUiProvider`); `buscar_horarios` elige Reservar y aplica fecha, momento e instructor (`reservarFechaUiProvider`, `reservarMomentoUiProvider`, `reservarInstructorUiProvider`); `seleccionar_horario` marca la franja de esa hora con `franjasSeleccionadasUiProvider.alternar` (no confirma); `limpiar_filtros` vuelve momento e instructor a «todos»; `ver_horas_disponibles` devuelve el saldo (`saldoHorasProvider`) para que la hoja lo muestre.
- Devuelve el mensaje para la persona («Mostrando clases del viernes…») o el motivo si no se pudo (sin horario a esa hora, sin horas para marcar más franjas).
- Tests: `test/features/asistente/ejecutor_de_consultas_test.dart` con `ProviderContainer` y el `ClasesService` falso que ya usan los tests de `clases`.

Archivos esperados (crear/tocar): el ejecutor y su test.

## Fuera de alcance (no tocar)

- Cancelar, reprogramar o confirmar una reserva (MOB-04); la barra «Confirmar» de CU14 no se toca; los providers de `clases` no cambian.

## Resultado esperado

Después de `buscar_horarios {momento: manana}`, el provider de momento queda en «Mañana» y la pantalla muestra la pestaña Reservar.

## Validación

1. `flutter test test/features/asistente/ejecutor_de_consultas_test.dart` en verde.
2. `flutter test test/features/clases` sigue en verde.
3. `flutter analyze` sin avisos nuevos.
