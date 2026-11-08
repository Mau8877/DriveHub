import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/routing/app_router.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/servidor/providers/servidor_provider.dart';
import 'package:mobile/features/servidor/services/probar_servidor.dart';
import 'package:mobile/features/servidor/services/servidor_storage.dart';

import '../../helpers/horarios_falsos.dart';
import '../../helpers/sesion_falsa.dart';
import 'servidor_provider_test.dart' show StorageDeServidorEnMemoria;

void main() {
  late StorageDeServidorEnMemoria storage;
  late List<String> pruebas;
  late ResultadoPrueba resultadoDeLaPrueba;

  /// Monta la app con el router real. `configurado: false` es la primera vez que se abre.
  Future<ProviderContainer> montar(
    WidgetTester tester, {
    required bool configurado,
    EstadoSesion sesion = const EstadoSesion.sinSesion(),
  }) async {
    final container = ProviderContainer(
      overrides: [
        sesionFalsa(SesionFalsa(sesion)),
        horariosServiceFalso(HorariosServiceFalso()),
        servidorStorageProvider.overrideWithValue(storage),
        servidorInicialProvider.overrideWithValue(EstadoServidor(configurado: configurado)),
        probadorServidorProvider.overrideWithValue((url) async {
          pruebas.add(url);
          return resultadoDeLaPrueba;
        }),
      ],
    );
    addTearDown(container.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: container.read(routerProvider)),
      ),
    );
    // * En /cargando el indicador anima para siempre: no se puede esperar a que se asiente
    sesion.fase == FaseSesion.cargando ? await tester.pump() : await tester.pumpAndSettle();
    return container;
  }

  Future<void> escribir(WidgetTester tester, String texto) async {
    await tester.enterText(find.byKey(const Key('campoServidor')), texto);
    await tester.pump();
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://10.0.2.2:8080/api/v1');
    ApiClient.configurarBaseUrl(null);
    storage = StorageDeServidorEnMemoria();
    pruebas = [];
    resultadoDeLaPrueba = ResultadoPrueba.servidorDriveHub;
  });

  tearDown(() => ApiClient.configurarBaseUrl(null));

  group('la primera vez que se abre la app', () {
    testWidgets('lleva a la pantalla de servidor, antes que al login', (tester) async {
      await montar(tester, configurado: false);

      expect(find.text('Conecta tu app'), findsOneWidget);
      expect(find.text('Iniciar sesión'), findsNothing);
    });

    testWidgets('lleva al servidor aunque la sesión todavía se esté recuperando', (tester) async {
      await montar(tester, configurado: false, sesion: const EstadoSesion.cargando());

      expect(find.text('Conecta tu app'), findsOneWidget);
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });

    testWidgets('no se puede salir de ahí a ninguna otra pantalla', (tester) async {
      final container = await montar(tester, configurado: false);

      for (final ruta in ['/login', '/register', '/forgot-password', '/inicio']) {
        container.read(routerProvider).go(ruta);
        await tester.pumpAndSettle();
        expect(find.text('Conecta tu app'), findsOneWidget, reason: '$ruta debería volver al servidor');
      }
    });

    testWidgets('sugiere la dirección del .env, sin el /api/v1 que la app agrega sola', (tester) async {
      await montar(tester, configurado: false);

      expect(tester.widget<TextFormField>(find.byKey(const Key('campoServidor'))).controller!.text, 'http://10.0.2.2:8080');
    });

    testWidgets('no ofrece «Cancelar»: no hay a dónde volver', (tester) async {
      await montar(tester, configurado: false);

      expect(find.byKey(const Key('botonCancelarServidor')), findsNothing);
    });

    testWidgets('guardar una dirección válida la guarda, la pone en uso y sigue al login', (tester) async {
      final container = await montar(tester, configurado: false);

      await escribir(tester, '192.168.1.10:8080');
      await tester.tap(find.byKey(const Key('botonGuardarServidor')));
      await tester.pumpAndSettle();

      expect(storage.valor, 'http://192.168.1.10:8080/api/v1');
      expect(ApiClient.baseUrlActual, 'http://192.168.1.10:8080/api/v1');
      expect(container.read(servidorProvider).configurado, true);
      expect(find.text('Iniciar sesión'), findsOneWidget);
      expect(find.text('Conecta tu app'), findsNothing);
    });

    testWidgets('ya con servidor, las demás pantallas dejan de mandar de vuelta al servidor', (tester) async {
      final container = await montar(tester, configurado: false);
      await escribir(tester, 'http://192.168.1.10:8080');
      await tester.tap(find.byKey(const Key('botonGuardarServidor')));
      await tester.pumpAndSettle();

      container.read(routerProvider).go('/register');
      await tester.pumpAndSettle();

      expect(find.text('Registrarme'), findsOneWidget);
      expect(find.text('Conecta tu app'), findsNothing);
    });

    testWidgets('con la dirección vacía no guarda y lo dice', (tester) async {
      await montar(tester, configurado: false);

      await escribir(tester, '');
      await tester.tap(find.byKey(const Key('botonGuardarServidor')));
      await tester.pump();

      expect(find.text('Escribe la dirección del servidor'), findsOneWidget);
      expect(storage.guardados, 0);
      expect(find.text('Conecta tu app'), findsOneWidget);
    });

    testWidgets('con una dirección mal escrita no guarda y explica cómo escribirla', (tester) async {
      await montar(tester, configurado: false);

      await escribir(tester, 'ftp://servidor');
      await tester.tap(find.byKey(const Key('botonGuardarServidor')));
      await tester.pump();

      expect(find.text('La dirección tiene que empezar con http:// o https://'), findsOneWidget);
      expect(storage.guardados, 0);
    });

    testWidgets('se puede guardar sin conexión: la prueba no es obligatoria', (tester) async {
      resultadoDeLaPrueba = ResultadoPrueba.sinRespuesta;
      await montar(tester, configurado: false);

      await escribir(tester, 'http://192.168.1.10:8080');
      await tester.tap(find.byKey(const Key('botonGuardarServidor')));
      await tester.pumpAndSettle();

      expect(pruebas, isEmpty);
      expect(storage.valor, 'http://192.168.1.10:8080/api/v1');
      expect(find.text('Iniciar sesión'), findsOneWidget);
    });
  });

  group('probar la conexión', () {
    testWidgets('prueba la dirección normalizada y avisa que encontró un servidor de DriveHub', (tester) async {
      await montar(tester, configurado: false);

      await escribir(tester, '192.168.1.10:8080');
      await tester.tap(find.byKey(const Key('botonProbarServidor')));
      await tester.pumpAndSettle();

      expect(pruebas, ['http://192.168.1.10:8080/api/v1']);
      expect(find.text('Servidor de DriveHub encontrado.'), findsOneWidget);
    });

    testWidgets('si responde otra cosa, advierte que no parece DriveHub', (tester) async {
      resultadoDeLaPrueba = ResultadoPrueba.respondeOtraCosa;
      await montar(tester, configurado: false);

      await tester.tap(find.byKey(const Key('botonProbarServidor')));
      await tester.pumpAndSettle();

      expect(find.textContaining('no parece un servidor de DriveHub'), findsOneWidget);
    });

    testWidgets('si no responde nada, lo dice y recuerda que se puede guardar igual', (tester) async {
      resultadoDeLaPrueba = ResultadoPrueba.sinRespuesta;
      await montar(tester, configurado: false);

      await tester.tap(find.byKey(const Key('botonProbarServidor')));
      await tester.pumpAndSettle();

      expect(find.textContaining('No hay respuesta'), findsOneWidget);
      expect(find.textContaining('Puedes guardarla igual'), findsOneWidget);
    });

    testWidgets('probar no guarda nada ni cambia de pantalla', (tester) async {
      await montar(tester, configurado: false);

      await escribir(tester, 'http://192.168.1.10:8080');
      await tester.tap(find.byKey(const Key('botonProbarServidor')));
      await tester.pumpAndSettle();

      expect(storage.guardados, 0);
      expect(ApiClient.baseUrlActual, 'http://10.0.2.2:8080/api/v1');
      expect(find.text('Conecta tu app'), findsOneWidget);
    });

    testWidgets('no prueba una dirección inválida', (tester) async {
      await montar(tester, configurado: false);

      await escribir(tester, 'ftp://x');
      await tester.tap(find.byKey(const Key('botonProbarServidor')));
      await tester.pump();

      expect(pruebas, isEmpty);
      expect(find.byKey(const Key('resultadoServidor')), findsNothing);
    });

    testWidgets('al editar la dirección se borra el resultado de la prueba anterior', (tester) async {
      await montar(tester, configurado: false);
      await tester.tap(find.byKey(const Key('botonProbarServidor')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('resultadoServidor')), findsOneWidget);

      await escribir(tester, 'http://otra:8080');

      expect(find.byKey(const Key('resultadoServidor')), findsNothing);
    });
  });

  group('las veces siguientes', () {
    testWidgets('con servidor ya elegido la app arranca directo en el login', (tester) async {
      await montar(tester, configurado: true);

      expect(find.text('Iniciar sesión'), findsOneWidget);
      expect(find.text('Conecta tu app'), findsNothing);
    });

    testWidgets('el login ofrece «Cambiar servidor»', (tester) async {
      await montar(tester, configurado: true);

      expect(find.byKey(const Key('botonCambiarServidor')), findsOneWidget);
    });

    testWidgets('«Cambiar servidor» abre la pantalla con lo que hay, y se puede cancelar', (tester) async {
      await montar(tester, configurado: true);

      await tester.ensureVisible(find.byKey(const Key('botonCambiarServidor')));
      await tester.tap(find.byKey(const Key('botonCambiarServidor')));
      await tester.pumpAndSettle();

      expect(find.text('Servidor'), findsOneWidget);
      expect(find.text('Conecta tu app'), findsNothing);

      await tester.tap(find.byKey(const Key('botonCancelarServidor')));
      await tester.pumpAndSettle();
      expect(find.text('Iniciar sesión'), findsOneWidget);
      expect(storage.guardados, 0);
    });

    testWidgets('cambiar el servidor lo reemplaza y vuelve al login', (tester) async {
      final container = await montar(tester, configurado: true);

      container.read(routerProvider).go('/servidor');
      await tester.pumpAndSettle();
      await escribir(tester, 'http://nuevo:9000');
      await tester.tap(find.byKey(const Key('botonGuardarServidor')));
      await tester.pumpAndSettle();

      expect(storage.valor, 'http://nuevo:9000/api/v1');
      expect(ApiClient.baseUrlActual, 'http://nuevo:9000/api/v1');
      expect(find.text('Iniciar sesión'), findsOneWidget);
    });

    testWidgets('con sesión iniciada /servidor no es accesible: lleva al inicio', (tester) async {
      final container = await montar(tester, configurado: true, sesion: const EstadoSesion.conSesion(usuarioDePrueba));
      expect(find.text('Conecta tu app'), findsNothing);

      container.read(routerProvider).go('/servidor');
      await tester.pumpAndSettle();

      expect(find.text('Conecta tu app'), findsNothing);
      expect(find.text('Servidor'), findsNothing);
    });
  });
}
