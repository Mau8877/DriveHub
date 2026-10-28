# UT-CU40-MOB-01 — Catálogo de luces y entrenamiento del modelo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU40-MOB-01` |
| **Nombre** | Catálogo de luces y entrenamiento del modelo |
| **Caso de uso** | CU40-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md).

## Solicitud de negocio

La app necesita saber qué luces del tablero reconoce y cómo explicarlas, y un modelo entrenable y repetible que las reconozca.

## Objetivo

Existen el catálogo de luces, el script que entrena y exporta el clasificador y la guía para correrlo, y las dependencias de IA en el dispositivo compilan en Android.

## Alcance

- Dependencias `tflite_flutter`, `camera`, `google_mlkit_face_detection` e `image` (las dos primeras de IA en el dispositivo; `camera` y ML Kit los usa CU39) y `kotlin.jvm.target.validation.mode=warning` en `android/gradle.properties` (resultado del spike: `flutter build apk --debug` compila).
- `assets/ml/luces_tablero.json`: 16 luces con código, nombre, gravedad, significado y qué hacer, y el umbral de confianza. `pubspec.yaml` declara `assets/ml/`.
- `ml/entrenar_tablero.py` (MobileNetV3-Small con transfer learning, afinado, exigencia de exactitud mínima en validación, exportación a `tablero.tflite` y `tablero_labels.json`), `ml/requirements-ml.txt`, `ml/README.md` (fotos, licencias, cómo entrenar y probar) y `ml/.gitignore`.
- `ARQUITECTURA_MOBILE.md`: la carpeta `ml/` y los `assets/ml/`.

Archivos esperados (crear/tocar): `pubspec.yaml`, `pubspec.lock`, `android/gradle.properties`, `assets/ml/luces_tablero.json`, los de `ml/` y la rule.

## Fuera de alcance (no tocar)

- Entrenar con un dataset real y empaquetar el `.tflite` (lo hace el responsable con el script).
- El código de la pantalla y del clasificador (MOB-02).

## Resultado esperado

`flutter build apk --debug` compila con las dependencias de IA y quien tenga fotos puede entrenar el modelo con un solo comando.

## Validación

1. `flutter build apk --debug` en `projects/mobile` (compila el APK).
2. `python entrenar_tablero.py --datos datos` con fotos de prueba imprime exactitud y aciertos por luz, y escribe `tablero.tflite` solo si llega al mínimo.
