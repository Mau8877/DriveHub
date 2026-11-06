# UT-CU50-MOB-06 — Reconocimiento de voz local (Whisper)

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU50-MOB-06` |
| **Nombre** | Reconocimiento de voz local (Whisper) |
| **Caso de uso** | CU50-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Documentación de los plugins `record` y `sherpa_onnx` en pub.dev.

## Solicitud de negocio

El estudiante habla y lo dicho tiene que convertirse en texto dentro del teléfono: sin internet y sin que el audio salga del dispositivo.

## Objetivo

Una interfaz `ReconocedorDeVoz` con una implementación real (Whisper tiny multilingüe, español) y una falsa para los tests.

## Alcance

- `pubspec.yaml`: `record` y `sherpa_onnx`; comprobar que compilan en Android.
- `features/asistente/services/reconocedor_de_voz.dart`: `abstract class ReconocedorDeVoz { Future<void> empezar(); Future<String> terminar(); Future<void> cancelar(); }` y `ModeloDeVozNoDisponible`.
- `reconocedor_de_voz_whisper.dart`: graba mientras la persona mantiene pulsado (PCM de 16 bits, 16 kHz, mono, **solo en memoria: no se escribe ningún archivo**) y lo descarta siempre al terminar, también si falla; transcribe con Whisper tiny (español, int8) en otro hilo y devuelve el texto; audio de menos de 0,3 s → texto vacío; más de 30 s se corta (límite de Whisper). Los archivos del modelo salen de `<documentos>/ia/whisper/` (`tiny-encoder.int8.onnx`, `tiny-decoder.int8.onnx`, `tiny-tokens.txt`).
- Separación para probar sin hardware: `GrabadorDeAudio` (real: `GrabadorRecord`), `TranscriptorDeVoz` (real: `TranscriptorWhisper`) y `ReconocedorDeVozPorGrabacion`, que los une. `PermisoDeMicrofonoDenegado` lo lanza el grabador.
- Provider `reconocedorDeVozProvider`.
- Tests con el reconocedor falso (texto, vacío, error) y uno que comprueba que el audio se descarta aun si falla la transcripción. **No** se abre el micrófono en `flutter test`.

Archivos esperados (crear/tocar): `pubspec.yaml`, los dos archivos de `services/`, el provider y el test.

## Fuera de alcance (no tocar)

- Permisos de Android (MOB-09), el botón (MOB-08), guardar el audio o enviarlo a cualquier lugar (no se hace) y la entrega del modelo de voz (follow-up).

## Resultado esperado

En un Android real y sin internet, decir «tengo práctica el viernes» devuelve ese texto y no se escribe ningún archivo de audio en el teléfono.

## Validación

1. `flutter test test/features/asistente/reconocedor_de_voz_test.dart` y `flutter analyze`.
2. `flutter build apk --debug` compila.
3. En un Android real, en modo avión: grabar una frase y comprobar el texto, la latencia y que no aparece ningún archivo de audio en el almacenamiento de la app.
