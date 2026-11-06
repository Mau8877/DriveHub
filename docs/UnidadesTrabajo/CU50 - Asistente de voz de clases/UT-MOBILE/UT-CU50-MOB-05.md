# UT-CU50-MOB-05 — Modelo de acciones en el teléfono (LiteRT-LM)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-05` |
| **Nombre** | Modelo de acciones en el teléfono (LiteRT-LM) |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU50-MOB-01 |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Documentación del plugin `flutter_litert_lm` en pub.dev.

## Solicitud de negocio

Es lo que permite entender la frase sin internet: el modelo FunctionGemma afinado corriendo dentro del teléfono.

## Objetivo

Una interfaz `ModeloDeAcciones` con una implementación real que carga el `.litertlm` y devuelve la acción elegida, y una falsa para los tests.

## Alcance

- `pubspec.yaml`: `flutter_litert_lm`. Comprobar que compila en Android con Flutter 3.47 (`flutter build apk --debug`); si hace falta un ajuste de Gradle, se anota en [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md) como se hizo con `tflite_flutter`.
- `features/asistente/services/modelo_de_acciones.dart`: `abstract class ModeloDeAcciones { Future<ResultadoInterpretacion> entender(String texto); void liberar(); }` y `ModeloNoDisponible`.
- `modelo_de_acciones_litert.dart`: crea el motor con la ruta `<documentos>/ia/acciones.litertlm` (CPU), una conversación por petición con el mensaje `developer` del entrenamiento, las herramientas del catálogo (MOB-01) y temperatura 0,1; el plugin **no ejecuta** las herramientas, solo devuelve la llamada. La salida pasa por `interpretar` (MOB-01). El motor se crea una vez y se reutiliza, y la inferencia no bloquea la interfaz.
- Provider `modeloDeAccionesProvider`.
- Tests: `test/features/asistente/modelo_de_acciones_test.dart` con un modelo falso (archivo ausente → `ModeloNoDisponible`; salida válida e inválida). **No** se carga un `.litertlm` en `flutter test`.

Archivos esperados (crear/tocar): `pubspec.yaml`, los dos archivos de `services/`, el provider y el test.

## Fuera de alcance (no tocar)

- La voz (MOB-06), qué se hace con la acción (MOB-03 y MOB-04), y la descarga o el empaquetado del `.litertlm` en producción (follow-up).

## Resultado esperado

En un Android real, con el modelo copiado a la carpeta de la app, «tengo práctica el viernes?» devuelve `ver_mis_clases` con `dia: viernes`.

## Validación

1. `flutter test test/features/asistente/modelo_de_acciones_test.dart` y `flutter analyze`.
2. `flutter build apk --debug` compila.
3. En un Android real: copiar el modelo con `adb push` a la carpeta de la app, ejecutar y comprobar la acción devuelta, el tiempo de respuesta y que no se cierra por memoria.
