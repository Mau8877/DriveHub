import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/pagos/models/estado_pago_visible.dart';
import 'package:mobile/features/pagos/models/motivo_reembolso.dart';
import 'package:mobile/features/pagos/screens/pago_detalle_screen.dart';

import '../../helpers/pagos_falsos.dart';

void main() {
  late PagosServiceFalso servicio;

  setUp(() {
    servicio = PagosServiceFalso();
    servicio.detalles = {'c1': detalleDePrueba(reembolsable: true, horasRestantes: 10)};
  });

  Future<void> montar(WidgetTester tester) async {
    tester.view.physicalSize = const Size(800, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final router = GoRouter(
      initialLocation: '/lista',
      routes: [
        GoRoute(
          path: '/lista',
          builder: (context, _) => Scaffold(
            body: TextButton(onPressed: () => context.push('/payments/c1'), child: const Text('ABRIR')),
          ),
        ),
        GoRoute(path: '/payments/:codigo', builder: (_, s) => PagoDetalleScreen(codigo: s.pathParameters['codigo']!)),
      ],
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [pagosServiceFalso(servicio)],
        child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('ABRIR'));
    await tester.pumpAndSettle();
  }

  Future<void> abrirDialogo(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('botonSolicitarReembolso')));
    await tester.pumpAndSettle();
  }

  Future<void> elegir(WidgetTester tester, MotivoReembolso motivo) async {
    await tester.tap(find.byKey(Key('motivo_${motivo.valorApi}')));
    await tester.pumpAndSettle();
  }

  bool solicitarHabilitado(WidgetTester tester) =>
      tester.widget<FilledButton>(find.byKey(const Key('botonConfirmarReembolso'))).onPressed != null;

  Future<void> confirmar(WidgetTester tester) async {
    await tester.tap(find.byKey(const Key('botonConfirmarReembolso')));
    await tester.pumpAndSettle();
  }

  testWidgets('la sección no aparece si la compra no es reembolsable', (tester) async {
    servicio.detalles = {'c1': detalleDePrueba()};

    await montar(tester);

    expect(find.byKey(const Key('seccionReembolso')), findsNothing);
    expect(find.text('Solicitar reembolso'), findsNothing);
  });

  testWidgets('si es reembolsable muestra lo que recibirá, lo que se retiene y el botón', (tester) async {
    await montar(tester);

    expect(find.byKey(const Key('seccionReembolso')), findsOneWidget);
    expect(find.text('Recibirás US\$ 36,32; se retiene US\$ 1,91 (5 %).'), findsOneWidget);
    expect(find.byKey(const Key('botonSolicitarReembolso')), findsOneWidget);
  });

  testWidgets('el diálogo muestra los montos, las horas y los siete motivos', (tester) async {
    await montar(tester);

    await abrirDialogo(tester);

    expect(find.byKey(const Key('textoMontosReembolso')), findsOneWidget);
    expect(find.textContaining('US\$ 36,32'), findsWidgets);
    expect(find.text('Se descontarán 10 horas de tu saldo.'), findsOneWidget);
    for (final motivo in MotivoReembolso.values) {
      expect(find.byKey(Key('motivo_${motivo.valorApi}')), findsOneWidget);
      expect(find.text(motivo.texto), findsOneWidget);
    }
    expect(find.byKey(const Key('campoMotivoDetalle')), findsNothing);
  });

  testWidgets('«Solicitar» está deshabilitado hasta elegir un motivo', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);
    expect(solicitarHabilitado(tester), isFalse);

    await elegir(tester, MotivoReembolso.mudanza);

    expect(solicitarHabilitado(tester), isTrue);
  });

  testWidgets('con «Otro» pide el texto y exige de 5 a 200 caracteres', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);

    await elegir(tester, MotivoReembolso.otro);
    expect(find.byKey(const Key('campoMotivoDetalle')), findsOneWidget);
    expect(solicitarHabilitado(tester), isFalse);

    await tester.enterText(find.byKey(const Key('campoMotivoDetalle')), 'abcd');
    await tester.pump();
    expect(solicitarHabilitado(tester), isFalse);

    await tester.enterText(find.byKey(const Key('campoMotivoDetalle')), '     ');
    await tester.pump();
    expect(solicitarHabilitado(tester), isFalse);

    await tester.enterText(find.byKey(const Key('campoMotivoDetalle')), 'abcde');
    await tester.pump();
    expect(solicitarHabilitado(tester), isTrue);
  });

  testWidgets('cancelar cierra el diálogo sin pedir nada', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);

    await tester.tap(find.byKey(const Key('botonCancelarReembolso')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('botonConfirmarReembolso')), findsNothing);
    expect(servicio.reembolsos, isEmpty);
  });

  testWidgets('confirmar envía el motivo; solo con «Otro» envía el texto', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.otro);
    await tester.enterText(find.byKey(const Key('campoMotivoDetalle')), '  Perdí mi trabajo  ');
    await tester.pump();

    await confirmar(tester);

    expect(servicio.reembolsos, hasLength(1));
    expect(servicio.reembolsos.single.motivo, MotivoReembolso.otro);
    expect(servicio.reembolsos.single.detalle, 'Perdí mi trabajo');
  });

  testWidgets('con otro motivo no manda texto', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.sinTiempo);

    await confirmar(tester);

    expect(servicio.reembolsos.single.motivo, MotivoReembolso.sinTiempo);
    expect(servicio.reembolsos.single.detalle, isNull);
  });

  testWidgets('tras confirmar la compra pasa a «reembolso en proceso» y la sección de reembolso desaparece', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);

    await confirmar(tester);

    expect(find.text('Estamos procesando tu reembolso.'), findsOneWidget);
    expect(find.text('Reembolso en proceso'), findsOneWidget);
    expect(find.byKey(const Key('botonSolicitarReembolso')), findsNothing);
    // * Termina de seguir la compra para no dejar temporizadores
    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsado, horasRestantes: 0)};
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();
  });

  testWidgets('la consulta cada 5 segundos termina en «reembolsado» con un aviso', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);
    await confirmar(tester);

    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsado, horasRestantes: 0)};
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();

    expect(find.text('Tu reembolso se completó.'), findsOneWidget);
    expect(find.text('Reembolsado'), findsWidgets);
    expect(find.text('Estamos procesando tu reembolso.'), findsNothing);
  });

  testWidgets('si la devolución falla, la compra vuelve a «confirmada» y se avisa que las horas siguen', (tester) async {
    await montar(tester);
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);
    await confirmar(tester);

    servicio.detalles = {'c1': detalleDePrueba(horasRestantes: 10)};
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();

    expect(find.text('No pudimos devolver el dinero, las horas siguen en tu cuenta.'), findsOneWidget);
    expect(find.text('Estamos procesando tu reembolso.'), findsNothing);
  });

  testWidgets('una compra que ya está en proceso al abrirla también se sigue', (tester) async {
    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsoEnProceso, horasRestantes: 0)};
    await montar(tester);
    expect(find.text('Estamos procesando tu reembolso.'), findsOneWidget);

    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsado, horasRestantes: 0)};
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();

    expect(find.text('Tu reembolso se completó.'), findsOneWidget);
  });

  testWidgets('ERR_PAG_11 avisa que ya usó horas y vuelve a pedir el detalle', (tester) async {
    await montar(tester);
    servicio.errorReembolso = ApiException(code: 'ERR_PAG_11', message: 'horas usadas');
    final detallesAntes = servicio.pedidosDeDetalle;
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);

    await confirmar(tester);

    expect(find.text('Ya usaste horas de esta compra, no se puede reembolsar.'), findsOneWidget);
    expect(servicio.pedidosDeDetalle, greaterThan(detallesAntes));
  });

  testWidgets('ERR_PAG_10 avisa que ya no se puede reembolsar', (tester) async {
    await montar(tester);
    servicio.errorReembolso = ApiException(code: 'ERR_PAG_10', message: 'no');
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);

    await confirmar(tester);

    expect(find.text('Esta compra ya no se puede reembolsar.'), findsOneWidget);
  });

  testWidgets('ERR_PAG_06 pide intentar más tarde', (tester) async {
    await montar(tester);
    servicio.errorReembolso = ApiException(code: 'ERR_PAG_06', message: 'sin pasarela');
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);

    await confirmar(tester);

    expect(find.text('No pudimos conectar con el pago, inténtalo más tarde.'), findsOneWidget);
  });

  testWidgets('sin conexión muestra el aviso y permite reintentar', (tester) async {
    await montar(tester);
    servicio.errorReembolso = DioException(
      requestOptions: RequestOptions(path: '/pagos/c1/reembolso'),
      type: DioExceptionType.connectionError,
    );
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);
    await confirmar(tester);
    expect(find.text('No hay conexión. Revisa tu internet e inténtalo de nuevo.'), findsOneWidget);

    servicio.errorReembolso = null;
    await abrirDialogo(tester);
    await elegir(tester, MotivoReembolso.mudanza);
    await confirmar(tester);

    expect(servicio.reembolsos, hasLength(2));
    servicio.detalles = {'c1': detalleDePrueba(estado: EstadoPagoVisible.reembolsado, horasRestantes: 0)};
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();
  });
}
