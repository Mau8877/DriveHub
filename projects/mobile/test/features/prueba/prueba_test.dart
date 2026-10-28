import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/prueba/models/prueba.dart';
import 'package:mobile/features/prueba/providers/prueba_providers.dart';
import 'package:mobile/features/prueba/screens/prueba_screen.dart';
import 'package:mobile/features/prueba/services/detector_rostro.dart';
import 'package:mobile/features/prueba/services/maquina_prueba.dart';
import 'package:mobile/features/prueba/services/sesiones_service.dart';

import '../../helpers/adaptador_en_cola.dart';

const _frente = Medicion(giro: 0, cabeceo: 0, ojosAbiertos: 0.9);
const _izquierda = Medicion(giro: 25, cabeceo: 0, ojosAbiertos: 0.9);
const _derecha = Medicion(giro: -25, cabeceo: 0, ojosAbiertos: 0.9);
const _arriba = Medicion(giro: 0, cabeceo: 15, ojosAbiertos: 0.9);

/// La postura que pide cada uno de los cinco pasos, en orden.
const _posturas = [_frente, _izquierda, _frente, _derecha, _arriba];

class DetectorFalso implements DetectorRostro {
  final _salida = StreamController<Medicion?>.broadcast();
  Object? errorAlIniciar;
  int iniciado = 0;
  int detenido = 0;

  @override
  String get modelo => 'detector-falso';

  @override
  Stream<Medicion?> get mediciones => _salida.stream;

  void emitir(Medicion? medicion) => _salida.add(medicion);

  @override
  Future<void> iniciar() async {
    final e = errorAlIniciar;
    if (e != null) throw e;
    iniciado++;
  }

  @override
  Future<void> detener() async => detenido++;

  @override
  Widget vistaPrevia(BuildContext context) =>
      const SizedBox(key: Key('vistaPrevia'));
}

class SesionesFalso extends SesionesService {
  SesionesFalso() : super(dio: Dio());

  Object? error;
  final List<ResultadoPrueba> guardadas = [];

  @override
  Future<String> guardarPrueba(
    ResultadoPrueba resultado, {
    String? categoria,
    String? escenario,
  }) async {
    final e = error;
    if (e != null) throw e;
    guardadas.add(resultado);
    return 'sesion-1';
  }
}

/// Recorre la máquina sosteniendo cada postura el tiempo necesario. Devuelve la hora al terminar.
int _recorrer(
  MaquinaPrueba maquina,
  List<Medicion?> posturas, {
  int desde = 0,
}) {
  var ahora = desde;
  for (final postura in posturas) {
    maquina.alimentar(postura, ahora);
    ahora += maquina.configuracion.sostenerMs;
    maquina.alimentar(postura, ahora);
    ahora += 100;
  }
  return ahora;
}

void main() {
  group('MaquinaPrueba', () {
    test('antes de iniciar no hay paso actual y alimentar no hace nada', () {
      final maquina = MaquinaPrueba();

      maquina.alimentar(_frente, 100);

      expect(maquina.iniciada, isFalse);
      expect(maquina.pasoActual, isNull);
      expect(maquina.resultados, isEmpty);
    });

    test('una prueba perfecta aprueba los cinco pasos y da 100', () {
      final maquina = MaquinaPrueba()..iniciar(0);

      final fin = _recorrer(maquina, _posturas);

      expect(maquina.terminada, isTrue);
      expect(maquina.pasoActual, isNull);
      final resultado = maquina.resultado(fin, modelo: 'm');
      expect(resultado.aprobados, 5);
      expect(resultado.puntaje, 100);
      expect(resultado.pasos.map((p) => p.orden), [1, 2, 3, 4, 5]);
      expect(resultado.pasos.every((p) => p.aprobado), isTrue);
      expect(resultado.retroalimentacion, startsWith('¡Muy bien!'));
      expect(resultado.duracionSeg, greaterThanOrEqualTo(3));
      expect(resultado.modelo, 'm');
    });

    test(
      'hay que sostener la postura: si se corta, el conteo empieza de nuevo',
      () {
        final maquina = MaquinaPrueba()..iniciar(0);

        maquina.alimentar(_frente, 0);
        maquina.alimentar(_frente, 400);
        expect(maquina.progresoPaso(400), closeTo(400 / 600, 0.001));
        maquina.alimentar(_izquierda, 500);
        expect(maquina.progresoPaso(500), 0);
        maquina.alimentar(_frente, 600);
        maquina.alimentar(_frente, 1100);

        expect(maquina.resultados, isEmpty);
        maquina.alimentar(_frente, 1200);
        expect(maquina.resultados.single.aprobado, isTrue);
        expect(maquina.indice, 1);
      },
    );

    test(
      'sin rostro no avanza y al pasar el tiempo el paso queda no logrado',
      () {
        final maquina = MaquinaPrueba()..iniciar(0);

        maquina.alimentar(null, 1000);
        expect(maquina.restanteMs(1000), 7000);
        maquina.alimentar(null, 8000);

        final primero = maquina.resultados.single;
        expect(primero.aprobado, isFalse);
        expect(primero.tiempoMs, 8000);
        expect(maquina.indice, 1);
        expect(maquina.restanteMs(8000), 8000);
      },
    );

    test(
      'con pasos no logrados el puntaje baja y la retroalimentación los nombra',
      () {
        final maquina = MaquinaPrueba()..iniciar(0);

        var t = _recorrer(maquina, [_frente, _izquierda, _frente, _derecha]);
        maquina.alimentar(_frente, t + 9000);

        expect(maquina.terminada, isTrue);
        final resultado = maquina.resultado(t + 9000, modelo: 'm');
        expect(resultado.puntaje, 80);
        expect(resultado.pasos.last.aprobado, isFalse);
        expect(resultado.retroalimentacion, contains('Lograste 4 de 5'));
        expect(
          resultado.retroalimentacion,
          contains('mira el espejo retrovisor'),
        );
      },
    );

    test('si no se logra nada el mensaje lo dice', () {
      final maquina = MaquinaPrueba()..iniciar(0);

      for (var i = 1; i <= 5; i++) {
        maquina.alimentar(null, i * 8000);
      }

      final resultado = maquina.resultado(40000, modelo: 'm');
      expect(resultado.puntaje, 0);
      expect(
        resultado.retroalimentacion,
        contains('No logramos validar ningún paso'),
      );
    });

    test(
      '«al frente» pide los ojos abiertos y tolera una medición sin esa probabilidad',
      () {
        final cerrados = MaquinaPrueba()..iniciar(0);
        const ojosCerrados = Medicion(giro: 0, cabeceo: 0, ojosAbiertos: 0.1);
        cerrados.alimentar(ojosCerrados, 0);
        cerrados.alimentar(ojosCerrados, 2000);
        expect(cerrados.resultados, isEmpty);

        final sinDato = MaquinaPrueba()..iniciar(0);
        const sinProbabilidad = Medicion(giro: 3, cabeceo: -4);
        sinDato.alimentar(sinProbabilidad, 0);
        sinDato.alimentar(sinProbabilidad, 600);
        expect(sinDato.resultados.single.aprobado, isTrue);
      },
    );

    test(
      'mirar al lado equivocado no cuenta; invertirLados cambia el sentido',
      () {
        final normal = MaquinaPrueba(
          pasos: const [PasoPrueba(TipoPaso.espejoIzquierdo, 'Izq')],
        )..iniciar(0);
        normal.alimentar(_derecha, 0);
        normal.alimentar(_derecha, 1000);
        expect(normal.resultados, isEmpty);

        final invertida = MaquinaPrueba(
          pasos: const [PasoPrueba(TipoPaso.espejoIzquierdo, 'Izq')],
          configuracion: const ConfiguracionPrueba(invertirLados: true),
        )..iniciar(0);
        invertida.alimentar(_derecha, 0);
        invertida.alimentar(_derecha, 600);
        expect(invertida.resultados.single.aprobado, isTrue);
      },
    );

    test('un giro apenas menor al ángulo pedido no cuenta', () {
      final maquina = MaquinaPrueba(
        pasos: const [PasoPrueba(TipoPaso.espejoDerecho, 'Der')],
      )..iniciar(0);
      const casi = Medicion(giro: -17, cabeceo: 0);

      maquina.alimentar(casi, 0);
      maquina.alimentar(casi, 3000);

      expect(maquina.resultados, isEmpty);
    });
  });

  group('SesionesService', () {
    setUp(
      () => dotenv.loadFromString(
        envString: 'API_BASE_URL=http://localhost:8080/api/v1',
      ),
    );

    test(
      'guardarPrueba manda el resultado con sus pasos y lee el código de la sesión',
      () async {
        final adaptador = AdaptadorEnCola([
          RespuestaFalsa(201, {
            'status': 'success',
            'data': {'codigo': 's1'},
            'message': 'ok',
            'timestamp': 'x',
          }),
        ]);
        ApiClient.instance.httpClientAdapter = adaptador;
        final maquina = MaquinaPrueba()..iniciar(0);
        final fin = _recorrer(maquina, _posturas);

        final codigo = await SesionesService().guardarPrueba(
          maquina.resultado(fin, modelo: 'mlkit-rostro-v1'),
          categoria: 'B',
        );

        expect(codigo, 's1');
        final pedido = adaptador.peticiones.single;
        expect(pedido.method, 'POST');
        expect(pedido.path, '/sesiones-ia');
        final cuerpo = pedido.data as Map<String, dynamic>;
        expect(cuerpo['puntaje'], 100.0);
        expect(cuerpo['modelo'], 'mlkit-rostro-v1');
        expect(cuerpo['categoria'], 'B');
        expect(cuerpo.containsKey('escenario'), isFalse);
        final pasos = cuerpo['pasos'] as List<dynamic>;
        expect(pasos.length, 5);
        expect(pasos.first, {
          'orden': 1,
          'instruccion': 'Mira al frente',
          'aprobado': true,
          'tiempoMs': 600,
        });
      },
    );

    test('un error de negocio llega como ApiException', () async {
      ApiClient.instance.httpClientAdapter = AdaptadorEnCola([
        RespuestaFalsa(400, {
          'status': 'failed',
          'data': null,
          'message': 'Los pasos de la prueba deben numerarse',
          'timestamp': 'x',
          'error': 'ERR_SYS_01',
        }),
      ]);
      final maquina = MaquinaPrueba()..iniciar(0);
      final fin = _recorrer(maquina, _posturas);

      await expectLater(
        SesionesService().guardarPrueba(maquina.resultado(fin, modelo: 'm')),
        throwsA(
          isA<ApiException>().having((e) => e.code, 'code', 'ERR_SYS_01'),
        ),
      );
    });
  });

  group('PruebaScreen', () {
    late DetectorFalso detector;
    late SesionesFalso sesiones;
    late int ahora;
    ProviderContainer? contenedor;

    /// Como `testWidgets`, pero al terminar desmonta la pantalla y libera el estado: la prueba deja un
    /// temporizador periódico corriendo y el test no puede acabar con uno pendiente.
    void pruebaWidget(
      String nombre,
      Future<void> Function(WidgetTester tester) cuerpo,
    ) {
      testWidgets(nombre, (tester) async {
        try {
          await cuerpo(tester);
        } finally {
          await tester.pumpWidget(const SizedBox());
          contenedor?.dispose();
          await tester.pump();
        }
      });
    }

    setUp(() {
      detector = DetectorFalso();
      sesiones = SesionesFalso();
      ahora = 0;
    });

    Future<ProviderContainer> montar(WidgetTester tester) async {
      tester.view.physicalSize = const Size(800, 2400);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final container = ProviderContainer(
        overrides: [
          detectorRostroProvider.overrideWithValue(detector),
          sesionesServiceProvider.overrideWithValue(sesiones),
          relojPruebaProvider.overrideWithValue(() => ahora),
        ],
      );
      contenedor = container;
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: MaterialApp(
            theme: AppTheme.light,
            home: const Scaffold(body: PruebaScreen()),
          ),
        ),
      );
      await tester.pumpAndSettle();
      return container;
    }

    /// Un paso de la prueba: se sostiene la [postura] el tiempo necesario con dos evaluaciones.
    Future<void> hacerPaso(WidgetTester tester, Medicion? postura) async {
      detector.emitir(postura);
      await tester.pump();
      await tester.pump(intervaloEvaluacion);
      ahora += 700;
      detector.emitir(postura);
      await tester.pump();
      await tester.pump(intervaloEvaluacion);
    }

    /// Deja correr lo que queda pendiente al terminar la prueba (cerrar la cámara, guardar, mostrar el resultado):
    /// son varias esperas encadenadas y `pumpAndSettle` solo no alcanza si no hay ningún cuadro pendiente.
    Future<void> asentar(WidgetTester tester) async {
      for (var i = 0; i < 4; i++) {
        await tester.pump();
      }
      await tester.pumpAndSettle();
    }

    Future<void> empezar(WidgetTester tester) async {
      await tester.tap(find.byKey(const Key('botonEmpezarPrueba')));
      await tester.pump();
      await tester.pump();
    }

    pruebaWidget(
      'al inicio muestra los pasos, el aviso de privacidad y el botón',
      (tester) async {
        await montar(tester);

        expect(find.text('Mira el espejo izquierdo'), findsOneWidget);
        expect(find.byKey(const Key('avisoPrivacidad')), findsOneWidget);
        expect(
          find.textContaining('No se graba ni se envía video'),
          findsOneWidget,
        );
        expect(find.byKey(const Key('botonEmpezarPrueba')), findsOneWidget);
        expect(detector.iniciado, 0);
      },
    );

    pruebaWidget('empezar abre la cámara y muestra el primer paso', (
      tester,
    ) async {
      await montar(tester);

      await empezar(tester);

      expect(detector.iniciado, 1);
      expect(find.byKey(const Key('vistaPrevia')), findsOneWidget);
      expect(find.text('Paso 1 de 5'), findsOneWidget);
      expect(find.byKey(const Key('instruccionPaso')), findsOneWidget);
      expect(find.text('Mira al frente'), findsWidgets);
    });

    pruebaWidget(
      'sostener cada postura completa la prueba y guarda el resultado',
      (tester) async {
        await montar(tester);
        await empezar(tester);

        for (final postura in _posturas) {
          await hacerPaso(tester, postura);
        }
        await asentar(tester);

        expect(find.byKey(const Key('resultadoPrueba')), findsOneWidget);
        expect(find.text('100%'), findsOneWidget);
        expect(find.text('5 de 5 pasos'), findsOneWidget);
        expect(find.byKey(const Key('resultadoPaso-5')), findsOneWidget);
        expect(find.byKey(const Key('guardado')), findsOneWidget);
        expect(sesiones.guardadas.single.puntaje, 100);
        expect(sesiones.guardadas.single.modelo, 'detector-falso');
        expect(detector.detenido, greaterThan(0));
      },
    );

    pruebaWidget('el avance del paso aparece mientras se sostiene la postura', (
      tester,
    ) async {
      await montar(tester);
      await empezar(tester);

      detector.emitir(_frente);
      await tester.pump();
      await tester.pump(intervaloEvaluacion);
      ahora += 300;
      detector.emitir(_frente);
      await tester.pump();
      await tester.pump(intervaloEvaluacion);

      final progreso = tester.widget<LinearProgressIndicator>(
        find.byKey(const Key('progresoPaso')),
      );
      expect(progreso.value, closeTo(0.5, 0.01));
    });

    pruebaWidget('si no se ve el rostro avisa que se centre', (tester) async {
      await montar(tester);
      await empezar(tester);

      ahora += 1000;
      await tester.pump(intervaloEvaluacion);

      expect(find.byKey(const Key('sinRostro')), findsOneWidget);
    });

    pruebaWidget('un paso sin lograr se marca no logrado y la prueba sigue', (
      tester,
    ) async {
      await montar(tester);
      await empezar(tester);

      ahora += 8100;
      await tester.pump(intervaloEvaluacion);
      expect(find.text('Paso 2 de 5'), findsOneWidget);

      for (final postura in _posturas.skip(1)) {
        await hacerPaso(tester, postura);
      }
      await asentar(tester);

      expect(find.text('80%'), findsOneWidget);
      expect(find.text('4 de 5 pasos'), findsOneWidget);
      expect(find.byIcon(Icons.cancel_outlined), findsOneWidget);
      expect(find.textContaining('Te faltó: mira al frente'), findsOneWidget);
    });

    pruebaWidget(
      'sin conexión el resultado se muestra y se puede reintentar el guardado',
      (tester) async {
        sesiones.error = DioException(
          requestOptions: RequestOptions(path: '/x'),
          type: DioExceptionType.connectionError,
        );
        await montar(tester);
        await empezar(tester);

        for (final postura in _posturas) {
          await hacerPaso(tester, postura);
        }
        await asentar(tester);

        expect(find.byKey(const Key('resultadoPrueba')), findsOneWidget);
        expect(find.byKey(const Key('noGuardado')), findsOneWidget);

        sesiones.error = null;
        await tester.tap(find.byKey(const Key('botonReintentarGuardado')));
        await tester.pumpAndSettle();

        expect(find.byKey(const Key('guardado')), findsOneWidget);
        expect(sesiones.guardadas.length, 1);
      },
    );

    pruebaWidget('sin permiso de cámara avisa y se queda en el inicio', (
      tester,
    ) async {
      detector.errorAlIniciar = const PermisoCamaraDenegado();
      await montar(tester);

      await empezar(tester);
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('errorPrueba')), findsOneWidget);
      expect(
        find.textContaining('Necesitamos permiso para usar la cámara'),
        findsOneWidget,
      );
      expect(find.byKey(const Key('botonEmpezarPrueba')), findsOneWidget);
    });

    pruebaWidget('sin cámara frontal avisa', (tester) async {
      detector.errorAlIniciar = const CamaraNoDisponible();
      await montar(tester);

      await empezar(tester);
      await tester.pumpAndSettle();

      expect(
        find.textContaining('No pudimos abrir la cámara frontal'),
        findsOneWidget,
      );
    });

    pruebaWidget('cancelar cierra la cámara y vuelve al inicio', (
      tester,
    ) async {
      await montar(tester);
      await empezar(tester);

      await tester.tap(find.byKey(const Key('botonCancelarPrueba')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('botonEmpezarPrueba')), findsOneWidget);
      expect(detector.detenido, greaterThan(0));
      expect(sesiones.guardadas, isEmpty);
    });

    pruebaWidget('repetir la prueba vuelve al inicio', (tester) async {
      await montar(tester);
      await empezar(tester);
      for (final postura in _posturas) {
        await hacerPaso(tester, postura);
      }
      await asentar(tester);

      await tester.tap(find.byKey(const Key('botonRepetirPrueba')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('botonEmpezarPrueba')), findsOneWidget);
    });
  });
}
