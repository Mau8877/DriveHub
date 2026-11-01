# UT-CU39-MOB-01 — Máquina de la prueba y detector de rostro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU39-MOB-01` |
| **Nombre** | Máquina de la prueba y detector de rostro |
| **Caso de uso** | CU39-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU40-MOB-01 (dependencias `camera` y `google_mlkit_face_detection`) |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md) (sección «IA en el dispositivo») y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md).

## Solicitud de negocio

La app debe poder decidir, con la cámara y sin internet, si la persona mira cada punto que se le pide.

## Objetivo

Existen la lógica pura de la prueba y el detector de rostros del teléfono.

## Alcance

- `features/prueba/models/prueba.dart`: `Medicion`, `TipoPaso`, `PasoPrueba`, `pasosDePrueba` (los cinco pasos), `ResultadoPaso` y `ResultadoPrueba`.
- `services/maquina_prueba.dart`: `ConfiguracionPrueba` (umbrales) y `MaquinaPrueba` (pasos, postura sostenida, tiempo límite, puntaje y retroalimentación), sin cámara ni reloj.
- `services/detector_rostro.dart`: la interfaz `DetectorRostro`, `PermisoCamaraDenegado`, `CamaraNoDisponible` y `DetectorRostroMlKit` (cámara frontal, formato NV21, detección de rostros con clasificación de ojos).
- `AndroidManifest.xml`: permiso `CAMERA` y `uses-feature` no obligatorios.
- Tests: la parte de `MaquinaPrueba` de `test/features/prueba/prueba_test.dart` (prueba perfecta, postura que se corta, tiempo agotado, puntaje y mensajes, ojos, lados invertidos y ángulo insuficiente).

Archivos esperados (crear/tocar): los de `lib/features/prueba/{models,services}`, `AndroidManifest.xml` y el test.

## Fuera de alcance (no tocar)

- La pantalla, el estado de la prueba y el guardado (MOB-02).

## Resultado esperado

Dadas mediciones de la cabeza y la hora, la máquina decide qué pasos se lograron y entrega el resultado.

## Validación

1. `flutter test test/features/prueba` y `flutter analyze` en `projects/mobile`.
2. En un Android real: el detector entrega mediciones al girar la cabeza (se comprueba con la UT siguiente).
