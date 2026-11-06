import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/services/reconocedor_de_voz.dart';
import 'package:mobile/features/asistente/services/transcriptor_whisper.dart';

class GrabadorFalso implements GrabadorDeAudio {
  StreamController<Uint8List>? flujo;
  Object? errorAlEmpezar;
  int empezadas = 0;
  int detenidas = 0;

  @override
  Future<Stream<Uint8List>> empezar() async {
    final e = errorAlEmpezar;
    if (e != null) throw e;
    empezadas++;
    flujo = StreamController<Uint8List>();
    return flujo!.stream;
  }

  /// Entrega audio como si llegara del micrófono.
  Future<void> hablar(Uint8List bytes) async {
    flujo!.add(bytes);
    await Future<void>.delayed(Duration.zero);
  }

  @override
  Future<void> detener() async => detenidas++;

  @override
  Future<void> liberar() async {}
}

class TranscriptorFalso implements TranscriptorDeVoz {
  String texto = '  tengo práctica el viernes  ';
  Object? error;
  final List<({Float32List muestras, int frecuencia})> llamadas = [];

  @override
  Future<String> transcribir(Float32List muestras, int frecuencia) async {
    llamadas.add((muestras: muestras, frecuencia: frecuencia));
    final e = error;
    if (e != null) throw e;
    return texto;
  }
}

/// `n` muestras PCM de 16 bits, todas con el mismo [valor].
Uint8List audio(int n, {int valor = 1000}) {
  final bytes = ByteData(n * 2);
  for (var i = 0; i < n; i++) {
    bytes.setInt16(i * 2, valor, Endian.little);
  }
  return bytes.buffer.asUint8List();
}

void main() {
  late GrabadorFalso grabador;
  late TranscriptorFalso transcriptor;
  late ReconocedorDeVozPorGrabacion reconocedor;

  setUp(() {
    grabador = GrabadorFalso();
    transcriptor = TranscriptorFalso();
    reconocedor = ReconocedorDeVozPorGrabacion(grabador: grabador, transcriptor: transcriptor);
  });

  group('pcm16AFloat32', () {
    test('lleva los enteros de 16 bits a números entre -1 y 1', () {
      final bytes = ByteData(8)
        ..setInt16(0, 0, Endian.little)
        ..setInt16(2, 16384, Endian.little)
        ..setInt16(4, -32768, Endian.little)
        ..setInt16(6, 32767, Endian.little);

      final f = pcm16AFloat32(bytes.buffer.asUint8List());

      expect(f, hasLength(4));
      expect(f[0], 0);
      expect(f[1], closeTo(0.5, 1e-6));
      expect(f[2], -1);
      expect(f[3], closeTo(1, 1e-3));
    });

    test('un byte suelto al final se descarta', () {
      expect(pcm16AFloat32(Uint8List(5)), hasLength(2));
    });

    test('sin bytes no hay muestras', () {
      expect(pcm16AFloat32(Uint8List(0)), isEmpty);
    });
  });

  group('ReconocedorDeVozPorGrabacion', () {
    test('graba, transcribe a 16 kHz y devuelve el texto sin espacios de más', () async {
      await reconocedor.empezar();
      await grabador.hablar(audio(8000));
      await grabador.hablar(audio(8000));

      final texto = await reconocedor.terminar();

      expect(texto, 'tengo práctica el viernes');
      expect(transcriptor.llamadas.single.frecuencia, 16000);
      expect(transcriptor.llamadas.single.muestras, hasLength(16000));
      expect(grabador.detenidas, 1);
    });

    test('un audio de menos de 0,3 s es silencio: no llama a Whisper', () async {
      await reconocedor.empezar();
      await grabador.hablar(audio(2000));

      expect(await reconocedor.terminar(), '');
      expect(transcriptor.llamadas, isEmpty);
    });

    test('terminar sin haber empezado devuelve vacío', () async {
      expect(await reconocedor.terminar(), '');
      expect(transcriptor.llamadas, isEmpty);
    });

    test('cancelar descarta el audio y deja de grabar', () async {
      await reconocedor.empezar();
      await grabador.hablar(audio(16000));

      await reconocedor.cancelar();

      expect(grabador.detenidas, 1);
      expect(await reconocedor.terminar(), '');
      expect(transcriptor.llamadas, isEmpty);
    });

    test('si falla la transcripción se propaga y el audio no se conserva', () async {
      transcriptor.error = const ModeloDeVozNoDisponible('sin whisper');
      await reconocedor.empezar();
      await grabador.hablar(audio(16000));

      await expectLater(reconocedor.terminar(), throwsA(isA<ModeloDeVozNoDisponible>()));

      transcriptor.error = null;
      expect(await reconocedor.terminar(), '', reason: 'el audio anterior ya se descartó');
    });

    test('lo que pasa de 30 s se corta', () async {
      await reconocedor.empezar();
      for (var i = 0; i < 4; i++) {
        await grabador.hablar(audio(16000 * 10));
      }

      await reconocedor.terminar();

      expect(transcriptor.llamadas.single.muestras, hasLength(maximoDeMuestras));
    });

    test('sin permiso de micrófono no graba y lo avisa', () async {
      grabador.errorAlEmpezar = const PermisoDeMicrofonoDenegado();

      await expectLater(reconocedor.empezar(), throwsA(isA<PermisoDeMicrofonoDenegado>()));

      expect(await reconocedor.terminar(), '');
    });

    test('empezar de nuevo descarta la grabación anterior', () async {
      await reconocedor.empezar();
      await grabador.hablar(audio(16000, valor: 30000));

      await reconocedor.empezar();
      await grabador.hablar(audio(16000, valor: 100));
      await reconocedor.terminar();

      expect(grabador.empezadas, 2);
      final muestras = transcriptor.llamadas.single.muestras;
      expect(muestras, hasLength(16000));
      expect(muestras.first, lessThan(0.01), reason: 'solo quedó la segunda grabación');
    });
  });

  group('TranscriptorWhisper', () {
    test('sin los archivos del modelo avisa que no está disponible, sin tocar sherpa', () async {
      final carpeta = Directory.systemTemp.createTempSync('whisper_vacio_');
      addTearDown(() => carpeta.deleteSync(recursive: true));
      final transcriptor = TranscriptorWhisper(carpetaDelModelo: carpeta.path);

      await expectLater(
        transcriptor.transcribir(Float32List(16000), 16000),
        throwsA(isA<ModeloDeVozNoDisponible>().having((e) => e.motivo, 'motivo', contains('tiny-encoder'))),
      );
    });
  });
}
