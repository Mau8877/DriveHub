import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/tutoriales/models/tutorial.dart';
import 'package:mobile/features/tutoriales/models/tutorial_request.dart';
import 'package:mobile/features/tutoriales/models/youtube.dart';
import 'package:mobile/features/tutoriales/providers/selector_pdf_provider.dart';
import 'package:mobile/features/tutoriales/providers/tutoriales_providers.dart';
import 'package:mobile/features/tutoriales/screens/biblioteca_tutoriales_view.dart';
import 'package:mobile/features/tutoriales/screens/mis_tutoriales_view.dart';
import 'package:mobile/features/tutoriales/screens/reproductor_video_screen.dart';
import 'package:mobile/features/tutoriales/screens/tutorial_form_screen.dart';
import 'package:mobile/features/tutoriales/services/tutoriales_service.dart';
import 'package:mobile/features/tutoriales/validators/tutoriales_validators.dart';
import 'package:mobile/shared/widgets/visor_pdf_screen.dart';

import '../../helpers/adaptador_en_cola.dart';

const _youtube = 'https://youtu.be/dQw4w9WgXcQ';

Map<String, dynamic> _tutorialJson({
  String codigo = 't1',
  String tipo = 'ENLACE',
  String estado = 'PUBLICADO',
  String titulo = 'Cómo estacionar',
}) => {
  'codigo': codigo,
  'titulo': titulo,
  'descripcion': 'Paso a paso',
  'categoria': 'B',
  'tipo': tipo,
  'estado': estado,
  'audiencia': ['ESTUDIANTE', 'INSTRUCTOR'],
  'etiquetas': ['vial'],
  'recurso': tipo == 'PDF'
      ? {
          'url': 'http://localhost:9000/drivehub-contenido/t.pdf',
          'nombre': 'guia.pdf',
          'mime': 'application/pdf',
          'tamanoBytes': 2048,
        }
      : {'url': _youtube, 'nombre': null, 'mime': null, 'tamanoBytes': null},
};

Tutorial _tutorial({
  String codigo = 't1',
  String tipo = 'ENLACE',
  String estado = 'PUBLICADO',
  String titulo = 'Cómo estacionar',
}) => Tutorial.fromJson(
  _tutorialJson(codigo: codigo, tipo: tipo, estado: estado, titulo: titulo),
);

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

const _archivo = ArchivoSubido(
  url: 'http://localhost:9000/drivehub-contenido/escuelas/e/tutoriales/n.pdf',
  mime: 'application/pdf',
  tamanoBytes: 4,
  sha256: 'a1b2',
  nombre: 'guia.pdf',
);

class TutorialesServiceFalso extends TutorialesService {
  TutorialesServiceFalso() : super(dio: Dio());

  List<Tutorial> mias = [];
  List<Tutorial> publicadosLista = [];
  Object? error;
  Object? errorAccion;
  final List<EstadoTutorial?> pedidosMios = [];
  final List<(String?, TipoTutorial?)> pedidosPublicados = [];
  final List<String> acciones = [];
  TutorialRequest? ultimoRequest;
  String? nombreSubido;

  Pagina<Tutorial> _pagina(List<Tutorial> items) => Pagina(
    items: items,
    page: 1,
    pageSize: 50,
    totalItems: items.length,
    totalPages: 1,
  );

  @override
  Future<Pagina<Tutorial>> mios({
    EstadoTutorial? estado,
    int page = 1,
    int pageSize = 50,
  }) async {
    pedidosMios.add(estado);
    final e = error;
    if (e != null) throw e;
    return _pagina(
      estado == null ? mias : mias.where((t) => t.estado == estado).toList(),
    );
  }

  @override
  Future<Pagina<Tutorial>> publicados({
    String? busqueda,
    TipoTutorial? tipo,
    int page = 1,
    int pageSize = 50,
  }) async {
    pedidosPublicados.add((busqueda, tipo));
    final e = error;
    if (e != null) throw e;
    return _pagina(publicadosLista);
  }

  @override
  Future<ArchivoSubido> subirPdf(
    Uint8List bytes, {
    required String nombre,
  }) async {
    nombreSubido = nombre;
    final e = errorAccion;
    if (e != null) throw e;
    return _archivo;
  }

  Tutorial _devolver(String accion, String codigo) {
    acciones.add('$accion:$codigo');
    final e = errorAccion;
    if (e != null) throw e;
    return _tutorial(codigo: codigo);
  }

  @override
  Future<Tutorial> crear(TutorialRequest request) async {
    ultimoRequest = request;
    return _devolver('crear', 'nuevo');
  }

  @override
  Future<Tutorial> editar(String codigo, TutorialRequest request) async {
    ultimoRequest = request;
    return _devolver('editar', codigo);
  }

  @override
  Future<Tutorial> publicar(String codigo) async =>
      _devolver('publicar', codigo);

  @override
  Future<Tutorial> archivar(String codigo) async =>
      _devolver('archivar', codigo);
}

ApiException _errorDeNegocio(String mensaje) =>
    ApiException(code: 'ERR_CON_07', message: mensaje);

void main() {
  group('youtube y validaciones', () {
    test('idDeYoutube lee los enlaces de YouTube y nada más', () {
      for (final url in [
        'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
        'https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ',
        'https://m.youtube.com/watch?v=dQw4w9WgXcQ&t=10s',
        _youtube,
        'https://youtu.be/dQw4w9WgXcQ?si=abc',
        'https://www.youtube.com/shorts/dQw4w9WgXcQ',
        'https://www.youtube.com/embed/dQw4w9WgXcQ',
        '  https://youtu.be/dQw4w9WgXcQ  ',
      ]) {
        expect(idDeYoutube(url), 'dQw4w9WgXcQ', reason: url);
      }
      for (final url in [
        'http://www.youtube.com/watch?v=dQw4w9WgXcQ',
        'https://vimeo.com/123456789',
        'https://youtube.com.evil.com/watch?v=dQw4w9WgXcQ',
        'https://www.youtube.com/watch?v=corto',
        'javascript:alert(1)',
        '',
      ]) {
        expect(idDeYoutube(url), isNull, reason: url);
      }
    });

    test(
      'validarTitulo y validarDescripcion siguen las reglas del backend',
      () {
        expect(validarTitulo(''), 'El título es obligatorio');
        expect(validarTitulo('ab'), contains('entre 3 y 150'));
        expect(validarTitulo('a' * 151), contains('entre 3 y 150'));
        expect(validarTitulo(' Cómo estacionar '), isNull);
        expect(validarDescripcion('a' * 1001), contains('1000'));
        expect(validarDescripcion(null), isNull);
      },
    );

    test('las etiquetas se separan por comas y se validan', () {
      expect(separarEtiquetas(' vial, ,señales,vial'), ['vial', 'señales']);
      expect(validarEtiquetas('a,b,c,d,e,f,g,h,i,j,k'), contains('hasta 10'));
      expect(validarEtiquetas('a' * 31), contains('hasta 30'));
      expect(validarEtiquetas('vial, señales'), isNull);
    });

    test('el enlace debe ser de YouTube y el PDF pesar hasta 20 MB', () {
      expect(validarEnlaceYoutube(''), 'Pega el enlace del video');
      expect(validarEnlaceYoutube('https://vimeo.com/1'), contains('YouTube'));
      expect(validarEnlaceYoutube(_youtube), isNull);
      expect(validarTamanoPdf(pdfMaxBytes), isNull);
      expect(validarTamanoPdf(pdfMaxBytes + 1), contains('20 MB'));
      expect(validarAudiencia({}), isNotNull);
      expect(validarAudiencia({AudienciaTutorial.estudiante}), isNull);
    });
  });

  group('TutorialRequest', () {
    test('un PDF nuevo manda el tipo y su descriptor', () {
      final json = TutorialRequest(
        titulo: ' Guía ',
        descripcion: '  ',
        categoria: 'B',
        tipo: TipoTutorial.pdf,
        audiencia: [AudienciaTutorial.estudiante],
        etiquetas: ['vial'],
        archivo: _archivo,
      ).toJson();

      expect(json['titulo'], 'Guía');
      expect(json['descripcion'], isNull);
      expect(json['tipo'], 'PDF');
      expect(json['audiencia'], ['ESTUDIANTE']);
      expect((json['recurso'] as Map)['tamanoBytes'], 4);
    });

    test('un enlace manda solo la dirección y sin recurso no manda tipo', () {
      final conEnlace = TutorialRequest(
        titulo: 'Guía',
        categoria: 'GENERAL',
        tipo: TipoTutorial.enlace,
        audiencia: [AudienciaTutorial.instructor],
        enlace: ' $_youtube ',
      ).toJson();
      expect(conEnlace['recurso'], {'url': _youtube});

      final sinRecurso = TutorialRequest(
        titulo: 'Guía',
        categoria: 'GENERAL',
        tipo: TipoTutorial.pdf,
        audiencia: [AudienciaTutorial.instructor],
      ).toJson();
      expect(sinRecurso.containsKey('recurso'), isFalse);
      expect(sinRecurso.containsKey('tipo'), isFalse);
    });
  });

  group('TutorialesService', () {
    late AdaptadorEnCola adaptador;

    void responder(int status, Map<String, dynamic> cuerpo) {
      adaptador = AdaptadorEnCola([RespuestaFalsa(status, cuerpo)]);
      ApiClient.instance.httpClientAdapter = adaptador;
    }

    setUp(
      () => dotenv.loadFromString(
        envString: 'API_BASE_URL=http://localhost:8080/api/v1',
      ),
    );

    test('mios pide la página con el estado y lee los tutoriales', () async {
      responder(200, _sobreLista([_tutorialJson()]));

      final pagina = await TutorialesService().mios(
        estado: EstadoTutorial.borrador,
      );

      expect(adaptador.peticiones.single.path, '/tutoriales');
      expect(adaptador.peticiones.single.queryParameters, {
        'page': 1,
        'pageSize': 50,
        'filter[estado]': 'BORRADOR',
      });
      final t = pagina.items.single;
      expect(t.tipo, TipoTutorial.enlace);
      expect(t.audiencia, [
        AudienciaTutorial.estudiante,
        AudienciaTutorial.instructor,
      ]);
      expect(t.recursoUrl, _youtube);
    });

    test(
      'publicados manda búsqueda y tipo, y sin ellos no manda filtros',
      () async {
        responder(200, _sobreLista([_tutorialJson(tipo: 'PDF')]));
        final pagina = await TutorialesService().publicados(
          busqueda: ' señales ',
          tipo: TipoTutorial.pdf,
        );

        expect(adaptador.peticiones.single.path, '/tutoriales/publicados');
        expect(adaptador.peticiones.single.queryParameters, {
          'page': 1,
          'pageSize': 50,
          'search': 'señales',
          'filter[tipo]': 'PDF',
        });
        expect(pagina.items.single.tamanoBytes, 2048);
        expect(pagina.items.single.recursoNombre, 'guia.pdf');

        responder(200, _sobreLista([]));
        await TutorialesService().publicados(busqueda: ' ');
        expect(adaptador.peticiones.single.queryParameters, {
          'page': 1,
          'pageSize': 50,
        });
      },
    );

    test(
      'subirPdf envía un multipart a /tutoriales/archivos y lee el descriptor',
      () async {
        responder(201, {
          'status': 'success',
          'data': {
            'url': _archivo.url,
            'mime': 'application/pdf',
            'tamanoBytes': 4,
            'sha256': 'a1b2',
            'nombre': 'guia.pdf',
          },
          'message': 'ok',
          'timestamp': 'x',
        });

        final archivo = await TutorialesService().subirPdf(
          Uint8List.fromList(utf8.encode('%PDF')),
          nombre: 'guia.pdf',
        );

        expect(adaptador.peticiones.single.path, '/tutoriales/archivos');
        expect(adaptador.peticiones.single.method, 'POST');
        expect(adaptador.peticiones.single.data, isA<FormData>());
        expect(archivo.url, _archivo.url);
        expect(archivo.tamanoBytes, 4);
      },
    );

    test(
      'crear, editar, publicar y archivar usan sus rutas y verbos',
      () async {
        final request = TutorialRequest(
          titulo: 'Guía',
          categoria: 'B',
          tipo: TipoTutorial.enlace,
          audiencia: [AudienciaTutorial.estudiante],
          enlace: _youtube,
        );
        final llamadas = <(String, String)>[];
        for (final (accion, ejecutar)
            in <(String, Future<Tutorial> Function(TutorialesService))>[
              ('POST /tutoriales', (s) => s.crear(request)),
              ('PATCH /tutoriales/t1', (s) => s.editar('t1', request)),
              ('POST /tutoriales/t1/publicar', (s) => s.publicar('t1')),
              ('POST /tutoriales/t1/archivar', (s) => s.archivar('t1')),
            ]) {
          responder(200, {
            'status': 'success',
            'data': _tutorialJson(),
            'message': 'ok',
            'timestamp': 'x',
          });
          final t = await ejecutar(TutorialesService());
          final p = adaptador.peticiones.single;
          llamadas.add(('${p.method} ${p.path}', accion));
          expect(t.codigo, 't1');
        }

        for (final (hecha, esperada) in llamadas) {
          expect(hecha, esperada);
        }
      },
    );

    test('un error de negocio llega como ApiException', () async {
      responder(400, {
        'status': 'failed',
        'data': null,
        'message': 'El enlace debe ser de un video de YouTube',
        'timestamp': 'x',
        'error': 'ERR_CON_07',
      });

      await expectLater(
        TutorialesService().publicar('t1'),
        throwsA(
          isA<ApiException>().having((e) => e.code, 'code', 'ERR_CON_07'),
        ),
      );
    });
  });

  group('BibliotecaTutorialesView', () {
    late TutorialesServiceFalso servicio;
    Object? abierto;

    setUp(() {
      servicio = TutorialesServiceFalso();
      abierto = null;
    });

    Future<void> montar(WidgetTester tester) async {
      final container = ProviderContainer(
        overrides: [tutorialesServiceProvider.overrideWithValue(servicio)],
      );
      addTearDown(container.dispose);
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (context, state) =>
                const Scaffold(body: BibliotecaTutorialesView()),
          ),
          GoRoute(
            path: '/pdf',
            builder: (context, state) {
              abierto = state.extra;
              return const Text('visor');
            },
          ),
          GoRoute(
            path: '/video',
            builder: (context, state) {
              abierto = state.extra;
              return const Text('reproductor');
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

    testWidgets('lista los tutoriales con su tipo, audiencia y etiquetas', (
      tester,
    ) async {
      servicio.publicadosLista = [
        _tutorial(),
        _tutorial(codigo: 't2', tipo: 'PDF', titulo: 'Guía en PDF'),
      ];

      await montar(tester);

      expect(find.byKey(const Key('tutorial-t1')), findsOneWidget);
      expect(find.text('Cómo estacionar'), findsOneWidget);
      expect(
        find.text('Video · Para estudiantes y instructores'),
        findsOneWidget,
      );
      expect(find.text('Ver video'), findsOneWidget);
      expect(find.text('Abrir PDF'), findsOneWidget);
      expect(find.text('vial'), findsNWidgets(2));
    });

    testWidgets('sin tutoriales muestra el estado vacío', (tester) async {
      await montar(tester);

      expect(find.text('Todavía no hay tutoriales'), findsOneWidget);
    });

    testWidgets('el tipo y la búsqueda se piden al servicio', (tester) async {
      servicio.publicadosLista = [_tutorial()];
      await montar(tester);

      await tester.tap(find.byKey(const Key('tipo-PDF')));
      await tester.pumpAndSettle();
      await tester.enterText(
        find.byKey(const Key('campoBuscarTutorial')),
        'estacionar',
      );
      await tester.testTextInput.receiveAction(TextInputAction.search);
      await tester.pumpAndSettle();

      expect(servicio.pedidosPublicados, [
        ('', null),
        ('', TipoTutorial.pdf),
        ('estacionar', TipoTutorial.pdf),
      ]);
    });

    testWidgets('con un filtro sin resultados lo dice', (tester) async {
      await montar(tester);
      await tester.tap(find.byKey(const Key('tipo-ENLACE')));
      await tester.pumpAndSettle();

      expect(find.text('No encontramos tutoriales'), findsOneWidget);
    });

    testWidgets('un error al cargar avisa y permite reintentar', (
      tester,
    ) async {
      servicio.error = Exception('sin red');
      await montar(tester);
      expect(
        find.textContaining('No pudimos cargar los tutoriales'),
        findsOneWidget,
      );

      servicio.error = null;
      servicio.publicadosLista = [_tutorial()];
      await tester.tap(find.byKey(const Key('botonReintentarTutoriales')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('tutorial-t1')), findsOneWidget);
    });

    testWidgets('un PDF se abre en el visor de la app', (tester) async {
      servicio.publicadosLista = [
        _tutorial(tipo: 'PDF', titulo: 'Guía en PDF'),
      ];
      await montar(tester);

      await tester.tap(find.byKey(const Key('botonAbrirTutorial-t1')));
      await tester.pumpAndSettle();

      expect(find.text('visor'), findsOneWidget);
      final args = abierto as VisorPdfArgs;
      expect(args.url, 'http://localhost:9000/drivehub-contenido/t.pdf');
      expect(args.titulo, 'Guía en PDF');
    });

    testWidgets('un video se abre en el reproductor de la app', (tester) async {
      servicio.publicadosLista = [_tutorial()];
      await montar(tester);

      await tester.tap(find.byKey(const Key('botonAbrirTutorial-t1')));
      await tester.pumpAndSettle();

      expect(find.text('reproductor'), findsOneWidget);
      final args = abierto as ReproductorVideoArgs;
      expect(args.url, _youtube);
      expect(args.titulo, 'Cómo estacionar');
    });
  });

  group('ReproductorVideoScreen', () {
    testWidgets('un enlace que no es de YouTube muestra un aviso', (
      tester,
    ) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.light,
          home: const ReproductorVideoScreen(
            url: 'https://vimeo.com/1',
            titulo: 'Video',
          ),
        ),
      );

      expect(find.byKey(const Key('errorReproductor')), findsOneWidget);
    });
  });

  group('MisTutorialesView', () {
    late TutorialesServiceFalso servicio;
    Object? editado;

    setUp(() {
      servicio = TutorialesServiceFalso();
      editado = null;
    });

    Future<void> montar(WidgetTester tester) async {
      final container = ProviderContainer(
        overrides: [tutorialesServiceProvider.overrideWithValue(servicio)],
      );
      addTearDown(container.dispose);
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (context, state) =>
                const Scaffold(body: MisTutorialesView()),
          ),
          GoRoute(
            path: '/tutorials/edit',
            builder: (context, state) {
              editado = state.extra;
              return const Text('formulario');
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

    testWidgets('cada estado ofrece sus acciones', (tester) async {
      servicio.mias = [
        _tutorial(codigo: 'b', estado: 'BORRADOR'),
        _tutorial(codigo: 'p'),
        _tutorial(codigo: 'a', estado: 'ARCHIVADO'),
      ];
      await montar(tester);

      // * Borrador: editar y publicar
      expect(find.byKey(const Key('botonEditarTutorial-b')), findsOneWidget);
      expect(find.byKey(const Key('botonPublicarTutorial-b')), findsOneWidget);
      expect(find.byKey(const Key('botonArchivarTutorial-b')), findsNothing);
      // * Publicado: editar y archivar
      expect(find.byKey(const Key('botonEditarTutorial-p')), findsOneWidget);
      expect(find.byKey(const Key('botonArchivarTutorial-p')), findsOneWidget);
      expect(find.byKey(const Key('botonPublicarTutorial-p')), findsNothing);
      // * Archivado: solo publicar de nuevo (no se edita)
      expect(find.byKey(const Key('botonEditarTutorial-a')), findsNothing);
      expect(find.text('Publicar de nuevo'), findsOneWidget);
    });

    testWidgets(
      'sin tutoriales muestra el estado vacío y el filtro pide ese estado',
      (tester) async {
        await montar(tester);
        expect(find.text('Todavía no tienes tutoriales'), findsOneWidget);

        await tester.tap(find.byKey(const Key('estado-BORRADOR')));
        await tester.pumpAndSettle();

        expect(servicio.pedidosMios, [null, EstadoTutorial.borrador]);
        expect(find.text('No hay tutoriales en este estado'), findsOneWidget);
      },
    );

    testWidgets('publicar llama al servicio, avisa y recarga la lista', (
      tester,
    ) async {
      servicio.mias = [_tutorial(codigo: 'b', estado: 'BORRADOR')];
      await montar(tester);

      await tester.tap(find.byKey(const Key('botonPublicarTutorial-b')));
      await tester.pumpAndSettle();

      expect(servicio.acciones, ['publicar:b']);
      expect(find.text('Tutorial publicado'), findsOneWidget);
      expect(servicio.pedidosMios.length, 2);
    });

    testWidgets('archivar pide confirmación y cancelar no hace nada', (
      tester,
    ) async {
      servicio.mias = [_tutorial(codigo: 'p')];
      await montar(tester);

      await tester.tap(find.byKey(const Key('botonArchivarTutorial-p')));
      await tester.pumpAndSettle();
      expect(find.text('¿Archivar este tutorial?'), findsOneWidget);
      await tester.tap(find.byKey(const Key('botonCancelarArchivar')));
      await tester.pumpAndSettle();
      expect(servicio.acciones, isEmpty);

      await tester.tap(find.byKey(const Key('botonArchivarTutorial-p')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonConfirmarArchivar')));
      await tester.pumpAndSettle();
      expect(servicio.acciones, ['archivar:p']);
    });

    testWidgets('si una acción falla avisa con el mensaje del backend', (
      tester,
    ) async {
      servicio.mias = [_tutorial(codigo: 'b', estado: 'BORRADOR')];
      servicio.errorAccion = _errorDeNegocio('El tutorial ya está publicado');
      await montar(tester);

      await tester.tap(find.byKey(const Key('botonPublicarTutorial-b')));
      await tester.pumpAndSettle();

      expect(find.text('El tutorial ya está publicado'), findsOneWidget);
    });

    testWidgets('editar abre el formulario con el tutorial', (tester) async {
      servicio.mias = [_tutorial(codigo: 'p')];
      await montar(tester);

      await tester.tap(find.byKey(const Key('botonEditarTutorial-p')));
      await tester.pumpAndSettle();

      expect(find.text('formulario'), findsOneWidget);
      expect((editado as Tutorial).codigo, 'p');
    });

    testWidgets('un error al cargar permite reintentar', (tester) async {
      servicio.error = Exception('sin red');
      await montar(tester);
      expect(
        find.textContaining('No pudimos cargar tus tutoriales'),
        findsOneWidget,
      );

      servicio.error = null;
      servicio.mias = [_tutorial()];
      await tester.tap(find.byKey(const Key('botonReintentarMisTutoriales')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('tutorial-t1')), findsOneWidget);
    });
  });

  group('TutorialFormScreen', () {
    late TutorialesServiceFalso servicio;
    PdfElegido? pdfElegido;

    setUp(() {
      servicio = TutorialesServiceFalso();
      pdfElegido = PdfElegido(
        bytes: Uint8List.fromList(utf8.encode('%PDF-1.4')),
        nombre: 'guia.pdf',
      );
    });

    Future<void> montar(WidgetTester tester, {Tutorial? tutorial}) async {
      // * Pantalla alta: el formulario es una lista larga y los campos de abajo deben estar construidos
      tester.view.physicalSize = const Size(800, 3200);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final container = ProviderContainer(
        overrides: [
          tutorialesServiceProvider.overrideWithValue(servicio),
          selectorDePdfProvider.overrideWithValue(() async => pdfElegido),
        ],
      );
      addTearDown(container.dispose);
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (context, state) => const Scaffold(body: Text('lista')),
          ),
          GoRoute(
            path: '/form',
            builder: (context, state) => TutorialFormScreen(tutorial: tutorial),
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
      unawaited(router.push('/form'));
      await tester.pumpAndSettle();
    }

    Future<void> escribir(
      WidgetTester tester,
      String llave,
      String texto,
    ) async {
      await tester.enterText(find.byKey(Key(llave)), texto);
      await tester.pump();
    }

    Future<void> guardar(WidgetTester tester) async {
      await tester.ensureVisible(find.byKey(const Key('botonGuardarTutorial')));
      await tester.tap(find.byKey(const Key('botonGuardarTutorial')));
      await tester.pumpAndSettle();
    }

    testWidgets('sin datos no envía y marca cada campo', (tester) async {
      await montar(tester);

      await guardar(tester);

      expect(find.text('El título es obligatorio'), findsOneWidget);
      expect(find.text('Pega el enlace del video'), findsOneWidget);
      expect(find.byKey(const Key('errorAudienciaTutorial')), findsOneWidget);
      expect(servicio.acciones, isEmpty);
    });

    testWidgets('crea un tutorial con enlace de YouTube y vuelve a la lista', (
      tester,
    ) async {
      await montar(tester);

      await escribir(tester, 'campoTituloTutorial', ' Cómo estacionar ');
      await escribir(tester, 'campoEtiquetasTutorial', 'vial, señales');
      await tester.ensureVisible(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await tester.tap(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await tester.tap(find.byKey(const Key('categoriaTutorial-B')));
      await escribir(tester, 'campoEnlaceTutorial', _youtube);
      await guardar(tester);

      expect(servicio.acciones, ['crear:nuevo']);
      final json = servicio.ultimoRequest!.toJson();
      expect(json['titulo'], 'Cómo estacionar');
      expect(json['categoria'], 'B');
      expect(json['tipo'], 'ENLACE');
      expect(json['audiencia'], ['ESTUDIANTE']);
      expect(json['etiquetas'], ['vial', 'señales']);
      expect(json['recurso'], {'url': _youtube});
      expect(find.text('lista'), findsOneWidget);
    });

    testWidgets('un enlace que no es de YouTube se rechaza', (tester) async {
      await montar(tester);
      await escribir(tester, 'campoTituloTutorial', 'Cómo estacionar');
      await tester.tap(find.byKey(const Key('audiencia-INSTRUCTOR')));
      await escribir(tester, 'campoEnlaceTutorial', 'https://vimeo.com/1');
      await guardar(tester);

      expect(
        find.text('El enlace debe ser de un video de YouTube'),
        findsOneWidget,
      );
      expect(servicio.acciones, isEmpty);
    });

    testWidgets('con PDF lo sube al elegirlo y lo manda al guardar', (
      tester,
    ) async {
      await montar(tester);
      await escribir(tester, 'campoTituloTutorial', 'Guía en PDF');
      await tester.ensureVisible(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await tester.tap(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await tester.tap(find.text('PDF').last);
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonElegirPdfTutorial')));
      await tester.pumpAndSettle();

      expect(servicio.nombreSubido, 'guia.pdf');
      expect(find.byKey(const Key('nombrePdfTutorial')), findsOneWidget);
      await guardar(tester);

      final json = servicio.ultimoRequest!.toJson();
      expect(json['tipo'], 'PDF');
      expect((json['recurso'] as Map)['sha256'], 'a1b2');
      expect(servicio.acciones, ['crear:nuevo']);
    });

    testWidgets('con PDF y sin elegir uno no envía', (tester) async {
      await montar(tester);
      await escribir(tester, 'campoTituloTutorial', 'Guía en PDF');
      await tester.ensureVisible(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await tester.tap(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await tester.tap(find.text('PDF').last);
      await tester.pumpAndSettle();
      await guardar(tester);

      expect(find.byKey(const Key('errorPdfTutorial')), findsOneWidget);
      expect(find.text('Elige el PDF del tutorial'), findsOneWidget);
      expect(servicio.acciones, isEmpty);
    });

    testWidgets('un PDF de más de 20 MB se rechaza sin subirlo', (
      tester,
    ) async {
      pdfElegido = PdfElegido(
        bytes: Uint8List(pdfMaxBytes + 1),
        nombre: 'grande.pdf',
      );
      await montar(tester);
      await tester.tap(find.text('PDF').last);
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('botonElegirPdfTutorial')));
      await tester.pumpAndSettle();

      expect(find.text('El PDF no puede pesar más de 20 MB'), findsOneWidget);
      expect(servicio.nombreSubido, isNull);
    });

    testWidgets('si el backend rechaza el tutorial muestra su mensaje', (
      tester,
    ) async {
      servicio.errorAccion = _errorDeNegocio(
        'El enlace debe ser de un video de YouTube',
      );
      await montar(tester);
      await escribir(tester, 'campoTituloTutorial', 'Cómo estacionar');
      await tester.ensureVisible(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await tester.tap(find.byKey(const Key('audiencia-ESTUDIANTE')));
      await escribir(tester, 'campoEnlaceTutorial', _youtube);
      await guardar(tester);

      expect(
        find.text('El enlace debe ser de un video de YouTube'),
        findsOneWidget,
      );
      expect(find.text('lista'), findsNothing);
    });

    testWidgets(
      'al editar muestra los datos y sin cambiar el recurso no lo manda',
      (tester) async {
        await montar(tester, tutorial: _tutorial(codigo: 't9'));

        expect(find.text('Editar tutorial'), findsOneWidget);
        expect(
          find.widgetWithText(TextFormField, 'Cómo estacionar'),
          findsOneWidget,
        );
        await escribir(tester, 'campoTituloTutorial', 'Título nuevo');
        await guardar(tester);

        expect(servicio.acciones, ['editar:t9']);
        final json = servicio.ultimoRequest!.toJson();
        expect(json['titulo'], 'Título nuevo');
        expect(json.containsKey('recurso'), isFalse);
        expect(json['audiencia'], ['ESTUDIANTE', 'INSTRUCTOR']);
      },
    );

    testWidgets('al editar un PDF sin elegir otro conserva el que tiene', (
      tester,
    ) async {
      await montar(
        tester,
        tutorial: _tutorial(codigo: 't9', tipo: 'PDF'),
      );

      expect(find.byKey(const Key('nombrePdfTutorial')), findsOneWidget);
      expect(find.text('guia.pdf'), findsOneWidget);
      await guardar(tester);

      expect(servicio.acciones, ['editar:t9']);
      expect(servicio.ultimoRequest!.toJson().containsKey('recurso'), isFalse);
    });

    testWidgets('al editar un enlace y cambiarlo manda el recurso nuevo', (
      tester,
    ) async {
      await montar(tester, tutorial: _tutorial(codigo: 't9'));
      await escribir(
        tester,
        'campoEnlaceTutorial',
        'https://youtu.be/AAAAAAAAAAA',
      );
      await guardar(tester);

      expect(servicio.ultimoRequest!.toJson()['recurso'], {
        'url': 'https://youtu.be/AAAAAAAAAAA',
      });
    });
  });
}
