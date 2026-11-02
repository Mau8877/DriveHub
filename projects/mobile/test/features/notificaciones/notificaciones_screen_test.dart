import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/screens/notificaciones_screen.dart';

import '../../helpers/notificaciones_falsas.dart';
import '../../helpers/sesion_falsa.dart';

Future<void> _montar(WidgetTester tester, NotificacionesServiceFalso servicio) async {
  tester.view.physicalSize = const Size(900, 1800);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sesionFalsa(SesionFalsa(const EstadoSesion.conSesion(usuarioDePrueba))),
        notificacionesServiceFalso(servicio),
      ],
      child: MaterialApp(theme: AppTheme.light, home: const NotificacionesScreen()),
    ),
  );
  await tester.pumpAndSettle();
}

Finder get _marcarTodas => find.byKey(const Key('botonMarcarTodas'));

bool _marcarTodasHabilitado(WidgetTester tester) =>
    tester.widget<TextButton>(_marcarTodas).onPressed != null;

Finder _noLeida(int n) => find.byKey(Key('noLeida-n$n'));

void main() {
  testWidgets('la lista distingue los avisos no leídos de los leídos', (tester) async {
    await _montar(
      tester,
      NotificacionesServiceFalso()..avisos = [avisoDePrueba(1), avisoDePrueba(2, leida: true)],
    );

    expect(find.text('Aviso 1'), findsOneWidget);
    expect(find.text('Aviso 2'), findsOneWidget);
    expect(_noLeida(1), findsOneWidget);
    expect(_noLeida(2), findsNothing);
  });

  testWidgets('ver la lista no marca nada como leído', (tester) async {
    final servicio = NotificacionesServiceFalso()..avisos = [avisoDePrueba(1)];
    await _montar(tester, servicio);

    expect(servicio.leidos, isEmpty);
    expect(_noLeida(1), findsOneWidget);
  });

  testWidgets('sin avisos muestra la bandeja vacía y «Marcar todas» queda deshabilitado', (tester) async {
    await _montar(tester, NotificacionesServiceFalso());

    expect(find.text('No tienes avisos'), findsOneWidget);
    expect(_marcarTodasHabilitado(tester), isFalse);
  });

  testWidgets('un filtro sin resultados dice que no hay avisos con ese filtro', (tester) async {
    await _montar(
      tester,
      NotificacionesServiceFalso()..avisos = [avisoDePrueba(1, leida: true)],
    );

    await tester.tap(find.widgetWithText(ChoiceChip, 'No leídas'));
    await tester.pumpAndSettle();

    expect(find.text('No hay avisos con este filtro'), findsOneWidget);
  });

  testWidgets('los filtros piden al servicio lo que corresponde', (tester) async {
    final servicio = NotificacionesServiceFalso()
      ..avisos = [
        avisoDePrueba(1, leida: true),
        avisoDePrueba(2, tipo: TipoNotificacion.examen),
        avisoDePrueba(3),
      ];
    await _montar(tester, servicio);

    await tester.tap(find.widgetWithText(ChoiceChip, 'No leídas'));
    await tester.pumpAndSettle();
    expect(find.text('Aviso 1'), findsNothing);
    expect(servicio.listados.last.soloNoLeidas, isTrue);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Exámenes'));
    await tester.pumpAndSettle();
    expect(find.text('Aviso 3'), findsNothing);
    expect(find.text('Aviso 2'), findsOneWidget);
    expect(servicio.listados.last.tipo, TipoNotificacion.examen);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Todas'));
    await tester.pumpAndSettle();
    expect(find.text('Aviso 1'), findsOneWidget);
  });

  testWidgets('al llegar al final carga la página siguiente', (tester) async {
    final servicio = NotificacionesServiceFalso()
      ..avisos = [for (var i = 1; i <= 25; i++) avisoDePrueba(i)];
    await _montar(tester, servicio);
    expect(servicio.listados, hasLength(1));

    await tester.drag(find.byType(ListView).last, const Offset(0, -6000));
    await tester.pumpAndSettle();

    expect(servicio.listados.map((l) => l.page), [1, 2]);
    await tester.scrollUntilVisible(find.text('Aviso 25'), 300, scrollable: find.byType(Scrollable).last);
    expect(find.text('Aviso 25'), findsOneWidget);
  });

  testWidgets('abrir un aviso muestra el mensaje completo y lo deja leído', (tester) async {
    final servicio = NotificacionesServiceFalso()..avisos = [avisoDePrueba(1), avisoDePrueba(2)];
    await _montar(tester, servicio);
    expect(_marcarTodasHabilitado(tester), isTrue);

    await tester.tap(find.byKey(const Key('aviso-n1')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('detalleNotificacion')), findsOneWidget);
    expect(find.text('Mensaje completo del aviso 1'), findsNWidgets(2));
    expect(servicio.leidos, ['n1']);

    // Cierra la hoja: en la lista ya figura leído
    await tester.tapAt(const Offset(10, 10));
    await tester.pumpAndSettle();
    expect(_noLeida(1), findsNothing);
    expect(_noLeida(2), findsOneWidget);
  });

  testWidgets('si no se puede marcar como leído, igual muestra el mensaje y avisa del problema', (tester) async {
    final servicio = NotificacionesServiceFalso()
      ..avisos = [avisoDePrueba(1)]
      ..errorAlLeer = Exception('sin red');
    await _montar(tester, servicio);

    await tester.tap(find.byKey(const Key('aviso-n1')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('detalleNotificacion')), findsOneWidget);
    expect(find.text('No pudimos marcar el aviso como leído'), findsOneWidget);
  });

  testWidgets('con ERR_NOT_04 el aviso desaparece de la lista', (tester) async {
    final servicio = NotificacionesServiceFalso()
      ..avisos = [avisoDePrueba(1), avisoDePrueba(2)]
      ..errorAlLeer = ApiException(code: 'ERR_NOT_04', message: 'El aviso no existe');
    await _montar(tester, servicio);

    await tester.tap(find.byKey(const Key('aviso-n1')));
    await tester.pumpAndSettle();
    await tester.tapAt(const Offset(10, 10));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('aviso-n1')), findsNothing);
    expect(find.byKey(const Key('aviso-n2')), findsOneWidget);
  });

  testWidgets('«Marcar todas como leídas» deja todo leído y avisa', (tester) async {
    final servicio = NotificacionesServiceFalso()..avisos = [avisoDePrueba(1), avisoDePrueba(2)];
    await _montar(tester, servicio);

    await tester.tap(_marcarTodas);
    await tester.pumpAndSettle();

    expect(servicio.leerTodasLlamadas, 1);
    expect(_noLeida(1), findsNothing);
    expect(_noLeida(2), findsNothing);
    expect(find.text('Todos tus avisos quedaron leídos'), findsOneWidget);
    expect(_marcarTodasHabilitado(tester), isFalse);
  });

  testWidgets('un error de carga ofrece «Reintentar» y no muestra una lista inventada', (tester) async {
    final servicio = NotificacionesServiceFalso()
      ..errorListado = Exception('sin red')
      ..avisos = [avisoDePrueba(1)];
    await _montar(tester, servicio);

    expect(find.byKey(const Key('botonReintentarAvisos')), findsOneWidget);
    expect(find.text('Aviso 1'), findsNothing);

    servicio.errorListado = null;
    await tester.tap(find.byKey(const Key('botonReintentarAvisos')));
    await tester.pumpAndSettle();

    expect(find.text('Aviso 1'), findsOneWidget);
  });

  testWidgets('un error al cargar más conserva lo mostrado y lo avisa', (tester) async {
    final servicio = NotificacionesServiceFalso()
      ..avisos = [for (var i = 1; i <= 25; i++) avisoDePrueba(i)]
      ..errorListado = Exception('sin red')
      ..errorListadoSoloDesdeLaPagina2 = true;
    await _montar(tester, servicio);

    await tester.drag(find.byType(ListView).last, const Offset(0, -6000));
    await tester.pumpAndSettle();

    expect(find.textContaining('No pudimos cargar más avisos'), findsOneWidget);
    expect(find.byKey(const Key('aviso-n20')), findsOneWidget);
  });
}
