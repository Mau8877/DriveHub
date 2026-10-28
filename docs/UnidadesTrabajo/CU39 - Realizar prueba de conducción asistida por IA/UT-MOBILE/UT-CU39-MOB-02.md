# UT-CU39-MOB-02 — Pantalla «Prueba» y guardado del resultado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU39-MOB-02` |
| **Nombre** | Pantalla «Prueba» y guardado del resultado |
| **Caso de uso** | CU39-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU39-MOB-01, UT-CU39-BE-01 |

## Antes de implementar

[CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

El estudiante practica la revisión de espejos y ve cómo le fue.

## Objetivo

La pestaña «Prueba» guía los pasos con la cámara, muestra el resultado y lo guarda en el historial.

## Alcance

- `services/sesiones_service.dart`: `SesionesService.guardarPrueba` (`POST /sesiones-ia`).
- `providers/prueba_providers.dart`: `detectorRostroProvider`, `sesionesServiceProvider`, `relojPruebaProvider` y `PruebaNotifier` (abre la cámara, evalúa cada 150 ms, termina, guarda y permite reintentar, cancelar y repetir).
- `screens/prueba_screen.dart`: inicio (pasos, aviso de privacidad, consejos y error), cámara abriéndose, paso en curso (vista de la cámara, instrucción, avance, tiempo y aviso de «no te veo»), resultado (puntaje, pasos, retroalimentación y estado de guardado) y orientación en vertical mientras dura la prueba.
- `estudio_screen.dart`: la pestaña «Prueba».
- Tests: la parte de servicio y de pantalla de `test/features/prueba/prueba_test.dart`; ajuste del test de pestañas en `resultados_test.dart`.

Archivos esperados (crear/tocar): `lib/features/prueba/{services,providers,screens}`, `estudio_screen.dart` y los tests.

## Fuera de alcance (no tocar)

- Historial de pruebas, selección de categoría o escenario, y la lógica de la máquina (MOB-01).

## Resultado esperado

El estudiante hace la prueba paso a paso y ve su puntaje; sin red igual lo ve y puede reintentar el guardado.

## Validación

1. `flutter test` (los de prueba: 22 casos) y `flutter analyze` en `projects/mobile`.
2. En un Android real con cámara frontal: girar la cabeza hacia cada punto avanza los pasos, el resultado se guarda y sin red avisa que no se guardó; verificar el sentido izquierdo/derecho (`invertirLados`).
