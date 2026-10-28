# UT-CU23-MOB-01 — Mis notas y pestañas de «Estudio»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU23-MOB-01` |
| **Nombre** | Mis notas y pestañas de «Estudio» |
| **Caso de uso** | CU23-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU23-BE-02 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Solicitud de negocio

El estudiante ve cómo le va en todas sus evaluaciones desde el teléfono.

## Objetivo

La pestaña «Mis notas» lista sus notas con filtro por tipo, junto a «Simulacros» en «Estudio».

## Alcance

- `features/resultados/`: `Resultado` y `OrigenResultado`; `ResultadosService.mios`; `resultadosServiceProvider`, `resultadosFiltroUiProvider` y `resultadosListProvider`; `ResultadosScreen` (chips de filtro, tarjetas con insignia, estado vacío y reintento).
- `features/estudio/screens/estudio_screen.dart`: las dos pestañas; `estudiante_shell.dart`: «Estudio» muestra `EstudioScreen`.
- Tests: `test/features/resultados/resultados_test.dart` (servicio, pantalla y pestañas).

Archivos esperados (crear/tocar): los de `lib/features/{resultados,estudio}`, `estudiante_shell.dart` y el test.

## Fuera de alcance (no tocar)

- Las pantallas de simulacros (CU21) y el resto de las secciones.

## Resultado esperado

El estudiante abre «Estudio > Mis notas» y ve sus notas, filtradas por tipo.

## Validación

1. `flutter test` (el de resultados: 8 casos) y `flutter analyze` en `projects/mobile`.
2. En un emulador contra el stack: tras terminar un simulacro aparece su nota en «Mis notas».
