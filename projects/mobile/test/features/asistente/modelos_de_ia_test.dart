import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/services/modelos_de_ia.dart';

/// Responde cada descarga con unos pocos bytes y anota qué rutas se pidieron; con [fallaEn] corta esa descarga.
class AdaptadorFalso implements HttpClientAdapter {
  final List<String> pedidas = [];
  String? fallaEn;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    pedidas.add(options.uri.toString());
    if (fallaEn != null && options.uri.path.endsWith(fallaEn!)) {
      throw DioException.connectionError(requestOptions: options, reason: 'sin red');
    }
    return ResponseBody.fromBytes(Uint8List.fromList([1, 2, 3]), 200);
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late Directory carpeta;
  late AdaptadorFalso adaptador;

  setUp(() async {
    carpeta = await Directory.systemTemp.createTemp('modelos_ia_');
    adaptador = AdaptadorFalso();
  });
  tearDown(() => carpeta.delete(recursive: true));

  ModelosDeIaEnArchivos crear() =>
      ModelosDeIaEnArchivos(carpeta: carpeta.path, urlBase: 'https://archivos.test/modelos/', dio: Dio()..httpClientAdapter = adaptador);

  test('sin archivos no están instalados', () async {
    expect(await crear().instalados(), isFalse);
  });

  test('descarga cada archivo a su ruta, sin dejar partes, y avanza hasta 1', () async {
    final modelos = crear();
    final avances = <double>[];

    await modelos.descargar(avances.add);

    expect(await modelos.instalados(), isTrue);
    expect(adaptador.pedidas, archivosDeModelosIa.keys.map((r) => 'https://archivos.test/modelos/$r'));
    expect(File('${carpeta.path}/whisper/tiny-tokens.txt').existsSync(), isTrue);
    expect(carpeta.listSync(recursive: true).where((f) => f.path.endsWith('.parte')), isEmpty);
    expect(avances.last, 1);
  });

  test('solo baja lo que falta', () async {
    File('${carpeta.path}/acciones.litertlm').writeAsBytesSync([0]);

    await crear().descargar((_) {});

    expect(adaptador.pedidas.where((u) => u.endsWith('acciones.litertlm')), isEmpty);
    expect(adaptador.pedidas, hasLength(archivosDeModelosIa.length - 1));
  });

  test('un corte no deja el archivo como instalado', () async {
    adaptador.fallaEn = 'tiny-decoder.int8.onnx';
    final modelos = crear();

    await expectLater(modelos.descargar((_) {}), throwsA(isA<DioException>()));

    expect(File('${carpeta.path}/whisper/tiny-decoder.int8.onnx').existsSync(), isFalse);
    expect(await modelos.instalados(), isFalse);
  });
}
