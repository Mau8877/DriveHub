# UT-CU42-MOB-01 — Pantalla «Historial de ayudas»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU42-MOB-01` |
| **Nombre** | Pantalla «Historial de ayudas» |
| **Caso de uso** | CU42-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU40-MOB-02, UT-CU42-BE-01 |

## Antes de implementar

[ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Solicitud de negocio

El estudiante revisa lo que le dijo la app sobre su tablero y dice si le sirvió.

## Objetivo

Desde «Tablero» se abre el historial de ayudas con su detalle y la opinión.

## Alcance

- `AyudasTableroService.historial()` y `marcarUtil()`.
- `historialAyudasProvider` y `marcarAyudaUtil()`; guardar una ayuda (CU40) invalida el historial.
- `HistorialAyudasScreen` (tarjetas con miniatura, luz, fecha, gravedad y opinión; estado vacío y reintento; hoja de detalle con la explicación, otras posibilidades y «¿Te sirvió esta ayuda?»).
- Ruta `/study/tablero/historial` y el botón «Ver mi historial de ayudas» en `TableroScreen`.
- Tests: `test/features/tablero/historial_test.dart` (servicio y pantalla).

Archivos esperados (crear/tocar): `ayudas_tablero_service.dart`, `tablero_providers.dart`, `historial_ayudas_screen.dart`, `tablero_screen.dart`, `app_router.dart` y el test.

## Fuera de alcance (no tocar)

- El análisis de la foto (CU40), la prueba de conducción (CU39) y el resto de «Estudio».

## Resultado esperado

El estudiante abre su historial, toca una ayuda y marca si le sirvió.

## Validación

1. `flutter test` (los del historial: 9 casos) y `flutter analyze` en `projects/mobile`.
2. En un Android contra el stack: analizar una luz con conexión y verla en el historial con su miniatura.
