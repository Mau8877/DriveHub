# UT-CU40-MOB-02 — Clasificador en el teléfono y pantalla «Tablero»

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU40-MOB-02` |
| **Nombre** | Clasificador en el teléfono y pantalla «Tablero» |
| **Caso de uso** | CU40-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU40-MOB-01, UT-CU40-BE-02 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

El estudiante entiende una luz del tablero con solo una foto, aunque no tenga internet.

## Objetivo

La pestaña «Tablero» analiza la foto en el teléfono, explica la luz y guarda el resultado en el historial.

## Alcance

- `features/tablero/models/`: `LuzTablero`, `SeveridadTablero`, `CatalogoTablero`, `Deteccion`, `LuzReconocida`, `AnalisisTablero` y `AyudaTablero` (la ayuda guardada, que reutiliza CU42).
- `services/`: `procesarImagen` / `procesarImagenAparte` (decodificar, orientación, 224 × 224 y miniatura), `ClasificadorTablero` + `ClasificadorTflite` + `mejoresDetecciones`, `AnalizadorTablero` (une el modelo con el catálogo) y `AyudasTableroService.crear` (multipart `datos` + `miniatura`).
- `providers/tablero_providers.dart`: catálogo, clasificador, analizador, servicio, `selectorDeFotoProvider` (cámara o galería) y `TableroNotifier` (analiza, guarda si el modelo está seguro y permite reintentar).
- `screens/tablero_screen.dart`: botones de cámara y galería, interruptor «Guardar la foto en mi historial», foto analizada, resultado (luz, gravedad, confianza, significado, qué hacer, otras posibilidades), «no estoy seguro», estado de guardado con reintento, avisos de error y la nota de ayuda orientativa.
- `estudio_screen.dart`: la pestaña «Tablero».
- Tests: `test/features/tablero/tablero_test.dart` (catálogo real, preprocesado, top-k, analizador, servicio y pantalla).

Archivos esperados (crear/tocar): los de `lib/features/tablero/` y `estudio_screen.dart`, y el test.

## Fuera de alcance (no tocar)

- El historial y marcar «útil» (CU42); la prueba de conducción (CU39); entrenar el modelo.

## Resultado esperado

El estudiante toma la foto de una luz y ve qué es, qué significa y qué hacer; sin red igual lo ve y la app avisa que no se guardó.

## Validación

1. `flutter test` (los de tablero: 22 casos) y `flutter analyze` en `projects/mobile`.
2. En un Android real con `tablero.tflite` empaquetado: fotos de tableros dan la luz correcta en menos de 1 s; en modo avión el resultado aparece y avisa que no se guardó.
