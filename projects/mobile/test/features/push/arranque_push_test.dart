import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/notificaciones/providers/bandeja_providers.dart';
import 'package:mobile/features/push/providers/arranque_push.dart';
import 'package:mobile/features/push/providers/push_provider.dart';
import 'package:mobile/features/push/services/mensajeria_push.dart';

import '../../helpers/push_falsos.dart';
import '../../helpers/sesion_falsa.dart';

void main() {
  late MensajeriaFalsa mensajeria;
  late DispositivosFalso dispositivos;
  late DispositivoStorageEnMemoria storage;
  late SesionFalsa sesion;
  late int lecturasDelContador;
  late GlobalKey<ScaffoldMessengerState> mensajero;

  ProviderContainer crear() {
    final container = ProviderContainer(
      overrides: [
        sesionFalsa(sesion),
        mensajeriaPushProvider.overrideWithValue(mensajeria),
        dispositivosServiceProvider.overrideWithValue(dispositivos),
        dispositivoPushStorageProvider.overrideWithValue(storage),
        conteoNoLeidasProvider.overrideWith((ref) async {
          lecturasDelContador++;
          return 0;
        }),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    mensajeria = MensajeriaFalsa();
    dispositivos = DispositivosFalso();
    storage = DispositivoStorageEnMemoria();
    sesion = SesionFalsa(const EstadoSesion.sinSesion());
    lecturasDelContador = 0;
    mensajero = GlobalKey<ScaffoldMessengerState>();
  });

  group('con la sesión', () {
    test('sin sesión no registra nada', () async {
      conectarPush(crear(), mensajero);
      await pumpEventQueue();

      expect(dispositivos.registrados, isEmpty);
      expect(mensajeria.permisosPedidos, 0);
    });

    test('al iniciar sesión registra el teléfono para esa persona', () async {
      final container = crear();
      conectarPush(container, mensajero);

      container.read(sesionProvider.notifier);
      sesion.cambiar(const EstadoSesion.conSesion(usuarioDePrueba));
      await pumpEventQueue();

      expect(dispositivos.registrados, ['token-1']);
      expect(container.read(registroDePushProvider).estaRegistradoPara('u1'), true);
    });

    test('con la sesión ya iniciada al arrancar la app (sesión recuperada) también registra', () async {
      sesion = SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba));
      conectarPush(crear(), mensajero);
      await pumpEventQueue();

      expect(dispositivos.registrados, ['token-1']);
    });

    test('cada renovación de la sesión no vuelve a registrar', () async {
      final container = crear();
      conectarPush(container, mensajero);

      sesion.cambiar(const EstadoSesion.conSesion(usuarioDePrueba));
      await pumpEventQueue();
      sesion.cambiar(const EstadoSesion.conSesion(usuarioDePrueba));
      sesion.cambiar(const EstadoSesion.conSesion(usuarioDePrueba));
      await pumpEventQueue();

      expect(dispositivos.registrados, hasLength(1));
      expect(mensajeria.permisosPedidos, 1);
    });

    test('al cerrar sesión se quita el teléfono, y se hace ANTES de perder el token (el gancho de la sesión)', () async {
      final container = crear();
      conectarPush(container, mensajero);
      sesion.cambiar(const EstadoSesion.conSesion(usuarioDePrueba));
      await pumpEventQueue();

      // * La sesión llama a este gancho al empezar a cerrarse (ver sesion_provider_test)
      await sesion.antesDeCerrarSesion!();

      expect(dispositivos.quitados, ['dispositivo-1']);
      expect(container.read(registroDePushProvider).estaRegistradoPara('u1'), false);
    });

    test('la sesión queda conectada con el gancho de quitar el teléfono', () {
      conectarPush(crear(), mensajero);

      expect(sesion.antesDeCerrarSesion, isNotNull);
    });
  });

  group('con un aviso en primer plano', () {
    Future<void> montarApp(WidgetTester tester, ProviderContainer container) => tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(scaffoldMessengerKey: mensajero, home: const Scaffold(body: SizedBox())),
      ),
    );

    testWidgets('muestra el título y el mensaje en un aviso breve', (tester) async {
      final container = crear();
      conectarPush(container, mensajero);
      await montarApp(tester, container);

      mensajeria.llegaUnAviso(const AvisoPush(titulo: 'Reserva confirmada', cuerpo: 'Tu clase es mañana a las 09:00'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));

      expect(find.text('Reserva confirmada: Tu clase es mañana a las 09:00'), findsOneWidget);
    });

    testWidgets('con solo un título muestra solo el título', (tester) async {
      final container = crear();
      conectarPush(container, mensajero);
      await montarApp(tester, container);

      mensajeria.llegaUnAviso(const AvisoPush(titulo: 'Pago recibido'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));

      expect(find.text('Pago recibido'), findsOneWidget);
    });

    testWidgets('un aviso sin texto no muestra nada, pero igual refresca el contador', (tester) async {
      final container = crear();
      conectarPush(container, mensajero);
      await montarApp(tester, container);
      await container.read(conteoNoLeidasProvider.future);

      mensajeria.llegaUnAviso(const AvisoPush());
      await tester.pump();

      expect(find.byType(SnackBar), findsNothing);
      await container.read(conteoNoLeidasProvider.future);
      expect(lecturasDelContador, 2);
    });

    testWidgets('refresca el contador de no leídas', (tester) async {
      final container = crear();
      conectarPush(container, mensajero);
      await montarApp(tester, container);
      await container.read(conteoNoLeidasProvider.future);
      expect(lecturasDelContador, 1);

      mensajeria.llegaUnAviso(const AvisoPush(titulo: 'Nuevo aviso'));
      await tester.pump();
      await container.read(conteoNoLeidasProvider.future);

      expect(lecturasDelContador, 2);
    });
  });
}
