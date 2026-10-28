import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/manuales/models/manual.dart';
import 'package:mobile/features/manuales/providers/manuales_providers.dart';
import 'package:mobile/features/manuales/screens/manuales_screen.dart';
import 'package:mobile/features/manuales/services/manuales_service.dart';
import 'package:mobile/shared/widgets/visor_pdf_screen.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _manualJson({
  String codigo = 'm1',
  String titulo = 'Señales de tránsito',
}) => {
  'codigo': codigo,
  'titulo': titulo,
  'descripcion': 'Todas las señales',
  'categoria': 'B',
  'tema': 'Señalización',
  'version': 2,
  'estado': 'PUBLICADO',
  'etiquetas': ['vial', 'señales'],
  'archivo': {
    'url': 'http://localhost:9000/drivehub-contenido/m1.pdf',
    'nombre': 'senales.pdf',
    'mime': 'application/pdf',
    'tamanoBytes': 2097152,
  },
};

Map<String, dynamic> _sobreLista(List<Map<String, dynamic>> data) => {
  'status': 'success',
  'data': data,
  'message': 'ok',
  'timestamp': 'x',
  'meta': {
    'page': 1,
    'pageSize': 50,
    'totalItems': data.length,
    'totalPages': 1,
  },
};

class ManualesServiceFalso extends ManualesService {
  ManualesServiceFalso() : super(dio: Dio());

  List<Manual> manuales = [];
  Object? error;
  final List<(String?, String?)> pedidos = [];

  @override
  Future<Pagina<Manual>> publicados({
    String? busqueda,
    String? categoria,
    int page = 1,
    int pageSize = 50,
  }) async {
    pedidos.add((busqueda, categoria));
    final e = error;
    if (e != null) throw e;
    return Pagina(
      items: manuales,
      page: 1,
      pageSize: pageSize,
      totalItems: manuales.length,
      totalPages: 1,
    );
  }
}

Manual _manual({String codigo = 'm1', String titulo = 'Señales de tránsito'}) =>
    Manual.fromJson(_manualJson(codigo: codigo, titulo: titulo));

void main() {
  group('ManualesService', () {
    setUp(
      () => dotenv.loadFromString(
        envString: 'API_BASE_URL=http://localhost:8080/api/v1',
      ),
    );

    test(
      'publicados pide la página con búsqueda y categoría y lee los manuales',
      () async {
        final adaptador = AdaptadorEnCola([
          RespuestaFalsa(200, _sobreLista([_manualJson()])),
        ]);
        ApiClient.instance.httpClientAdapter = adaptador;

        final pagina = await ManualesService().publicados(
          busqueda: '  señales ',
          categoria: 'B',
        );

        final pedido = adaptador.peticiones.single;
        expect(pedido.path, '/manuales/publicados');
        expect(pedido.queryParameters, {
          'page': 1,
          'pageSize': 50,
          'search': 'señales',
          'filter[categoria]': 'B',
        });
        final manual = pagina.items.single;
        expect(manual.titulo, 'Señales de tránsito');
        expect(manual.etiquetas, ['vial', 'señales']);
        expect(manual.tamanoBytes, 2097152);
        expect(manual.archivoUrl, contains('m1.pdf'));
      },
    );

    test('sin búsqueda ni categoría no manda filtros', () async {
      final adaptador = AdaptadorEnCola([RespuestaFalsa(200, _sobreLista([]))]);
      ApiClient.instance.httpClientAdapter = adaptador;

      await ManualesService().publicados(busqueda: '  ');

      expect(adaptador.peticiones.single.queryParameters, {
        'page': 1,
        'pageSize': 50,
      });
    });

    test('un error de negocio llega como ApiException', () async {
      ApiClient.instance.httpClientAdapter = AdaptadorEnCola([
        RespuestaFalsa(403, {
          'status': 'failed',
          'data': null,
          'message': 'sin permiso',
          'timestamp': 'x',
          'error': 'ERR_SYS_05',
        }),
      ]);

      await expectLater(
        ManualesService().publicados(),
        throwsA(
          isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_05'),
        ),
      );
    });
  });

  group('ManualesScreen', () {
    late ManualesServiceFalso servicio;
    VisorPdfArgs? abierto;

    setUp(() {
      servicio = ManualesServiceFalso();
      abierto = null;
    });

    Future<void> montar(WidgetTester tester) async {
      final container = ProviderContainer(
        overrides: [manualesServiceProvider.overrideWithValue(servicio)],
      );
      addTearDown(container.dispose);
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (context, state) => const Scaffold(body: ManualesScreen()),
          ),
          GoRoute(
            path: '/pdf',
            builder: (context, state) {
              abierto = state.extra as VisorPdfArgs;
              return const Text('visor');
            },
          ),
        ],
      );
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp.router(
            theme: AppTheme.light,
            routerConfig: router,
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets(
      'lista los manuales con su categoría, tema, tamaño y etiquetas',
      (tester) async {
        servicio.manuales = [_manual()];

        await montar(tester);

        expect(find.byKey(const Key('manual-m1')), findsOneWidget);
        expect(find.text('Señales de tránsito'), findsOneWidget);
        expect(find.text('Todas las señales'), findsOneWidget);
        expect(find.text('Señalización · 2.0 MB · versión 2'), findsOneWidget);
        expect(find.text('vial'), findsOneWidget);
      },
    );

    testWidgets('sin manuales muestra el estado vacío', (tester) async {
      await montar(tester);

      expect(find.text('Todavía no hay manuales'), findsOneWidget);
    });

    testWidgets('la categoría y la búsqueda se piden al servicio', (
      tester,
    ) async {
      servicio.manuales = [_manual()];
      await montar(tester);

      await tester.tap(find.byKey(const Key('categoria-B')));
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byKey(const Key('campoBuscarManual')),
        'señales',
      );
      await tester.testTextInput.receiveAction(TextInputAction.search);
      await tester.pumpAndSettle();

      expect(servicio.pedidos, [('', null), ('', 'B'), ('señales', 'B')]);
    });

    testWidgets('con un filtro sin resultados lo dice', (tester) async {
      await montar(tester);
      await tester.tap(find.byKey(const Key('categoria-A')));
      await tester.pumpAndSettle();

      expect(find.text('No encontramos manuales'), findsOneWidget);
    });

    testWidgets('un error al cargar avisa y permite reintentar', (
      tester,
    ) async {
      servicio.error = Exception('sin red');
      await montar(tester);
      expect(
        find.textContaining('No pudimos cargar los manuales'),
        findsOneWidget,
      );

      servicio.error = null;
      servicio.manuales = [_manual()];
      await tester.tap(find.byKey(const Key('botonReintentarManuales')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('manual-m1')), findsOneWidget);
    });

    testWidgets('Abrir PDF lo abre en el visor de la app con su título', (
      tester,
    ) async {
      servicio.manuales = [_manual()];
      await montar(tester);

      await tester.tap(find.byKey(const Key('botonAbrirManual-m1')));
      await tester.pumpAndSettle();

      expect(find.text('visor'), findsOneWidget);
      expect(abierto?.url, 'http://localhost:9000/drivehub-contenido/m1.pdf');
      expect(abierto?.titulo, 'Señales de tránsito');
    });
  });
}
