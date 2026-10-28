import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/tablero/models/ayuda_tablero.dart';
import 'package:mobile/features/tablero/models/luz_tablero.dart';
import 'package:mobile/features/tablero/providers/tablero_providers.dart';
import 'package:mobile/features/tablero/screens/historial_ayudas_screen.dart';
import 'package:mobile/features/tablero/services/ayudas_tablero_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _ayudaJson({
  String codigo = 'a1',
  String luz = 'aceite',
  String nombre = 'Presión de aceite',
  String severidad = 'CRITICA',
  bool? util,
  String? imagenUrl,
}) => {
  'codigo': codigo,
  'indicadores': [
    {
      'codigo': luz,
      'nombre': nombre,
      'confianza': 0.91,
      'severidad': severidad,
    },
    {
      'codigo': 'bateria',
      'nombre': 'Batería / carga',
      'confianza': 0.06,
      'severidad': 'ADVERTENCIA',
    },
  ],
  'respuesta': 'La presión del aceite es baja. Detén el auto.',
  'modelo': 'tablero-v1',
  'latenciaMs': 40,
  'imagenUrl': imagenUrl,
  'util': util,
  'creadaEn': '2026-11-02T15:00:00Z',
};

AyudaTablero _ayuda({
  String codigo = 'a1',
  String nombre = 'Presión de aceite',
  bool? util,
}) => AyudaTablero.fromJson(
  _ayudaJson(codigo: codigo, nombre: nombre, util: util),
);

class HistorialServiceFalso extends AyudasTableroService {
  HistorialServiceFalso() : super(dio: Dio());

  List<AyudaTablero> ayudas = [];
  Object? error;
  Object? errorUtil;
  int pedidos = 0;
  final List<(String, bool)> opiniones = [];

  @override
  Future<Pagina<AyudaTablero>> historial({
    int page = 1,
    int pageSize = 50,
  }) async {
    pedidos++;
    final e = error;
    if (e != null) throw e;
    return Pagina(
      items: ayudas,
      page: 1,
      pageSize: pageSize,
      totalItems: ayudas.length,
      totalPages: 1,
    );
  }

  @override
  Future<AyudaTablero> marcarUtil(String codigo, {required bool util}) async {
    final e = errorUtil;
    if (e != null) throw e;
    opiniones.add((codigo, util));
    ayudas = [
      for (final a in ayudas)
        a.codigo == codigo ? _ayuda(codigo: codigo, util: util) : a,
    ];
    return ayudas.firstWhere((a) => a.codigo == codigo);
  }
}

void main() {
  group('AyudasTableroService (historial)', () {
    setUp(
      () => dotenv.loadFromString(
        envString: 'API_BASE_URL=http://localhost:8080/api/v1',
      ),
    );

    test('historial pide la página y lee las ayudas', () async {
      final adaptador = AdaptadorEnCola([
        RespuestaFalsa(200, {
          'status': 'success',
          'data': [
            _ayudaJson(imagenUrl: 'http://x/a.jpg', util: true),
            _ayudaJson(codigo: 'a2'),
          ],
          'message': 'ok',
          'timestamp': 'x',
          'meta': {'page': 1, 'pageSize': 50, 'totalItems': 2, 'totalPages': 1},
        }),
      ]);
      ApiClient.instance.httpClientAdapter = adaptador;

      final pagina = await AyudasTableroService().historial();

      expect(adaptador.peticiones.single.path, '/ayudas-tablero');
      expect(adaptador.peticiones.single.queryParameters, {
        'page': 1,
        'pageSize': 50,
      });
      expect(pagina.items.map((a) => a.codigo), ['a1', 'a2']);
      final primera = pagina.items.first;
      expect(primera.principal!.severidad, SeveridadTablero.critica);
      expect(primera.indicadores.length, 2);
      expect(primera.imagenUrl, 'http://x/a.jpg');
      expect(primera.util, isTrue);
      expect(pagina.items.last.util, isNull);
    });

    test('marcarUtil manda PATCH con la opinión y lee la ayuda', () async {
      final adaptador = AdaptadorEnCola([
        RespuestaFalsa(200, {
          'status': 'success',
          'data': _ayudaJson(util: false),
          'message': 'ok',
          'timestamp': 'x',
        }),
      ]);
      ApiClient.instance.httpClientAdapter = adaptador;

      final ayuda = await AyudasTableroService().marcarUtil('a1', util: false);

      final pedido = adaptador.peticiones.single;
      expect(pedido.method, 'PATCH');
      expect(pedido.path, '/ayudas-tablero/a1');
      expect(pedido.data, {'util': false});
      expect(ayuda.util, isFalse);
    });

    test('un error de negocio llega como ApiException', () async {
      ApiClient.instance.httpClientAdapter = AdaptadorEnCola([
        RespuestaFalsa(404, {
          'status': 'failed',
          'data': null,
          'message': 'La ayuda no existe',
          'timestamp': 'x',
          'error': 'ERR_IA_03',
        }),
      ]);

      await expectLater(
        AyudasTableroService().marcarUtil('nada', util: true),
        throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_IA_03')),
      );
    });
  });

  group('HistorialAyudasScreen', () {
    late HistorialServiceFalso servicio;

    setUp(() => servicio = HistorialServiceFalso());

    Future<void> montar(WidgetTester tester) async {
      final container = ProviderContainer(
        overrides: [ayudasTableroServiceProvider.overrideWithValue(servicio)],
      );
      addTearDown(container.dispose);
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            theme: AppTheme.light,
            home: const HistorialAyudasScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('lista las ayudas con su luz, fecha, gravedad y opinión', (
      tester,
    ) async {
      servicio.ayudas = [
        _ayuda(),
        _ayuda(codigo: 'a2', nombre: 'Frenos', util: true),
      ];

      await montar(tester);

      expect(find.byKey(const Key('ayuda-a1')), findsOneWidget);
      expect(find.text('Presión de aceite'), findsOneWidget);
      expect(find.text('Frenos'), findsOneWidget);
      expect(find.textContaining('02 nov 2026'), findsNWidgets(2));
      expect(find.textContaining('Te sirvió'), findsOneWidget);
      expect(find.text('Crítica'), findsNWidgets(2));
    });

    testWidgets('sin ayudas muestra el estado vacío', (tester) async {
      await montar(tester);

      expect(find.text('Todavía no tienes ayudas guardadas'), findsOneWidget);
    });

    testWidgets('un error al cargar avisa y permite reintentar', (
      tester,
    ) async {
      servicio.error = Exception('sin red');
      await montar(tester);
      expect(
        find.textContaining('No pudimos cargar tu historial'),
        findsOneWidget,
      );

      servicio.error = null;
      servicio.ayudas = [_ayuda()];
      await tester.tap(find.byKey(const Key('botonReintentarHistorial')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('ayuda-a1')), findsOneWidget);
    });

    testWidgets('tocar una ayuda abre su detalle con la explicación', (
      tester,
    ) async {
      servicio.ayudas = [_ayuda()];
      await montar(tester);

      await tester.tap(find.byKey(const Key('ayuda-a1')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('respuestaAyuda')), findsOneWidget);
      expect(
        find.text('La presión del aceite es baja. Detén el auto.'),
        findsOneWidget,
      );
      expect(find.text('Batería / carga · 6%'), findsOneWidget);
      expect(find.textContaining('Confianza 91%'), findsOneWidget);
    });

    testWidgets('marcar «sí me sirvió» guarda la opinión y refresca la lista', (
      tester,
    ) async {
      servicio.ayudas = [_ayuda()];
      await montar(tester);
      final pedidosAntes = servicio.pedidos;

      await tester.tap(find.byKey(const Key('ayuda-a1')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonUtil')));
      await tester.pumpAndSettle();

      expect(servicio.opiniones, [('a1', true)]);
      expect(servicio.pedidos, greaterThan(pedidosAntes));
      expect(
        tester.widget<ChoiceChip>(find.byKey(const Key('botonUtil'))).selected,
        isTrue,
      );
    });

    testWidgets('si no se puede guardar la opinión avisa y no la marca', (
      tester,
    ) async {
      servicio.ayudas = [_ayuda()];
      servicio.errorUtil = DioException(
        requestOptions: RequestOptions(path: '/x'),
        type: DioExceptionType.connectionError,
      );
      await montar(tester);

      await tester.tap(find.byKey(const Key('ayuda-a1')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonNoUtil')));
      await tester.pumpAndSettle();

      expect(
        find.textContaining('No pudimos guardar tu opinión'),
        findsOneWidget,
      );
      expect(
        tester
            .widget<ChoiceChip>(find.byKey(const Key('botonNoUtil')))
            .selected,
        isFalse,
      );
    });
  });
}
