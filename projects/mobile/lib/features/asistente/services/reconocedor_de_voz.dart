import 'dart:async';
import 'dart:typed_data';

/// Whisper no está en el teléfono (todavía no se copió) o no se pudo cargar (CU50).
class ModeloDeVozNoDisponible implements Exception {
  final String motivo;

  const ModeloDeVozNoDisponible(this.motivo);

  @override
  String toString() => 'ModeloDeVozNoDisponible: $motivo';
}

/// La persona no concedió el permiso de micrófono (CU50).
class PermisoDeMicrofonoDenegado implements Exception {
  const PermisoDeMicrofonoDenegado();

  @override
  String toString() => 'PermisoDeMicrofonoDenegado';
}

/// Convierte lo que dice la persona en texto, dentro del teléfono y sin internet (CU50).
abstract class ReconocedorDeVoz {
  /// Empieza a grabar. Lanza [PermisoDeMicrofonoDenegado] si no hay permiso.
  Future<void> empezar();

  /// Deja de grabar y devuelve lo que se dijo; texto vacío si no se oyó nada. Lanza [ModeloDeVozNoDisponible] si
  /// Whisper no está.
  Future<String> terminar();

  /// Deja de grabar y descarta el audio.
  Future<void> cancelar();
}

/// La frecuencia con la que se graba y con la que Whisper espera el audio.
const int frecuenciaDeVoz = 16000;

/// Whisper procesa como máximo 30 s de audio; lo que pase de ahí se descarta.
const int maximoDeMuestras = frecuenciaDeVoz * 30;

/// Menos de esto (0,3 s) no es una frase: se toma como que no se dijo nada.
const int minimoDeMuestras = frecuenciaDeVoz * 3 ~/ 10;

/// Graba el micrófono SIN escribir nada en disco: entrega el audio como PCM de 16 bits, mono, a [frecuenciaDeVoz].
abstract class GrabadorDeAudio {
  /// Lanza [PermisoDeMicrofonoDenegado] si no hay permiso.
  Future<Stream<Uint8List>> empezar();

  Future<void> detener();

  Future<void> liberar();
}

/// Pasa audio a texto: recibe las muestras normalizadas a `[-1, 1]` y devuelve lo que se dijo.
abstract class TranscriptorDeVoz {
  Future<String> transcribir(Float32List muestras, int frecuencia);
}

/// El audio PCM de 16 bits little-endian como números entre -1 y 1. Un byte suelto al final se descarta.
Float32List pcm16AFloat32(Uint8List bytes) {
  final muestras = bytes.lengthInBytes ~/ 2;
  final datos = ByteData.sublistView(bytes);
  final salida = Float32List(muestras);
  for (var i = 0; i < muestras; i++) {
    salida[i] = datos.getInt16(i * 2, Endian.little) / 32768.0;
  }
  return salida;
}

/// El [ReconocedorDeVoz] real: graba con un [GrabadorDeAudio] y transcribe con un [TranscriptorDeVoz].
///
/// * El audio vive solo en memoria y se descarta siempre al terminar, también si la transcripción falla: no se
///   guarda ni sale del teléfono (CA13 del CU).
/// * Mantener pulsado más de 30 s corta lo que sobra, porque Whisper no procesa más.
class ReconocedorDeVozPorGrabacion implements ReconocedorDeVoz {
  final GrabadorDeAudio _grabador;
  final TranscriptorDeVoz _transcriptor;

  BytesBuilder? _audio;
  StreamSubscription<Uint8List>? _suscripcion;

  ReconocedorDeVozPorGrabacion({required this._grabador, required this._transcriptor});

  @override
  Future<void> empezar() async {
    await _descartar();
    final audio = BytesBuilder(copy: false);
    final flujo = await _grabador.empezar();
    _audio = audio;
    _suscripcion = flujo.listen((trozo) {
      // * No se acumula más de lo que Whisper puede usar (2 bytes por muestra)
      if (audio.length < maximoDeMuestras * 2) audio.add(trozo);
    });
  }

  @override
  Future<String> terminar() async {
    final audio = _audio;
    if (audio == null) return '';
    try {
      await _suscripcion?.cancel();
      await _grabador.detener();
      var muestras = pcm16AFloat32(audio.takeBytes());
      if (muestras.length < minimoDeMuestras) return '';
      if (muestras.length > maximoDeMuestras) muestras = Float32List.sublistView(muestras, 0, maximoDeMuestras);
      return (await _transcriptor.transcribir(muestras, frecuenciaDeVoz)).trim();
    } finally {
      _audio = null;
      _suscripcion = null;
    }
  }

  @override
  Future<void> cancelar() => _descartar();

  Future<void> _descartar() async {
    final suscripcion = _suscripcion;
    final habiaAudio = _audio != null;
    _audio = null;
    _suscripcion = null;
    await suscripcion?.cancel();
    if (habiaAudio) await _grabador.detener();
  }
}
