import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart' as img;
import 'package:image_picker/image_picker.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/tablero/models/analisis_tablero.dart';
import 'package:mobile/features/tablero/models/ayuda_tablero.dart';
import 'package:mobile/features/tablero/models/luz_tablero.dart';
import 'package:mobile/features/tablero/providers/tablero_providers.dart';
import 'package:mobile/features/tablero/screens/tablero_screen.dart';
import 'package:mobile/features/tablero/services/analizador_tablero.dart';
import 'package:mobile/features/tablero/services/ayudas_tablero_service.dart';
import 'package:mobile/features/tablero/services/clasificador_tablero.dart';
import 'package:mobile/features/tablero/services/procesar_imagen.dart';

import '../../helpers/adaptador_en_cola.dart';

Uint8List _png(int ancho, int alto, {int r = 255, int g = 0, int b = 0}) {
  final imagen = img.Image(width: ancho, height: alto);
  img.fill(imagen, color: img.ColorRgb8(r, g, b));
  return Uint8List.fromList(img.encodePng(imagen));
}

CatalogoTablero _catalogoReal() {
  final json = jsonDecode(
    File('assets/ml/luces_tablero.json').readAsStringSync(),
  );
  return CatalogoTablero.fromJson(json as Map<String, dynamic>);
}

class ClasificadorFalso implements ClasificadorTablero {
  List<Deteccion> detecciones = const [
    Deteccion(codigo: 'aceite', confianza: 0.91),
  ];
  Object? error;
  int llamadas = 0;

  @override
  String get modelo => 'tablero-falso';

  @override
  Future<List<Deteccion>> clasificar(Float32List entrada) async {
    llamadas++;
    final e = error;
    if (e != null) throw e;
    return detecciones;
  }
}

class AyudasServiceFalso extends AyudasTableroService {
  AyudasServiceFalso() : super(dio: Dio());

  Object? error;
  final List<AnalisisTablero> guardados = [];

  @override
  Future<AyudaTablero> crear(AnalisisTablero analisis) async {
    final e = error;
    if (e != null) throw e;
    guardados.add(analisis);
    return AyudaTablero(
      codigo: 'a1',
      indicadores: const [],
      respuesta: '',
      modelo: analisis.modelo,
      latenciaMs: analisis.latenciaMs,
      creadaEn: DateTime(2026, 11, 2),
    );
  }
}

void main() {
  group('catálogo real (assets/ml/luces_tablero.json)', () {
    test('tiene todas las luces con su ficha completa y un umbral válido', () {
      final catalogo = _catalogoReal();

      expect(catalogo.cantidad, greaterThanOrEqualTo(10));
      expect(catalogo.umbralConfianza, inInclusiveRange(0.3, 0.95));
      final aceite = catalogo.luz('aceite')!;
      expect(aceite.severidad, SeveridadTablero.critica);
      expect(aceite.explicacion, contains(aceite.significado));
      expect(aceite.explicacion, contains(aceite.queHacer));
      expect(catalogo.luz('no_existe'), isNull);
    });

    test('cada luz trae textos no vacíos y no se repiten los códigos', () {
      final json =
          jsonDecode(File('assets/ml/luces_tablero.json').readAsStringSync())
              as Map<String, dynamic>;
      final luces = (json['luces'] as List<dynamic>)
          .cast<Map<String, dynamic>>();

      expect({for (final l in luces) l['codigo']}.length, luces.length);
      for (final l in luces) {
        for (final campo in ['codigo', 'nombre', 'significado', 'queHacer']) {
          expect(
            (l[campo] as String).trim(),
            isNotEmpty,
            reason: '${l['codigo']}.$campo',
          );
        }
        expect(
          () => SeveridadTablero.fromJson(l['severidad'] as String),
          returnsNormally,
        );
      }
    });
  });

  group('procesarImagen', () {
    test('lleva la foto a 224 x 224 RGB y arma una miniatura JPEG', () {
      final p = procesarImagen(_png(300, 200));

      expect(p.entrada.length, ladoModelo * ladoModelo * 3);
      expect(p.entrada.sublist(0, 3), [255.0, 0.0, 0.0]);
      expect(p.entrada.reduce((a, b) => a > b ? a : b), 255.0);
      final miniatura = img.decodeJpg(p.miniatura)!;
      expect([miniatura.width, miniatura.height], [300, 200]);
    });

    test('la miniatura de una foto grande tiene el lado mayor en 512', () {
      final ancha = img.decodeJpg(procesarImagen(_png(1200, 600)).miniatura)!;
      final alta = img.decodeJpg(procesarImagen(_png(600, 1200)).miniatura)!;

      expect([ancha.width, ancha.height], [512, 256]);
      expect([alta.width, alta.height], [256, 512]);
    });

    test('lo que no es una imagen lanza ImagenInvalida', () {
      expect(
        () => procesarImagen(Uint8List.fromList([1, 2, 3])),
        throwsA(isA<ImagenInvalida>()),
      );
    });
  });

  test('mejoresDetecciones ordena de mayor a menor y se queda con k', () {
    final top = mejoresDetecciones(
      [0.1, 0.6, 0.05, 0.25],
      ['a', 'b', 'c', 'd'],
    );

    expect(top.map((d) => d.codigo), ['b', 'd', 'a']);
    expect(top.first.confianza, 0.6);
    expect(mejoresDetecciones([0.9], ['a', 'b']).length, 1);
  });

  group('AnalizadorTablero', () {
    late ClasificadorFalso clasificador;
    late AnalizadorTablero analizador;

    setUp(() {
      clasificador = ClasificadorFalso();
      analizador = AnalizadorTablero(
        clasificador: clasificador,
        preparar: (bytes) async => procesarImagen(bytes),
      );
    });

    test('une la detección con su ficha y deja la miniatura', () async {
      clasificador.detecciones = const [
        Deteccion(codigo: 'aceite', confianza: 0.8),
        Deteccion(codigo: 'bateria', confianza: 0.15),
      ];

      final analisis = await analizador.analizar(_png(50, 50), _catalogoReal());

      expect(analisis.principal!.luz.codigo, 'aceite');
      expect(analisis.luces.map((l) => l.luz.codigo), ['aceite', 'bateria']);
      expect(analisis.modelo, 'tablero-falso');
      expect(analisis.miniatura, isNotEmpty);
      expect(analisis.seguro(0.6), isTrue);
      expect(analisis.seguro(0.9), isFalse);
    });

    test(
      'descarta las luces que no están en el catálogo y sin miniatura no la deja',
      () async {
        clasificador.detecciones = const [
          Deteccion(codigo: 'desconocida', confianza: 0.9),
          Deteccion(codigo: 'frenos', confianza: 0.05),
        ];

        final analisis = await analizador.analizar(
          _png(50, 50),
          _catalogoReal(),
          conMiniatura: false,
        );

        expect(analisis.luces.map((l) => l.luz.codigo), ['frenos']);
        expect(analisis.miniatura, isNull);
        expect(analisis.seguro(0.6), isFalse);
      },
    );

    test('si el catálogo no reconoce nada no hay luz principal', () async {
      clasificador.detecciones = const [Deteccion(codigo: 'x', confianza: 0.9)];

      final analisis = await analizador.analizar(_png(50, 50), _catalogoReal());

      expect(analisis.principal, isNull);
      expect(analisis.seguro(0.1), isFalse);
    });

    test('propaga ImagenInvalida y ModeloNoDisponible', () async {
      await expectLater(
        analizador.analizar(Uint8List.fromList([0, 1]), _catalogoReal()),
        throwsA(isA<ImagenInvalida>()),
      );
      clasificador.error = const ModeloNoDisponible();
      await expectLater(
        analizador.analizar(_png(20, 20), _catalogoReal()),
        throwsA(isA<ModeloNoDisponible>()),
      );
    });
  });

  group('AyudasTableroService', () {
    setUp(
      () => dotenv.loadFromString(
        envString: 'API_BASE_URL=http://localhost:8080/api/v1',
      ),
    );

    test(
      'crear manda datos y miniatura como multipart y lee la ayuda guardada',
      () async {
        final adaptador = AdaptadorEnCola([
          RespuestaFalsa(201, {
            'status': 'success',
            'data': {
              'codigo': 'a1',
              'indicadores': [
                {
                  'codigo': 'aceite',
                  'nombre': 'Presión de aceite',
                  'confianza': 0.91,
                  'severidad': 'CRITICA',
                },
              ],
              'respuesta': 'texto',
              'modelo': 'tablero-falso',
              'latenciaMs': 12,
              'imagenUrl': 'http://x/a.jpg',
              'util': null,
              'creadaEn': '2026-11-02T10:00:00Z',
            },
            'message': 'ok',
            'timestamp': 'x',
          }),
        ]);
        ApiClient.instance.httpClientAdapter = adaptador;
        final catalogo = _catalogoReal();
        final analisis = AnalisisTablero(
          luces: [LuzReconocida(luz: catalogo.luz('aceite')!, confianza: 0.91)],
          latenciaMs: 12,
          modelo: 'tablero-falso',
          miniatura: [0xff, 0xd8, 0xff, 0x00],
        );

        final ayuda = await AyudasTableroService().crear(analisis);

        final pedido = adaptador.peticiones.single;
        expect(pedido.method, 'POST');
        expect(pedido.path, '/ayudas-tablero');
        final form = pedido.data as FormData;
        final datos =
            jsonDecode(form.fields.firstWhere((f) => f.key == 'datos').value)
                as Map<String, dynamic>;
        expect(datos['modelo'], 'tablero-falso');
        expect(datos['latenciaMs'], 12);
        expect((datos['indicadores'] as List).single['severidad'], 'CRITICA');
        expect(datos['respuesta'], catalogo.luz('aceite')!.explicacion);
        expect(form.files.single.key, 'miniatura');
        expect(ayuda.principal!.severidad, SeveridadTablero.critica);
        expect(ayuda.imagenUrl, 'http://x/a.jpg');
        expect(ayuda.creadaEn.year, 2026);
      },
    );

    test('sin miniatura no manda la parte miniatura', () async {
      final adaptador = AdaptadorEnCola([
        RespuestaFalsa(201, {
          'status': 'success',
          'data': {
            'codigo': 'a1',
            'indicadores': <Map<String, dynamic>>[],
            'respuesta': '',
            'modelo': 'm',
            'latenciaMs': 1,
            'creadaEn': '2026-11-02T10:00:00Z',
          },
          'message': 'ok',
          'timestamp': 'x',
        }),
      ]);
      ApiClient.instance.httpClientAdapter = adaptador;

      await AyudasTableroService().crear(
        const AnalisisTablero(luces: [], latenciaMs: 1, modelo: 'm'),
      );

      expect((adaptador.peticiones.single.data as FormData).files, isEmpty);
    });

    test('un error de negocio llega como ApiException', () async {
      ApiClient.instance.httpClientAdapter = AdaptadorEnCola([
        RespuestaFalsa(400, {
          'status': 'failed',
          'data': null,
          'message': 'La foto debe ser una imagen',
          'timestamp': 'x',
          'error': 'ERR_IA_01',
        }),
      ]);

      await expectLater(
        AyudasTableroService().crear(
          const AnalisisTablero(luces: [], latenciaMs: 1, modelo: 'm'),
        ),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_IA_01')),
      );
    });
  });

  group('TableroScreen', () {
    late ClasificadorFalso clasificador;
    late AyudasServiceFalso servicio;
    late Uint8List? foto;
    late List<ImageSource> origenes;

    setUp(() {
      clasificador = ClasificadorFalso();
      servicio = AyudasServiceFalso();
      foto = _png(40, 40);
      origenes = [];
    });

    Future<ProviderContainer> montar(WidgetTester tester) async {
      // * Pantalla alta: la lista es larga y los botones de abajo deben estar construidos
      tester.view.physicalSize = const Size(800, 3200);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final container = ProviderContainer(
        overrides: [
          catalogoTableroProvider.overrideWith((ref) async => _catalogoReal()),
          analizadorTableroProvider.overrideWithValue(
            AnalizadorTablero(
              clasificador: clasificador,
              preparar: (bytes) async => procesarImagen(bytes),
            ),
          ),
          ayudasTableroServiceProvider.overrideWithValue(servicio),
          selectorDeFotoProvider.overrideWithValue((origen) async {
            origenes.add(origen);
            return foto;
          }),
        ],
      );
      addTearDown(container.dispose);
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            theme: AppTheme.light,
            home: const Scaffold(body: TableroScreen()),
          ),
        ),
      );
      await tester.pumpAndSettle();
      return container;
    }

    Future<void> tocar(WidgetTester tester, String llave) async {
      await tester.ensureVisible(find.byKey(Key(llave)));
      await tester.tap(find.byKey(Key(llave)));
      await tester.pumpAndSettle();
    }

    testWidgets('al inicio explica qué hacer y avisa que es orientativo', (
      tester,
    ) async {
      await montar(tester);

      expect(find.text('Analiza una luz del tablero'), findsOneWidget);
      expect(find.byKey(const Key('avisoOrientativo')), findsOneWidget);
    });

    testWidgets(
      'una luz segura muestra qué significa y qué hacer, y la guarda',
      (tester) async {
        await montar(tester);

        await tocar(tester, 'botonTomarFoto');

        expect(origenes, [ImageSource.camera]);
        expect(find.byKey(const Key('resultadoTablero')), findsOneWidget);
        expect(find.text('Presión de aceite'), findsOneWidget);
        expect(find.text('Crítica'), findsOneWidget);
        expect(find.text('Confianza 91%'), findsOneWidget);
        expect(
          find.textContaining('presión del aceite del motor'),
          findsOneWidget,
        );
        expect(find.byKey(const Key('guardado')), findsOneWidget);
        expect(servicio.guardados.single.principal!.luz.codigo, 'aceite');
        expect(servicio.guardados.single.miniatura, isNotEmpty);
      },
    );

    testWidgets('elegir de la galería usa la galería', (tester) async {
      await montar(tester);

      await tocar(tester, 'botonElegirDeGaleria');

      expect(origenes, [ImageSource.gallery]);
      expect(find.byKey(const Key('resultadoTablero')), findsOneWidget);
    });

    testWidgets('apagar «guardar la foto» guarda el resultado sin miniatura', (
      tester,
    ) async {
      await montar(tester);

      await tocar(tester, 'interruptorGuardarFoto');
      await tocar(tester, 'botonTomarFoto');

      expect(servicio.guardados.single.miniatura, isNull);
    });

    testWidgets(
      'con poca confianza no afirma nada, pide otra foto y no guarda',
      (tester) async {
        clasificador.detecciones = const [
          Deteccion(codigo: 'aceite', confianza: 0.3),
          Deteccion(codigo: 'bateria', confianza: 0.2),
        ];
        await montar(tester);

        await tocar(tester, 'botonTomarFoto');

        expect(find.byKey(const Key('noEstoySeguro')), findsOneWidget);
        expect(find.byKey(const Key('resultadoTablero')), findsNothing);
        expect(servicio.guardados, isEmpty);
      },
    );

    testWidgets(
      'sin conexión el resultado se queda y se puede reintentar el guardado',
      (tester) async {
        servicio.error = DioException(
          requestOptions: RequestOptions(path: '/x'),
          type: DioExceptionType.connectionError,
        );
        await montar(tester);

        await tocar(tester, 'botonTomarFoto');

        expect(find.byKey(const Key('resultadoTablero')), findsOneWidget);
        expect(find.byKey(const Key('noGuardado')), findsOneWidget);

        servicio.error = null;
        await tocar(tester, 'botonReintentarGuardado');

        expect(find.byKey(const Key('guardado')), findsOneWidget);
        expect(servicio.guardados.length, 1);
      },
    );

    testWidgets(
      'si el modelo no está avisa que el análisis no está disponible',
      (tester) async {
        clasificador.error = const ModeloNoDisponible();
        await montar(tester);

        await tocar(tester, 'botonTomarFoto');

        expect(find.byKey(const Key('errorTablero')), findsOneWidget);
        expect(find.textContaining('no está disponible'), findsOneWidget);
      },
    );

    testWidgets('una foto que no se puede leer avisa', (tester) async {
      foto = Uint8List.fromList([1, 2, 3, 4]);
      await montar(tester);

      await tocar(tester, 'botonTomarFoto');

      expect(find.textContaining('No pudimos leer esa foto'), findsOneWidget);
      expect(clasificador.llamadas, 0);
    });

    testWidgets('cancelar la cámara no cambia nada', (tester) async {
      foto = null;
      await montar(tester);

      await tocar(tester, 'botonTomarFoto');

      expect(find.text('Analiza una luz del tablero'), findsOneWidget);
      expect(clasificador.llamadas, 0);
    });
  });
}
