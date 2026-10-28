import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/estudio/screens/estudio_screen.dart';
import 'package:mobile/features/manuales/providers/manuales_providers.dart';
import 'package:mobile/features/resultados/models/resultado.dart';
import 'package:mobile/features/resultados/providers/resultados_providers.dart';
import 'package:mobile/features/resultados/screens/resultados_screen.dart';
import 'package:mobile/features/resultados/services/resultados_service.dart';

import '../../helpers/adaptador_en_cola.dart';
import '../../helpers/simulacros_falsos.dart';
import '../manuales/manuales_test.dart' show ManualesServiceFalso;

Map<String, dynamic> _notaJson({
  String codigo = 'r1',
  String origen = 'SIMULACRO',
  bool aprobado = true,
  num puntaje = 80,
  String? nombre,
}) => {
  'codigo': codigo,
  'estudianteCodigo': 'e1',
  'estudianteNombre': nombre,
  'origen': origen,
  'titulo': 'Simulacro: Básico',
  'puntaje': puntaje,
  'aprobado': aprobado,
  'fecha': '2026-11-03T02:00:00Z',
  'dia': '2026-11-02',
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

class ResultadosServiceFalso extends ResultadosService {
  ResultadosServiceFalso() : super(dio: Dio());

  List<Resultado> notas = [];
  Object? error;
  final List<OrigenResultado?> pedidos = [];
  List<Resultado> deAlumnos = [];
  final List<OrigenResultado?> pedidosAlumnos = [];

  @override
  Future<Pagina<Resultado>> deMisAlumnos({
    OrigenResultado? origen,
    int page = 1,
    int pageSize = 50,
  }) async {
    pedidosAlumnos.add(origen);
    final e = error;
    if (e != null) throw e;
    final filtradas = origen == null
        ? deAlumnos
        : deAlumnos.where((n) => n.origen == origen).toList();
    return Pagina(
      items: filtradas,
      page: 1,
      pageSize: pageSize,
      totalItems: filtradas.length,
      totalPages: 1,
    );
  }

  @override
  Future<Pagina<Resultado>> mios({
    OrigenResultado? origen,
    int page = 1,
    int pageSize = 50,
  }) async {
    pedidos.add(origen);
    final e = error;
    if (e != null) throw e;
    final filtradas = origen == null
        ? notas
        : notas.where((n) => n.origen == origen).toList();
    return Pagina(
      items: filtradas,
      page: 1,
      pageSize: pageSize,
      totalItems: filtradas.length,
      totalPages: 1,
    );
  }
}

Resultado _nota({
  String codigo = 'r1',
  OrigenResultado origen = OrigenResultado.simulacro,
  bool aprobado = true,
  String titulo = 'Simulacro: Básico',
  String? nombre,
}) => Resultado(
  codigo: codigo,
  origen: origen,
  titulo: titulo,
  estudianteNombre: nombre,
  puntaje: 80,
  aprobado: aprobado,
  dia: DateTime(2026, 11, 2),
);

void main() {
  group('ResultadosService', () {
    late ResultadosService servicio;

    AdaptadorEnCola responder(int status, Map<String, dynamic> cuerpo) {
      final adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
      ApiClient.instance.httpClientAdapter = adaptador;
      return adaptador;
    }

    setUp(() {
      dotenv.loadFromString(
        envString: 'API_BASE_URL=http://localhost:8080/api/v1',
      );
      servicio = ResultadosService();
    });

    test('mios pide la página y lee las notas', () async {
      final adaptador = responder(200, _sobreLista([_notaJson()]));

      final pagina = await servicio.mios();

      expect(adaptador.peticiones.single.path, '/resultados/mios');
      expect(adaptador.peticiones.single.queryParameters, {
        'page': 1,
        'pageSize': 50,
      });
      expect(pagina.items.single.origen, OrigenResultado.simulacro);
      expect(pagina.items.single.puntaje, 80.0);
      expect(pagina.items.single.dia, DateTime(2026, 11, 2));
    });

    test('con un tipo manda filter[origen] y lee los tres tipos', () async {
      final adaptador = responder(
        200,
        _sobreLista([
          _notaJson(
            origen: 'EXAMEN_CONDUCCION',
            aprobado: false,
            puntaje: 55.5,
          ),
          _notaJson(codigo: 'r2', origen: 'PRUEBA_CLASE'),
        ]),
      );

      final pagina = await servicio.mios(
        origen: OrigenResultado.examenConduccion,
      );

      expect(
        adaptador.peticiones.single.queryParameters['filter[origen]'],
        'EXAMEN_CONDUCCION',
      );
      expect(pagina.items.map((n) => n.origen), [
        OrigenResultado.examenConduccion,
        OrigenResultado.pruebaClase,
      ]);
      expect(pagina.items.first.aprobado, isFalse);
    });

    test(
      'deMisAlumnos pide la ruta del instructor y lee el nombre del alumno',
      () async {
        final adaptador = responder(
          200,
          _sobreLista([_notaJson(nombre: 'Ana Pérez')]),
        );

        final pagina = await servicio.deMisAlumnos(
          origen: OrigenResultado.simulacro,
        );

        expect(adaptador.peticiones.single.path, '/resultados/de-mis-alumnos');
        expect(adaptador.peticiones.single.queryParameters, {
          'page': 1,
          'pageSize': 50,
          'filter[origen]': 'SIMULACRO',
        });
        expect(pagina.items.single.estudianteNombre, 'Ana Pérez');
      },
    );

    test('un error de negocio llega como ApiException', () async {
      responder(403, {
        'status': 'failed',
        'data': null,
        'message': 'sin permiso',
        'timestamp': 'x',
        'error': 'ERR_SYS_05',
      });

      await expectLater(
        servicio.mios(),
        throwsA(
          isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_05'),
        ),
      );
    });
  });

  group('ResultadosScreen', () {
    late ResultadosServiceFalso servicio;

    setUp(() => servicio = ResultadosServiceFalso());

    Future<void> montar(WidgetTester tester, {Widget? pantalla}) async {
      final container = ProviderContainer(
        overrides: [
          resultadosServiceProvider.overrideWithValue(servicio),
          simulacrosServiceFalso(SimulacrosServiceFalso()),
          manualesServiceProvider.overrideWithValue(ManualesServiceFalso()),
        ],
      );
      addTearDown(container.dispose);
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            theme: AppTheme.light,
            home: Scaffold(body: pantalla ?? const ResultadosScreen()),
          ),
        ),
      );
      await tester.pumpAndSettle();
    }

    testWidgets('lista las notas con su tipo, fecha, puntaje y resultado', (
      tester,
    ) async {
      servicio.notas = [
        _nota(),
        _nota(
          codigo: 'r2',
          origen: OrigenResultado.examenConduccion,
          aprobado: false,
          titulo: 'Examen de conducción, categoría B (intento 1)',
        ),
      ];

      await montar(tester);

      expect(find.byKey(const Key('nota-r1')), findsOneWidget);
      expect(find.text('Simulacro: Básico'), findsOneWidget);
      expect(
        find.textContaining('Simulacro · 02 nov 2026 · 80%'),
        findsOneWidget,
      );
      expect(find.text('Aprobado'), findsOneWidget);
      expect(find.text('No aprobado'), findsOneWidget);
    });

    testWidgets('sin notas muestra el estado vacío', (tester) async {
      await montar(tester);

      expect(find.text('Todavía no tienes notas'), findsOneWidget);
    });

    testWidgets('el filtro pide solo ese tipo y un tipo sin notas lo dice', (
      tester,
    ) async {
      servicio.notas = [_nota()];

      await montar(tester);
      await tester.tap(find.byKey(const Key('filtro-EXAMEN_CONDUCCION')));
      await tester.pumpAndSettle();

      expect(servicio.pedidos, [null, OrigenResultado.examenConduccion]);
      expect(find.text('No hay notas de este tipo'), findsOneWidget);
    });

    testWidgets('un error al cargar avisa y permite reintentar', (
      tester,
    ) async {
      servicio.error = Exception('sin red');

      await montar(tester);
      expect(
        find.textContaining('No pudimos cargar tus notas'),
        findsOneWidget,
      );
      servicio.error = null;
      servicio.notas = [_nota()];
      await tester.tap(find.byKey(const Key('botonReintentarNotas')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('nota-r1')), findsOneWidget);
    });

    testWidgets('la vista del instructor muestra el nombre de cada alumno', (
      tester,
    ) async {
      servicio.deAlumnos = [
        _nota(nombre: 'Ana Pérez'),
        _nota(codigo: 'r2', nombre: 'Luis Gómez', aprobado: false),
      ];

      await montar(tester, pantalla: const ResultadosScreen(deAlumnos: true));

      expect(
        find.textContaining('Ana Pérez · Simulacro · 02 nov 2026 · 80%'),
        findsOneWidget,
      );
      expect(find.textContaining('Luis Gómez'), findsOneWidget);
      expect(find.text('No aprobado'), findsOneWidget);
      expect(servicio.pedidos, isEmpty);
      expect(servicio.pedidosAlumnos, [null]);
    });

    testWidgets('la vista del instructor sin notas, con filtro y con error', (
      tester,
    ) async {
      await montar(tester, pantalla: const ResultadosScreen(deAlumnos: true));
      expect(find.text('Tus alumnos todavía no tienen notas'), findsOneWidget);

      await tester.tap(find.byKey(const Key('filtro-EXAMEN_CONDUCCION')));
      await tester.pumpAndSettle();
      expect(servicio.pedidosAlumnos, [null, OrigenResultado.examenConduccion]);
      expect(find.text('No hay notas de este tipo'), findsOneWidget);

      servicio.error = Exception('sin red');
      await tester.tap(find.byKey(const Key('filtro-TODAS')));
      await tester.pumpAndSettle();
      expect(
        find.textContaining('No pudimos cargar las notas de tus alumnos'),
        findsOneWidget,
      );
      servicio.error = null;
      servicio.deAlumnos = [_nota(nombre: 'Ana Pérez')];
      await tester.tap(find.byKey(const Key('botonReintentarNotas')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('nota-r1')), findsOneWidget);
    });

    testWidgets(
      '«Estudio» tiene las pestañas Simulacros, Manuales y Mis notas',
      (tester) async {
        servicio.notas = [_nota()];

        await montar(tester, pantalla: const EstudioScreen());

        expect(find.text('Simulacros'), findsWidgets);
        await tester.tap(find.text('Mis notas'));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('nota-r1')), findsOneWidget);
      },
    );
  });
}
