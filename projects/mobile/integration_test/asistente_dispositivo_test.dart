import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_litert_lm/flutter_litert_lm.dart' show LiteLmBackend;
import 'package:integration_test/integration_test.dart';
import 'package:mobile/features/asistente/models/catalogo_acciones.dart';
import 'package:mobile/features/asistente/models/resultado_interpretacion.dart';
import 'package:mobile/features/asistente/services/modelo_de_acciones.dart';
import 'package:mobile/features/asistente/services/motor_litert_lm.dart';
import 'package:mobile/features/asistente/services/reconocedor_de_voz.dart';
import 'package:mobile/features/asistente/services/transcriptor_whisper.dart';
import 'package:path_provider/path_provider.dart';

/// Prueba del asistente de voz (CU50) EN UN TELÉFONO, con los modelos reales: Whisper convierte unos audios en
/// texto y el modelo de acciones elige la acción. Mide cuánto tarda cada paso.
///
/// * No corre en `flutter test` normal: necesita un Android con los modelos copiados en `<documentos>/ia/`
///   (`acciones.litertlm` y `whisper/`) y los audios en `<documentos>/ia/test_wavs/` (`f1.wav`... a 16 kHz, mono,
///   16 bits). Se ejecuta con `flutter test integration_test/asistente_dispositivo_test.dart -d <dispositivo>`.
/// * Es una medición, no una barrera: imprime una tabla y solo falla si un modelo no carga.
/// `--dart-define=BACKEND=gpu` prueba la GPU; por defecto, CPU.
const _backend = String.fromEnvironment('BACKEND', defaultValue: 'cpu');

/// `--dart-define=VARIANTE=corto` manda al modelo descripciones recortadas (la primera frase de cada función y sin
/// la descripción de los parámetros), para medir cuánto del tiempo se va en procesar el texto de las funciones.
const _variante = String.fromEnvironment('VARIANTE', defaultValue: 'completo');

CatalogoAcciones _recortar(CatalogoAcciones c) => CatalogoAcciones({
  for (final e in c.acciones.entries)
    e.key: AccionDelCatalogo(
      nombre: e.value.nombre,
      descripcion: e.value.descripcion.split('. ').first,
      parametros: {
        for (final p in e.value.parametros.entries)
          p.key: ParametroAccion(nombre: p.value.nombre, tipo: p.value.tipo, descripcion: p.value.nombre, valores: p.value.valores),
      },
      requeridos: e.value.requeridos,
    ),
});

const _casos = <(String, String)>[
  ('f1', 'ver_mis_clases'),
  ('f2', 'ver_horas_disponibles'),
  ('f3', 'buscar_horarios'),
  ('f4', 'cancelar_clase'),
  ('f5', 'reprogramar_clase'),
  ('f6', 'ver_historial'),
  ('f7', 'fuera_de_alcance'),
];

/// Las muestras de un WAV PCM de 16 bits, normalizadas a `[-1, 1]` (busca el bloque `data`).
Float32List _leerWav(File archivo) {
  final bytes = archivo.readAsBytesSync();
  var i = 12;
  while (i + 8 <= bytes.length) {
    final id = String.fromCharCodes(bytes.sublist(i, i + 4));
    final tamano = ByteData.sublistView(bytes, i + 4, i + 8).getUint32(0, Endian.little);
    if (id == 'data') {
      final fin = (i + 8 + tamano).clamp(0, bytes.length);
      return pcm16AFloat32(Uint8List.sublistView(bytes, i + 8, fin));
    }
    i += 8 + tamano;
  }
  throw StateError('El WAV no tiene bloque data: ${archivo.path}');
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('voz y modelo reales en el teléfono', (tester) async {
    final docs = (await getApplicationDocumentsDirectory()).path;
    final s = Platform.pathSeparator;
    final transcriptor = TranscriptorWhisper(carpetaDelModelo: '$docs${s}ia${s}whisper');
    final completo = await CatalogoAcciones.cargar();
    final catalogo = _variante == 'corto' ? _recortar(completo) : completo;
    final modelo = ModeloDeAccionesLitert(
      motor: MotorLitertLm(
        rutaModelo: '$docs${s}ia${s}acciones.litertlm',
        catalogo: catalogo,
        backend: LiteLmBackend.values.byName(_backend),
      ),
      catalogo: catalogo,
    );
    addTearDown(modelo.liberar);

    final filas = <String>[];
    var accionesCorrectas = 0;
    for (final (id, esperada) in _casos) {
      final muestras = _leerWav(File('$docs${s}ia${s}test_wavs$s$id.wav'));

      final t0 = Stopwatch()..start();
      final texto = (await transcriptor.transcribir(muestras, frecuenciaDeVoz)).trim();
      final msVoz = t0.elapsedMilliseconds;

      final t1 = Stopwatch()..start();
      final r = await modelo.entender(texto);
      final msModelo = t1.elapsedMilliseconds;

      final obtenida = switch (r) {
        AccionEntendida(:final accion) => accion.accion.nombre,
        AccionRechazada(:final motivo) => 'rechazo:${motivo.name}',
      };
      final detalle = r is AccionEntendida
          ? ' (dia: ${r.accion.dia}, momento: ${r.accion.momento}, instructor: ${r.accion.instructor}, hora: ${r.accion.hora})'
          : '';
      final bien = obtenida == esperada || (esperada == 'fuera_de_alcance' && obtenida == 'rechazo:fueraDeAlcance');
      if (bien) accionesCorrectas++;
      filas.add(
        '${bien ? 'OK ' : 'MAL'} $id  voz ${msVoz}ms  modelo ${msModelo}ms  «$texto»  ->  $obtenida$detalle  (esperada: $esperada)',
      );
    }

    // * `debugPrint` sale en la consola de `flutter test` sin recortarse
    debugPrint('\n===== RESULTADOS EN EL TELEFONO =====');
    filas.forEach(debugPrint);
    debugPrint('Acciones correctas: $accionesCorrectas/${_casos.length}');
    debugPrint('=====================================\n');

    expect(filas, hasLength(_casos.length));
  });
}
